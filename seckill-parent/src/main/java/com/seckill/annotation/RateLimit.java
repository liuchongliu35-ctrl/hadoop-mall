package com.seckill.annotation;

import com.seckill.common.LimitType;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {
//    限流类型：“USER_ACTIVITY（用户-活动级）、GLOBAL（全局级）
    LimitType limitType() default LimitType.USER_ACTIVITY;

//    限流阈值
    int limit() default 5;

//    时间窗口
    int period() default 60;

//    时间单位
    TimeUnit timeunit() default TimeUnit.SECONDS;

//    限流提示信息
    String message() default "请求过于频繁，请稍后再试";
}
