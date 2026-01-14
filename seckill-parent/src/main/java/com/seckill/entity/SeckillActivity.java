package com.seckill.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("t_seckill_activity")
public class SeckillActivity {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String activityName;
    
    private Long productId;
    
    private BigDecimal seckillPrice;
    
    private Integer seckillStock;
    
    private LocalDateTime startTime;
    
    private LocalDateTime endTime;
    
    private Integer status; // 0-未开始 1-进行中 2-已结束
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    
    @TableLogic
    private Integer deleted;
}