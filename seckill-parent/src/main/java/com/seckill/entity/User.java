package com.seckill.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用户实体类
 * 对应HBase表: user_profile
 */
@Data
@TableName("t_user")
public class User {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 用户名 (对应HBase cf_base:username)
     */
    private String username;
    
    /**
     * 密码
     */
    private String password;
    
    /**
     * 昵称 (对应HBase cf_base:nickname)
     */
    private String nickname;
    
    /**
     * 手机号 (对应HBase cf_base:phone)
     */
    private String phone;
    
    /**
     * 邮箱 (对应HBase cf_base:email)
     */
    private String email;
    
    /**
     * 性别 (对应HBase cf_base:gender)
     */
    private String gender;
    
    /**
     * 生日 (对应HBase cf_base:birthday)
     */
    private LocalDate birthday;
    
    /**
     * 角色 1-管理员 2-普通用户
     */
    private Integer role;
    
    /**
     * 状态 1-正常 0-禁用 (对应HBase cf_base:status)
     */
    private Integer status;
    
    /**
     * 会员等级 (对应HBase cf_account:level)
     */
    private Integer level;
    
    /**
     * 积分 (对应HBase cf_account:points)
     */
    private Long points;
    
    /**
     * 余额 (对应HBase cf_account:balance)
     */
    private BigDecimal balance;
    
    /**
     * 成长值 (对应HBase cf_account:growth_value)
     */
    private Long growthValue;
    
    /**
     * 最后登录时间 (对应HBase cf_behavior:last_login)
     */
    private LocalDateTime lastLogin;
    
    /**
     * 最后登录IP (对应HBase cf_behavior:last_login_ip)
     */
    private String lastLoginIp;
    
    /**
     * 登录次数 (对应HBase cf_behavior:login_count)
     */
    private Long loginCount;
    
    /**
     * 累计消费金额 (对应HBase cf_behavior:total_order_amount)
     */
    private BigDecimal totalOrderAmount;
    
    /**
     * 注册时间 (对应HBase cf_base:register_time)
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