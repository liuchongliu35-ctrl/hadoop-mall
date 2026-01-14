package com.seckill.vo;

import lombok.Data;

import java.time.LocalDateTime;

//活动 ID、状态文本、状态码、剩余库存、开始时间、结束时间
@Data
public class ActivityStatusVo {
    private Long activityId;
    private String activityName;
    private String status;
    private Integer statusCode;
    private Integer seckillStock;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
