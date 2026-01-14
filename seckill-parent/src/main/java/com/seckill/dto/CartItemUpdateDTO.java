package com.seckill.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CartItemUpdateDTO {
    @NotBlank(message = "商品ID不能为空")
    private String productId;
    @NotNull(message = "商品数量不能为空")
    @Min(value = 1, message = "更新后的商品数量至少为1")
    private Integer quantity;
}