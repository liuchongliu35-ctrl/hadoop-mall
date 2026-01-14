package com.seckill.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.seckill.entity.SeckillOrder;

/**
 * 秒杀订单Mapper接口（HBase实现）
 */
public interface SeckillOrderMapper {

    int insert(SeckillOrder order);

    int updateById(SeckillOrder order);

    int deleteById(Long id);

    SeckillOrder selectById(Long id);

    IPage<SeckillOrder> selectOrderPage(Page<SeckillOrder> page, Integer status);

    java.util.List<SeckillOrder> selectListByUser(Long userId, Integer status);
}