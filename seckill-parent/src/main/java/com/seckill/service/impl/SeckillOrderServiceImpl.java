package com.seckill.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seckill.common.BusinessException;
import com.seckill.common.PageQuery;
import com.seckill.common.PageResult;
import com.seckill.dto.CartItemAddDTO;
import com.seckill.dto.SeckillOrderDTO;
import com.seckill.entity.Product;
import com.seckill.entity.SeckillActivity;
import com.seckill.entity.SeckillOrder;
import com.seckill.enums.ActivityStatusEnum;
import com.seckill.enums.PayStatusEnum; // 【新增】引入枚举
import com.seckill.mapper.ProductMapper;
import com.seckill.mapper.SeckillActivityMapper;
import com.seckill.mapper.SeckillOrderMapper;
import com.seckill.service.ProductService;
import com.seckill.service.SalesAnalysisService;
import com.seckill.service.SeckillOrderService;
import com.seckill.util.RedisUtil;
import com.seckill.vo.SeckillOrderVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 秒杀订单服务实现类
 */
@Slf4j
@Service
public class SeckillOrderServiceImpl implements SeckillOrderService {

    @Autowired
    private SeckillOrderMapper orderMapper;

    @Autowired
    private SeckillActivityMapper activityMapper;

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private ProductMapper  productMapper;

    @Autowired
    private SalesAnalysisService salesAnalysisService;

    @Autowired
    private ProductService productService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String ACTIVITY_LOCK_PREFIX = "seckill:activity:lock:";
    private static final String ACTIVITY_STOCK_PREFIX = "seckill:activity:stock:";
    private static final String PRODUCT_STOCK_PREFIX = "stock:";
    private static final String ORDER_STATUS_PREFIX = "order:status:";

    @Override
    @Transactional
    public Long createSeckillOrder(SeckillOrderDTO orderDTO) {
        Long activityId = orderDTO.getActivityId();
        Long userId = orderDTO.getUserId();

        String lockKey = ACTIVITY_LOCK_PREFIX + activityId;

        return redisUtil.executeWithLock(lockKey, 10L, TimeUnit.SECONDS, () -> {
            log.info("用户 {} 开始秒杀活动 {}", userId, activityId);

            SeckillActivity activity = activityMapper.selectById(activityId);
            if (activity == null) throw new BusinessException("秒杀活动不存在");
            if (!activity.getStatus().equals(ActivityStatusEnum.IN_PROGRESS.getCode())) throw new BusinessException("秒杀活动未进行中");
            if (activity.getEndTime().isBefore(LocalDateTime.now())) throw new BusinessException("秒杀活动已结束");

            String stockKey = ACTIVITY_STOCK_PREFIX + activityId;
            Integer remainingStock = (Integer) redisUtil.get(stockKey);
            String productStockKey = PRODUCT_STOCK_PREFIX + orderDTO.getProductId();

            if (remainingStock == null) {
                remainingStock = activity.getSeckillStock();
                redisUtil.set(stockKey, remainingStock, 24 * 60 * 60, TimeUnit.SECONDS);
                redisUtil.set(productStockKey, remainingStock, 24 * 60 * 60, TimeUnit.SECONDS);
            }

            if (remainingStock <= 0) throw new BusinessException("商品已售罄");

            // 检查HBase是否有历史订单 (排除已取消的)
            List<SeckillOrder> userOrders = orderMapper.selectListByUser(userId, null);
            boolean exists = userOrders.stream()
                    .anyMatch(o -> o.getActivityId().equals(activityId)
                            && (o.getStatus() != null && !o.getStatus().equals(PayStatusEnum.CANCELLED.getCode()))); // 【修改】使用枚举
            if (exists) throw new BusinessException("您已经参与过此秒杀活动");

            // 预扣库存
            Long newStock = redisUtil.decrement(stockKey);
            if (newStock < 0) {
                redisUtil.increment(stockKey);
                throw new BusinessException("商品已售罄");
            }
            redisUtil.decrement(productStockKey);

            try {
                SeckillOrder order = new SeckillOrder();
                BeanUtils.copyProperties(orderDTO, order);
                order.setOrderNo(generateOrderNo());
                order.setProductName(activity.getActivityName());
                order.setSeckillPrice(activity.getSeckillPrice());
                order.setTotalAmount(activity.getSeckillPrice().multiply(BigDecimal.valueOf(orderDTO.getQuantity())));
                order.setDiscountAmount(BigDecimal.ZERO);
                order.setActualAmount(order.getTotalAmount());

                // 【修改】设置状态为 UNPAID (0)
                order.setStatus(PayStatusEnum.UNPAID.getCode());

                order.setCreateTime(LocalDateTime.now());
                order.setUpdateTime(LocalDateTime.now());
                order.setOrderItems(buildOrderItemsJson(order));

                int result = orderMapper.insert(order);
                if (result <= 0) {
                    redisUtil.increment(stockKey);
                    redisUtil.increment(productStockKey);
                    throw new BusinessException("创建订单失败");
                }

                cacheOrderStatus(order.getId(), order.getStatus());
                log.info("用户 {} 秒杀成功，订单ID: {}", userId, order.getId());
                return order.getId();

            } catch (Exception e) {
                redisUtil.increment(stockKey);
                redisUtil.increment(productStockKey);
                log.error("创建订单异常，回滚库存", e);
                throw new BusinessException("创建订单失败");
            }
        });
    }

