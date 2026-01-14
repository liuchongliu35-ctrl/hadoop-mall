package com.seckill.service;

import com.seckill.common.PageQuery;
import com.seckill.common.PageResult;
import com.seckill.dto.CartItemAddDTO;
import com.seckill.dto.SeckillOrderDTO;
import com.seckill.vo.SeckillOrderVO;

import java.util.List;
import java.util.Map;

/**
 * 秒杀订单服务接口
 */
public interface SeckillOrderService {
    
    /**
     * 秒杀下单（使用分布式锁）
     * @param orderDTO 订单信息
     * @return 订单ID
     */
    Long createSeckillOrder(SeckillOrderDTO orderDTO);
    
    /**
     * 取消订单
     * @param orderId 订单ID
     */
    void cancelOrder(Long orderId);
    
    /**
     * 支付订单
     * @param orderId 订单ID
     */
    void payOrder(Long orderId);
    
    /**
     * 查询订单详情
     * @param orderId 订单ID
     * @return 订单详情
     */
    SeckillOrderVO getOrderDetail(Long orderId);
    
    /**
     * 查询用户订单列表
     * @param userId 用户ID
     * @param status 订单状态
     * @return 订单列表
     */
    List<SeckillOrderVO> getUserOrders(Long userId, Integer status);
    
    /**
     * 分页查询订单列表
     * @param pageQuery 分页参数
     * @param status 订单状态
     * @return 分页结果
     */
    PageResult<SeckillOrderVO> getOrderList(PageQuery pageQuery, Integer status);

//    Map<String, Object> createOrdersFromCart(Long userId, List<CartItemAddDTO> items);

}