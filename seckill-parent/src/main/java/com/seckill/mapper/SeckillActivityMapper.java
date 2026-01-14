package com.seckill.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.seckill.entity.SeckillActivity;

import java.util.List;

/**
 * 秒杀活动Mapper（HBase实现）
 */
public interface SeckillActivityMapper {

    int insert(SeckillActivity activity);

    int updateById(SeckillActivity activity);

    int deleteById(Long id);

    SeckillActivity selectById(Long id);

    IPage<SeckillActivity> selectActivityPage(Page<SeckillActivity> page, Integer status);

    List<SeckillActivity> selectActiveActivities();

    List<SeckillActivity> selectAll();
}