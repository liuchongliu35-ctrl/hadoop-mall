package com.seckill.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ProductVO {
    
    private Long id;
    private String productName;
    private String productDesc;
    private BigDecimal price;
    private Integer stock;
    private Long categoryId;
    private String categoryName;
    private String imgUrl;
    private Integer status;
    private String statusDesc;
    private LocalDateTime createTime;
}