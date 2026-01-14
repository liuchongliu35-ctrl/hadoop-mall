package com.seckill.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.seckill.common.PageQuery;
import com.seckill.common.Result;
import com.seckill.dto.SeckillActivityDTO;
import com.seckill.vo.ActivityStatusVo;
import com.seckill.vo.SeckillActivityVO;

import java.util.List;

public interface SeckillActivityService {
    
    void saveActivity(SeckillActivityDTO activityDTO);
    
    void updateActivity(Long id, SeckillActivityDTO activityDTO);
    
    void updateActivityStatus(Long id, Integer status);
    
    SeckillActivityVO getActivityById(Long id);
    
    IPage<SeckillActivityVO> getActivityList(PageQuery pageQuery, Integer status);
    
    void updateActivityStatusAutomatically();
    
    List<SeckillActivityVO> getActiveActivities();


}