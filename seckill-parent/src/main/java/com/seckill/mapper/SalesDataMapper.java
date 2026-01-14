package com.seckill.mapper;

import com.seckill.entity.SalesData;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 销售数据Mapper（HBase实现）
 */
public interface SalesDataMapper {

    /**
     * 累加每日销售数据（按商品维度）
     */
    void addDailySale(LocalDate date, Long productId, Long categoryId, long quantity, BigDecimal amount);

    /**
     * 按日期范围查询每日销售数据
     */
    List<SalesData> queryDailySales(LocalDate startDate, LocalDate endDate, Long productId);
}


