package com.seckill.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 秒杀订单实体类
 * 对应HBase表: order_history
 */
@Data
@TableName("seckill_order")
public class SeckillOrder {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 订单号
     */
    private String orderNo;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 活动ID
     */
    private Long activityId;
    
    /**
     * 商品ID
     */
    private Long productId;
    
    /**
     * 商品名称
     */
    private String productName;
    
    /**
     * 秒杀价格
     */
    private BigDecimal seckillPrice;
    
    /**
     * 购买数量
     */
    private Integer quantity;
    
    /**
     * 订单总金额 (对应HBase cf_base:total_amount)
     */
    private BigDecimal totalAmount;
    
    /**
     * 优惠金额 (对应HBase cf_base:discount_amount)
     */
    private BigDecimal discountAmount;
    
    /**
     * 实付金额 (对应HBase cf_base:actual_amount)
     */
    private BigDecimal actualAmount;
    
    /**
     * 订单状态 (对应HBase cf_base:status)
     * 1-待付款, 2-待发货, 3-已发货, 4-已完成, 5-已取消
     */
    private Integer status;
    
    /**
     * 支付方式 (对应HBase cf_base:pay_method)
     */
    private String payMethod;
    
    /**
     * 支付时间 (对应HBase cf_base:pay_time)
     */
    private LocalDateTime payTime;
    
    /**
     * 发货时间 (对应HBase cf_base:deliver_time)
     */
    private LocalDateTime deliverTime;
    
    /**
     * 完成时间 (对应HBase cf_base:complete_time)
     */
    private LocalDateTime completeTime;
    
    /**
     * 收货人 (对应HBase cf_address:receiver)
     */
    private String receiver;
    
    /**
     * 联系电话 (对应HBase cf_address:phone)
     */
    private String phone;
    
    /**
     * 详细地址 (对应HBase cf_address:address)
     */
    private String address;
    
    /**
     * 邮编 (对应HBase cf_address:postcode)
     */
    private String postcode;
    
    /**
     * 商品明细 JSON格式 (对应HBase cf_items:item_1, item_2...)
     * 格式: [{"product_id":"1001_20190001","name":"商品名","price":100,"quantity":2,"amount":200}, ...]
     * 支持多商品订单
     */
    private String orderItems;
    
    /**
     * 快递公司 (对应HBase cf_logistics:express_company)
     */
    private String expressCompany;
    
    /**
     * 快递单号 (对应HBase cf_logistics:express_no)
     */
    private String expressNo;
    
    /**
     * 物流轨迹 JSON数组 (对应HBase cf_logistics:logistics_info)
     */
    private String logisticsInfo;
    
    /**
     * 创建时间 (对应HBase cf_base:create_time)
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