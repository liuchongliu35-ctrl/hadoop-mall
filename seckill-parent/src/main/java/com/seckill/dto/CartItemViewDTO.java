package com.seckill.dto;

import lombok.Data;
import java.math.BigDecimal;
@Data
public class CartItemViewDTO {
    private String productId;
    private String name;
    private String image; // 假设有商品图片
    private BigDecimal price;
    private Integer quantity;
    private BigDecimal subTotal; // 单个商品小计
    private String stockStatus;
}