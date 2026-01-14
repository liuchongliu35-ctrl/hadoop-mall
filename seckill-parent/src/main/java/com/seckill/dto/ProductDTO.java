package com.seckill.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

@Data
public class ProductDTO {
    
    private Long id;
    
    @NotBlank(message = "商品名称不能为空")
    private String productName;
    
    private String productDesc;
    
    @NotNull(message = "价格不能为空")
    private BigDecimal price;
    
    @NotNull(message = "库存不能为空")
    private Integer stock;
    
    @NotNull(message = "分类ID不能为空")
    private Long categoryId;
    
    private String imgUrl;
    
    @NotNull(message = "状态不能为空")
    private Integer status;
}