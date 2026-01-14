package com.seckill.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;

@Data
public class ProductCategoryDTO {
    
    private Long id;
    
    @NotBlank(message = "分类名称不能为空")
    private String categoryName;
    
    private Long parentId;
}