package com.seckill.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class SeckillActivityDTO {
    
    private Long id;
    
    @NotBlank(message = "活动名称不能为空")
    private String activityName;
    
    @NotNull(message = "商品ID不能为空")
    private Long productId;
    
    @NotNull(message = "秒杀价格不能为空")
    private BigDecimal seckillPrice;
    
    @NotNull(message = "秒杀库存不能为空")
    private Integer seckillStock;
    
    @NotNull(message = "开始时间不能为空")
    private LocalDateTime startTime;
    
    @NotNull(message = "结束时间不能为空")
    private LocalDateTime endTime;
    
    private Integer status;
}