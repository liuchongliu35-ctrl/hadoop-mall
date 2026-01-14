package com.seckill.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seckill.common.Result;
import com.seckill.entity.User;
import com.seckill.util.JwtUtil;
import com.seckill.util.RedisCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private RedisCache redisCache;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        
        // 预检请求直接通过
        if ("OPTIONS".equals(request.getMethod())) {
            return true;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            writeErrorResponse(response, 401, "未登录或token格式错误");
            return false;
        }

        String token = authHeader.substring(7);
//        这个是使用jwt的会话管理工具
//        if (!jwtUtil.validateToken(token)) {
//            writeErrorResponse(response, 401, "token已失效");
//            return false;
//        }

        // 将用户信息放入请求头
        Long userId = jwtUtil.getUserId(token);
//        todo 这里我们改用redis来管理用户会话
        String key="login:"+userId;
        User user = redisCache.getCacheObject(key);
        if(Objects.isNull(user)){
            throw new RuntimeException("用户未登录");
        }
//        todo 会话续期
        redisCache.expire(key, 30, TimeUnit.MINUTES);
        String username = jwtUtil.getUsername(token);
        Integer role = jwtUtil.getRole(token);
        
        request.setAttribute("userId", userId);
        request.setAttribute("username", username);
        request.setAttribute("role", role);

        return true;
    }

    private void writeErrorResponse(HttpServletResponse response, int code, String msg) throws Exception {
        response.setStatus(200);
        response.setContentType("application/json;charset=UTF-8");
        ObjectMapper objectMapper = new ObjectMapper();
        String json = objectMapper.writeValueAsString(Result.error(code, msg));
        response.getWriter().write(json);
    }
}