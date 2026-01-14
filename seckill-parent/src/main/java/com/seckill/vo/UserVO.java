package com.seckill.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserVO {
    
    private Long id;
    private String username;
    private String phone;
    private String email;
    private Integer role;
    private String roleDesc;
    private LocalDateTime createTime;
}