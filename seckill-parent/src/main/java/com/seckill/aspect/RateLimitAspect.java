package com.seckill.aspect;

import com.seckill.annotation.RateLimit;
import com.seckill.common.BusinessException;
import com.seckill.common.LimitType;
import com.seckill.dto.SeckillOrderDTO;
import com.seckill.util.JwtUtil;
import com.seckill.util.RedisCache;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

@Aspect
@Component
@RequiredArgsConstructor
public class RateLimitAspect {
    private final RedisCache redisCache;
    @Autowired
    private StringRedisTemplate redisTemplate;
    private final JwtUtil jwtUtil;

    //    拦截所有带@RateLimit注解的方法
    @Before("@annotation(com.seckill.annotation.RateLimit)")
    public void doRateLimit(JoinPoint joinPoint) {
//        获取注解信息
        MethodSignature signature = (MethodSignature)joinPoint.getSignature();
        Method method = signature.getMethod();
        RateLimit rateLimit = method.getAnnotation(RateLimit.class);

//        构建限流Redis键（根据限流类型）
        String limitKey = buildLimitKey(rateLimit.limitType(), joinPoint);
//        todo 执行限流校验！！！核心

        checkLimit(limitKey, rateLimit.limit(), rateLimit.period(), rateLimit.timeunit(), rateLimit.message());
    }

    //    todo 构建限流键:有用户id和商品id组成
    private String buildLimitKey(LimitType limitType, JoinPoint joinPoint) {
//        获取request
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        String prefix="seckill:rateLimit:";
        switch (limitType){
            case USER_ACTIVITY:
//                获取用户id
                String token = request.getHeader("Authorization");
                if (token == null || !token.startsWith("Bearer ")) {
                    throw new BusinessException(401, "token格式不正确");
                }
                token = token.substring(7);
                if (!jwtUtil.validateToken(token)) { // 补充token有效性校验，避免无效token获取userId
                    throw new BusinessException(401, "token已失效");
                }
                Long userId = jwtUtil.getUserId(token);
//                通过经过切面的数据获取活动id
                Object[] args = joinPoint.getArgs();
                // 校验参数数组是否为空，避免数组下标越界
                if (args == null || args.length == 0) {
                    throw new BusinessException(400, "请求参数不能为空");
                }
                // 校验第一个参数是否为SeckillOrderDTO类型
                if (!(args[0] instanceof SeckillOrderDTO seckillOrderDTO)) {
                    throw new BusinessException(400, "请求参数类型错误，无法提取活动ID");
                }
                // 强转为SeckillOrderDTO，提取activityId
                Long activityId = seckillOrderDTO.getActivityId();
                // 校验activityId是否为空（避免DTO中activityId为null导致限流键异常）
                if (activityId == null) {
                    throw new BusinessException(400, "活动ID不能为空");
                }
                return prefix+"userActivity:"+userId+":"+activityId;
            case GLOBAL:
                // 限流键格式：seckill:rateLimit:global
                return prefix + "global";
            default:
                throw new BusinessException(400, "不支持的限流类型");
        }

    }

    //    todo 核心：限流校验,基于redis自增计数
    private void checkLimit(String limitKey, int limit, int period, TimeUnit timeUnit,String message){
//        1、redis自增计数（原子操作增加1）
        Long count = redisTemplate.opsForValue().increment(limitKey, 1);
//        2、第一次请求时设置键的过期时间
        if (count!=null && count==1){
            redisCache.expire(limitKey,period,timeUnit);
        }
//        3、超过阈值，抛出异常
        if (count !=null&&count>limit){
            throw new BusinessException(401,message);
        }
    }
}
