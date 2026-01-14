package com.seckill.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 购物车实体类
 * 对应HBase表: cart_data
 * 注意: 购物车数据主要存储在Redis中，HBase用于持久化备份
 */
@Data
@TableName("cart_data")
public class CartData {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 购物车商品明细 JSON格式 (对应HBase cf_items:product_1001, product_1002...)
     * 格式: {
     *   "product_1001": {"product_id":"1001_20190001","name":"商品名","price":100,"quantity":2,"selected":1},
     *   "product_1002": {...}
     * }
     */
    private String cartItems;
    
    /**
     * 最后更新时间 (对应HBase cf_items:update_time)
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    
    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    /**
     * 是否删除：0-未删除，1-已删除
     */
    @TableLogic
    private Integer deleted;
}

