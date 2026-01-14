package com.seckill.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 热门商品排行 VO
 */
@Data
public class HotProductVO {

    private Long productId;

    private String productName;

    private BigDecimal price;

    /**
     * 销量（当日）
     */
    private Long quantity;
}


