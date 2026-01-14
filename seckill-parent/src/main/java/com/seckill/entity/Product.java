package com.seckill.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品实体类
 * 对应HBase表: product_info
 */
@Data
@TableName("t_product")
public class Product {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 商品名称 (对应HBase cf_base:name)
     */
    private String productName;
    
    /**
     * 商品描述 (对应HBase cf_detail:description)
     */
    private String productDesc;
    
    /**
     * 价格 (对应HBase cf_base:price)
     */
    private BigDecimal price;
    
    /**
     * 成本价 (对应HBase cf_base:cost)
     */
    private BigDecimal cost;
    
    /**
     * 总库存 (对应HBase cf_stock:total_stock)
     */
    private Integer stock;
    
    /**
     * 品类编号 (对应HBase cf_base:category)
     */
    private Long categoryId;
    
    /**
     * 品牌 (对应HBase cf_base:brand)
     */
    private String brand;
    
    /**
     * 图片URL JSON数组 (对应HBase cf_detail:images)
     * 格式: ["url1", "url2", ...]
     */
    private String images;
    
    /**
     * 图片URL (保留原有字段，兼容性)
     */
    private String imgUrl;
    
    /**
     * 规格参数 JSON格式 (对应HBase cf_detail:spec)
     */
    private String spec;
    
    /**
     * 标签 逗号分隔 (对应HBase cf_detail:tags)
     */
    private String tags;
    
    /**
     * 各仓库库存 JSON格式 (对应HBase cf_stock:warehouse_stock)
     * 格式: {"warehouse_id1":数量, "warehouse_id2":数量}
     */
    private String warehouseStock;
    
    /**
     * 安全库存 (对应HBase cf_stock:safe_stock)
     */
    private Integer safeStock;
    
    /**
     * 锁定库存 (对应HBase cf_stock:lock_stock)
     */
    private Integer lockStock;
    
    /**
     * 浏览数 (对应HBase cf_stat:view_count)
     */
    private Long viewCount;
    
    /**
     * 销量 (对应HBase cf_stat:sale_count)
     */
    private Long saleCount;
    
    /**
     * 收藏数 (对应HBase cf_stat:collect_count)
     */
    private Long collectCount;
    
    /**
     * 状态 1-上架 0-下架 (对应HBase cf_base:status)
     */
    private Integer status;
    
    /**
     * 创建时间 (对应HBase cf_base:create_time)
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    /**
     * 更新时间 (对应HBase cf_stat:update_time)
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    
    /**
     * 是否删除：0-未删除，1-已删除
     */
    @TableLogic
    private Integer deleted;
}