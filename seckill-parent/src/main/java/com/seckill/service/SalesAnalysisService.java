package com.seckill.service;

import com.seckill.entity.SalesData;
import com.seckill.entity.SeckillOrder;
import com.seckill.vo.HotProductVO;
import com.seckill.vo.SalesDashboardVO;

import java.time.LocalDate;
import java.util.List;

/**
 * 销售分析服务
 */
public interface SalesAnalysisService {

    /**
     * 订单支付成功后，记录销售数据Redis+HBase
     */
    void recordPaidOrder(SeckillOrder order);

    /**
     * 今日实时销售看板从Redis中拿数据
     */
    SalesDashboardVO getTodayDashboard();

    /**
     * 今日热门商品排行榜使用Redis ZSet
     */
    List<HotProductVO> getTodayHotProducts(int topN);

    /**
     * 历史销售数据（日粒度，HBase）
     */
    List<SalesData> getDailySales(LocalDate startDate, LocalDate endDate, Long productId);
}


