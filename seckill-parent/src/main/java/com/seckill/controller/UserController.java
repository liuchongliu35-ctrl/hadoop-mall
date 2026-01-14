package com.seckill.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.seckill.common.PageQuery;
import com.seckill.common.Result;
import com.seckill.dto.UserLoginDTO;
import com.seckill.dto.UserRegisterDTO;
import com.seckill.enums.UserRoleEnum;
import com.seckill.service.UserService;
import com.seckill.util.JwtUtil;
import com.seckill.vo.LoginVO;
import com.seckill.vo.UserVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserService userService;
    
    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/register")
    public Result<?> register(@RequestBody UserRegisterDTO registerDTO) {
        userService.register(registerDTO);
        return Result.success("注册成功");
    }

    @PostMapping("/login")
    public Result<LoginVO> login(@RequestBody UserLoginDTO loginDTO) {
        LoginVO loginVO = userService.login(loginDTO);
        return Result.success(loginVO);
    }

    @GetMapping("/current")
    public Result<UserVO> getCurrentUser(HttpServletRequest request) {
        String token = getTokenFromRequest(request);
        UserVO userVO = userService.getCurrentUser(token);
        return Result.success(userVO);
    }

    @GetMapping("/list")
    public Result<List<UserVO>> getUserList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String username,
            HttpServletRequest request) {
        
        // 检查权限
        String token = getTokenFromRequest(request);
        if (!jwtUtil.validateToken(token)) {
            return Result.unauthorized("未登录");
        }
        
        Integer role = jwtUtil.getRole(token);
        if (!role.equals(UserRoleEnum.ADMIN.getCode())) {
            return Result.forbidden("无权限访问");
        }
        
        PageQuery pageQuery = new PageQuery();
        pageQuery.setPageNum(pageNum);
        pageQuery.setPageSize(pageSize);
        
        List<UserVO> result = userService.getUserList(pageQuery, username);
        log.info("用户列表如下：{}", result);
        return Result.success(result);
    }

    @PutMapping("/status/{id}")
    public Result<?> updateUserStatus(
            @PathVariable Long id,
            @RequestParam Integer status,
            HttpServletRequest request) {
        
        // 检查权限
        String token = getTokenFromRequest(request);
        if (!jwtUtil.validateToken(token)) {
            return Result.unauthorized("未登录");
        }
        
        Integer role = jwtUtil.getRole(token);
        if (!role.equals(UserRoleEnum.ADMIN.getCode())) {
            return Result.forbidden("无权限访问");
        }
        
        userService.updateUserStatus(id, status);
        return Result.success("更新成功");
    }

    private String getTokenFromRequest(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }
}