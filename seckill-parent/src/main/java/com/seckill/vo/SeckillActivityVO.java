package com.seckill.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class SeckillActivityVO {
    
    private Long id;
    private String activityName;
    private Long productId;
    private String productName;
    private String productDesc;
    private BigDecimal originalPrice;
    private BigDecimal seckillPrice;
    private Integer seckillStock;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer status;
    private String statusDesc;
    private String imgUrl;
    private LocalDateTime createTime;
}