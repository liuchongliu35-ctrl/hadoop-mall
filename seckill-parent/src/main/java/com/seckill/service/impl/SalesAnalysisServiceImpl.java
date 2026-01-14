package com.seckill.service.impl;

import com.seckill.entity.Product;
import com.seckill.entity.SalesData;
import com.seckill.entity.SeckillOrder;
import com.seckill.mapper.ProductMapper;
import com.seckill.mapper.SalesDataMapper;
import com.seckill.service.SalesAnalysisService;
import com.seckill.vo.HotProductVO;
import com.seckill.vo.SalesDashboardVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
public class SalesAnalysisServiceImpl implements SalesAnalysisService {

    private static final String STAT_ORDERS_PREFIX = "stat:orders:";
    private static final String STAT_SALES_PREFIX = "stat:sales:";
    private static final String RANK_DAILY_SALE_PREFIX = "rank:daily:sale:";

    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.BASIC_ISO_DATE; // yyyyMMdd

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private SalesDataMapper salesDataMapper;

    @Autowired
    private ProductMapper productMapper;

//    todo redis和hbase都对成功的订单进行一个记录
    @Override
    public void recordPaidOrder(SeckillOrder order) {
        try {
//            todo key中加日期信息，唯一这一天的数据
            LocalDate today = LocalDate.now();
            String dayStr = today.format(DAY_FMT);

            // 1. Redis 实时看板统计
            String ordersKey = STAT_ORDERS_PREFIX + dayStr;
            String salesKey = STAT_SALES_PREFIX + dayStr;

            // 订单数 +1
            redisTemplate.opsForValue().increment(ordersKey);

            // todo 销售额累加，以分为单位存储，避免浮点精度
            BigDecimal amount = order.getActualAmount() != null ? order.getActualAmount() : order.getTotalAmount();
            if (amount != null) {
                long cents = amount.multiply(BigDecimal.valueOf(100)).longValue();
                redisTemplate.opsForValue().increment(salesKey, cents);
            }

            // 2. Redis 热门商品排行榜（按数量）
            String rankKey = RANK_DAILY_SALE_PREFIX + dayStr;
            String member = "product:" + order.getProductId();
            redisTemplate.opsForZSet().incrementScore(rankKey, member, order.getQuantity());

            // 3. HBase记录历史销售数据，加入到记录日销数据的Hbase表中
            Product product = productMapper.selectById(order.getProductId());
            Long categoryId = product != null ? product.getCategoryId() : null;
            salesDataMapper.addDailySale(today, order.getProductId(), categoryId,
                    order.getQuantity(), amount != null ? amount : BigDecimal.ZERO);
        } catch (Exception e) {
            log.error("记录销售分析数据失败", e);
        }
    }
//todo 从redis中获取今日销售数据
    @Override
    public SalesDashboardVO getTodayDashboard() {
        LocalDate today = LocalDate.now();
        String dayStr = today.format(DAY_FMT);
        String ordersKey = STAT_ORDERS_PREFIX + dayStr;
        String salesKey = STAT_SALES_PREFIX + dayStr;

        Long orders = getLongValue(ordersKey);
        Long salesCents = getLongValue(salesKey);
        BigDecimal salesAmount = salesCents != null
                ? BigDecimal.valueOf(salesCents).divide(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;

        SalesDashboardVO vo = new SalesDashboardVO();
        vo.setDate(today);
        vo.setOrderCount(orders != null ? orders : 0L);
        vo.setTotalAmount(salesAmount);
        return vo;
    }

    @Override
    public List<HotProductVO> getTodayHotProducts(int topN) {
        LocalDate today = LocalDate.now();
//        todo 找到redis中存放今日数据的仓库
        String dayStr = today.format(DAY_FMT);
        String rankKey = RANK_DAILY_SALE_PREFIX + dayStr;
//         todo 使用redis自带的ZSet即Sorted Set按顺序取前几名
        Set<ZSetOperations.TypedTuple<Object>> set = redisTemplate.opsForZSet()
                .reverseRangeWithScores(rankKey, 0, topN - 1); //todo 倒序取最高
        List<HotProductVO> list = new ArrayList<>();
        if (set == null) {
            return list;
        }
//        todo 将数据解析出来
        for (ZSetOperations.TypedTuple<Object> tuple : set) {
            Object member = tuple.getValue();
            Double score = tuple.getScore();
            if (member == null || score == null) continue;
            String memberStr = member.toString(); // product:ID
            if (!memberStr.startsWith("product:")) continue;
            Long productId = Long.parseLong(memberStr.substring("product:".length()));
//            todo 根据已有数据从商品表中将完整数据拿出来补全结果对象
            Product product = productMapper.selectById(productId);

            HotProductVO vo = new HotProductVO();
            vo.setProductId(productId);
            vo.setQuantity(score.longValue());
            if (product != null) {
                vo.setProductName(product.getProductName());
                vo.setPrice(product.getPrice());
            }
            list.add(vo);
        }
        return list;
    }

    @Override
    public List<SalesData> getDailySales(LocalDate startDate, LocalDate endDate, Long productId) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("startDate and endDate cannot be null");
        }
        return salesDataMapper.queryDailySales(startDate, endDate, productId);
    }

    private Long getLongValue(String key) {
        Object v = redisTemplate.opsForValue().get(key);
        if (v == null) return null;
        if (v instanceof Integer i) return i.longValue();
        if (v instanceof Long l) return l;
        if (v instanceof String s) return Long.parseLong(s);
        if (v instanceof Double d) return d.longValue();
        return null;
    }
}


