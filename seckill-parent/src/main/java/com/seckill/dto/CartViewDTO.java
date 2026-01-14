package com.seckill.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;
@Data
public class CartViewDTO {
    private List<CartItemViewDTO> items;
    private Integer totalItems; // 商品总件数
    private BigDecimal totalPrice; // 购物车总价
}
