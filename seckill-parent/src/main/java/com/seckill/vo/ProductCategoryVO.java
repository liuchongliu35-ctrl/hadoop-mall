package com.seckill.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ProductCategoryVO {
    
    private Long id;
    private String categoryName;
    private Long parentId;
    private LocalDateTime createTime;
}