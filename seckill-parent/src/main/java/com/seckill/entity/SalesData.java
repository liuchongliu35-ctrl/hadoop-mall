package com.seckill.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 销售数据实体类
 * 对应HBase表: sales_data
 */
@Data
@TableName("sales_data")
public class SalesData {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 日期 (对应HBase cf_daily:date)
     */
    private LocalDate date;
    
    /**
     * 商品ID (对应HBase cf_daily:product_id)
     */
    private Long productId;
    
    /**
     * 品类ID (对应HBase cf_daily:category_id)
     */
    private Long categoryId;
    
    /**
     * 销售数量 (对应HBase cf_daily:sale_count)
     */
    private Long saleCount;
    
    /**
     * 销售金额 (对应HBase cf_daily:sale_amount)
     */
    private BigDecimal saleAmount;
    
    /**
     * 退货数量 (对应HBase cf_daily:refund_count)
     */
    private Long refundCount;
    
    /**
     * 退货金额 (对应HBase cf_daily:refund_amount)
     */
    private BigDecimal refundAmount;
    
    /**
     * 小时级销量数据 JSON格式 (对应HBase cf_hourly:hour_00 ~ hour_23)
     * 格式: {"hour_00":销量, "hour_01":销量, ..., "hour_23":销量}
     */
    private String hourlyData;
    
    /**
     * 区域销售数据 JSON格式 (对应HBase cf_region:region_beijing, region_shanghai...)
     * 格式: {"region_beijing":销量, "region_shanghai":销量, ...}
     */
    private String regionData;
    
    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    
    /**
     * 是否删除：0-未删除，1-已删除
     */
    @TableLogic
    private Integer deleted;
}

