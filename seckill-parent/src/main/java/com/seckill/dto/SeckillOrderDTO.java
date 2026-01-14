package com.seckill.dto;

import lombok.Data;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * 秒杀订单DTO
 */
@Data
public class SeckillOrderDTO {
    
    /**
     * 活动ID
     */
    @NotNull(message = "活动ID不能为空")
    private Long activityId;
    
    /**
     * 用户ID
     */
//    @NotNull(message = "用户ID不能为空")
    private Long userId;
    
    /**
     * 商品ID
     */
    @NotNull(message = "商品ID不能为空")
    private Long productId;
    
    /**
     * 购买数量
     */
    @NotNull(message = "购买数量不能为空")
    @Positive(message = "购买数量必须大于0")
    private Integer quantity;
}