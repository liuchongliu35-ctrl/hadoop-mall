package com.seckill.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 实时销售看板 VO
 */
@Data
public class SalesDashboardVO {

    private LocalDate date;

    /**
     * 订单数
     */
    private Long orderCount;

    /**
     * 总销售额
     */
    private BigDecimal totalAmount;
}


