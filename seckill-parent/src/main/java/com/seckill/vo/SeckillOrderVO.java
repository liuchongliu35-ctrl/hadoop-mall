package com.seckill.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 秒杀订单VO
 */
@Data
public class SeckillOrderVO {
    
    /**
     * 订单ID
     */
    private Long id;
    
    /**
     * 订单号
     */
    private String orderNo;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 活动ID
     */
    private Long activityId;
    
    /**
     * 商品ID
     */
    private Long productId;
    
    /**
     * 商品名称
     */
    private String productName;
    
    /**
     * 秒杀价格
     */
    private BigDecimal seckillPrice;
    
    /**
     * 购买数量
     */
    private Integer quantity;
    
    /**
     * 总金额
     */
    private BigDecimal totalAmount;
    
    /**
     * 订单状态
     */
    private Integer status;
    
    /**
     * 订单状态描述
     */
    private String statusDesc;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}