    @Override
    @Transactional
    public void cancelOrder(Long orderId) {
        String lockKey = "seckill:order:cancel:" + orderId;

        if (redisUtil.tryLock(lockKey, 5L, TimeUnit.SECONDS)) {
            try {
                SeckillOrder order = orderMapper.selectById(orderId);
                if (order == null) throw new BusinessException("订单不存在");

                // 【修改】只允许取消 UNPAID 状态的订单
                if (!order.getStatus().equals(PayStatusEnum.UNPAID.getCode())) {
                    throw new BusinessException("只能取消未支付订单");
                }

                // 【修改】设置状态为 CANCELLED (2)
                order.setStatus(PayStatusEnum.CANCELLED.getCode());
                order.setUpdateTime(LocalDateTime.now());
                orderMapper.updateById(order);
                cacheOrderStatus(orderId, order.getStatus());

                String stockKey = ACTIVITY_STOCK_PREFIX + order.getActivityId();
                redisUtil.increment(stockKey);
                redisUtil.increment(PRODUCT_STOCK_PREFIX + order.getProductId());

                log.info("取消订单成功，订单ID: {}", orderId);
            } finally {
                redisUtil.unlock(lockKey);
            }
        } else {
            throw new BusinessException("系统繁忙，请稍后重试");
        }
    }

    @Override
    @Transactional
    public void payOrder(Long orderId) {
        SeckillOrder order = orderMapper.selectById(orderId);
        if (order == null) throw new BusinessException("订单不存在");

        // 【修改】检查是否为 UNPAID
        if (!order.getStatus().equals(PayStatusEnum.UNPAID.getCode())) {
            throw new BusinessException("订单状态异常或已支付");
        }

        // 【修改】设置状态为 PAID (1)
        order.setStatus(PayStatusEnum.PAID.getCode());
        order.setPayTime(LocalDateTime.now());
        order.setPayMethod("online");
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.updateById(order);
        cacheOrderStatus(orderId, order.getStatus());

        String lockKey = "seckill:stock:update:" + order.getActivityId();
        if (redisUtil.tryLock(lockKey, 5L, TimeUnit.SECONDS)) {
            try {
                SeckillActivity activity = activityMapper.selectById(order.getActivityId());
                activity.setSeckillStock(activity.getSeckillStock() - order.getQuantity());
                activityMapper.updateById(activity);
            } finally {
                redisUtil.unlock(lockKey);
            }
        }

        salesAnalysisService.recordPaidOrder(order);
        log.info("订单支付成功，订单ID: {}", orderId);
    }

