package com.seckill.task;

import com.seckill.service.SeckillActivityService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 秒杀活动状态自动更新定时任务
 */
@Slf4j
@Component
public class ActivityStatusUpdateTask {
    
    @Autowired
    private SeckillActivityService activityService;
    
    /**
     * 每分钟执行一次，自动更新活动状态
     * 将应该开始的活动状态更新为进行中
     * 将应该结束的活动状态更新为已结束
     */
    @Scheduled(cron = "0 * * * * ?")
    public void updateActivityStatus() {
        try {
            log.info("开始执行秒杀活动状态自动更新任务");
            activityService.updateActivityStatusAutomatically();
            log.info("秒杀活动状态自动更新任务执行完成");
        } catch (Exception e) {
            log.error("秒杀活动状态自动更新任务执行失败", e);
        }
    }
}