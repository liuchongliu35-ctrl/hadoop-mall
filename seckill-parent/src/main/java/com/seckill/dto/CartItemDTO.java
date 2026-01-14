package com.seckill.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartItemDTO {
    /**
     * 商品数量
     */
    private Integer quantity;
    /**
     * 添加到购物车的时间戳
     */
    private Long addTime;
}