    @Override
    public SeckillOrderVO getOrderDetail(Long orderId) {
        SeckillOrder order = orderMapper.selectById(orderId);
        if (order == null) throw new BusinessException("订单不存在");

        SeckillOrderVO vo = new SeckillOrderVO();
        BeanUtils.copyProperties(order, vo);
        vo.setStatusDesc(getOrderStatusDesc(order.getStatus()));
        return vo;
    }

    @Override
    public List<SeckillOrderVO> getUserOrders(Long userId, Integer status) {
        List<SeckillOrder> orders = orderMapper.selectListByUser(userId, status);
        return orders.stream().map(order -> {
            SeckillOrderVO vo = new SeckillOrderVO();
            BeanUtils.copyProperties(order, vo);
            vo.setStatusDesc(getOrderStatusDesc(order.getStatus()));
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public PageResult<SeckillOrderVO> getOrderList(PageQuery pageQuery, Integer status) {
        Page<SeckillOrder> page = new Page<>(pageQuery.getPageNum(), pageQuery.getPageSize());
        IPage<SeckillOrder> orderPage = orderMapper.selectOrderPage(page, status);
        List<SeckillOrder> records = orderPage.getRecords();

        List<SeckillOrderVO> voList = new ArrayList<>();
        if (records == null || records.isEmpty()) {
            return new PageResult<>(voList, 0L, pageQuery.getPageNum(), pageQuery.getPageSize());
        }

        for (SeckillOrder order : records) {
            log.info("这个订单的信息如下：{}", order);
            SeckillOrderVO vo = new SeckillOrderVO();
//            BeanUtils.copyProperties(order, vo);
            vo.setId(order.getId());
            vo.setOrderNo(order.getOrderNo());
            vo.setUserId(order.getUserId());
            vo.setActivityId(order.getActivityId());
            vo.setProductId(order.getProductId());
            vo.setProductName(order.getProductName()); // 优先取订单快照中的名称
            vo.setSeckillPrice(order.getSeckillPrice());
            vo.setQuantity(order.getQuantity());
            vo.setTotalAmount(order.getTotalAmount());
            vo.setStatus(order.getStatus());
            vo.setCreateTime(order.getCreateTime());
            vo.setUpdateTime(order.getUpdateTime());

            // 补全商品信息
            if (vo.getProductName() == null && order.getProductId() != null) {
                Product product = productMapper.selectById(order.getProductId());
                if (product != null) {
                    vo.setProductName(product.getProductName());
                }
            }

            if (order.getUserId() != null) {
                vo.setUserId(order.getUserId());
            }
            vo.setStatusDesc(getOrderStatusDesc(order.getStatus()));
            voList.add(vo);
        }

        return new PageResult<>(voList, orderPage.getTotal(), pageQuery.getPageNum(), pageQuery.getPageSize());
    }



    private void cacheOrderStatus(Long orderId, Integer status) {
        if (orderId == null || status == null) return;
        redisUtil.set(ORDER_STATUS_PREFIX + orderId, status, 24, TimeUnit.HOURS);
    }

    private String buildOrderItemsJson(SeckillOrder order) {
        try {
            Map<String, Object> item = new HashMap<>();
            item.put("product_id", order.getProductId());
            item.put("name", order.getProductName());
            item.put("price", order.getSeckillPrice());
            item.put("quantity", order.getQuantity());
            item.put("amount", order.getTotalAmount());
            return objectMapper.writeValueAsString(Collections.singletonList(item));
        } catch (Exception e) {
            log.warn("构建orderItems失败", e);
            return null;
        }
    }

    /**
     * 【修改】使用 PayStatusEnum 获取状态描述
     */
    private String getOrderStatusDesc(Integer status) {
        if (status == null) return "未知状态";
        PayStatusEnum statusEnum = PayStatusEnum.getByCode(status);
        if (statusEnum != null) {
            return statusEnum.getDesc();
        }
        return "未知状态";
    }

    private String generateOrderNo() {
        return "SK" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
