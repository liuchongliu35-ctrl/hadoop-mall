package com.seckill.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class RedisUtil {

    private final RedisTemplate<String, Object> redisTemplate;
    private final RedissonLockUtil redissonLockUtil;

    public RedisUtil(RedisTemplate<String, Object> redisTemplate, RedissonLockUtil redissonLockUtil) {
        this.redisTemplate = redisTemplate;
        this.redissonLockUtil = redissonLockUtil;
    }

    public void set(String key, Object value) {
        redisTemplate.opsForValue().set(key, value);
    }

    public void set(String key, Object value, long timeout, TimeUnit timeUnit) {
        redisTemplate.opsForValue().set(key, value, timeout, timeUnit);
    }

    public Object get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    public Boolean delete(String key) {
        return redisTemplate.delete(key);
    }

    public Boolean hasKey(String key) {
        return redisTemplate.hasKey(key);
    }

    public Boolean expire(String key, long timeout, TimeUnit timeUnit) {
        return redisTemplate.expire(key, timeout, timeUnit);
    }

    public Long getExpire(String key) {
        return redisTemplate.getExpire(key);
    }

    public Long increment(String key) {
        return redisTemplate.opsForValue().increment(key);
    }

    public Long increment(String key, long delta) {
        return redisTemplate.opsForValue().increment(key, delta);
    }

    public Long decrement(String key) {
        return redisTemplate.opsForValue().decrement(key);
    }

    public Long decrement(String key, long delta) {
        return redisTemplate.opsForValue().decrement(key, delta);
    }

    public Boolean setIfAbsent(String key, Object value) {
        return redisTemplate.opsForValue().setIfAbsent(key, value);
    }

    public Boolean setIfAbsent(String key, Object value, long timeout, TimeUnit timeUnit) {
        return redisTemplate.opsForValue().setIfAbsent(key, value, timeout, timeUnit);
    }

    // ========================= Redisson分布式锁方法 =========================

    /**
     * 获取分布式锁
     * @param lockKey 锁的key
     * @return 是否获取成功
     */
    public boolean tryLock(String lockKey) {
        return redissonLockUtil.tryLock(lockKey);
    }

    /**
     * 获取分布式锁
     * @param lockKey 锁的key
     * @param leaseTime 锁的持有时间
     * @param timeUnit 时间单位
     * @return 是否获取成功
     */
    public boolean tryLock(String lockKey, long leaseTime, TimeUnit timeUnit) {
        return redissonLockUtil.tryLock(lockKey, leaseTime, timeUnit);
    }

    /**
     * 获取分布式锁
     * @param lockKey 锁的key
     * @param waitTime 等待时间
     * @param leaseTime 锁的持有时间
     * @param timeUnit 时间单位
     * @return 是否获取成功
     */
    public boolean tryLock(String lockKey, long waitTime, long leaseTime, TimeUnit timeUnit) {
        return redissonLockUtil.tryLock(lockKey, waitTime, leaseTime, timeUnit);
    }

    /**
     * 释放分布式锁
     * @param lockKey 锁的key
     */
    public void unlock(String lockKey) {
        redissonLockUtil.unlock(lockKey);
    }

    /**
     * 检查是否持有分布式锁
     * @param lockKey 锁的key
     * @return 是否持有锁
     */
    public boolean isLocked(String lockKey) {
        return redissonLockUtil.isLocked(lockKey);
    }

    /**
     * 执行带锁的业务逻辑
     * @param lockKey 锁的key
     * @param action 业务逻辑
     * @return 执行结果
     */
    public <T> T executeWithLock(String lockKey, java.util.function.Supplier<T> action) {
        return redissonLockUtil.executeWithLock(lockKey, action);
    }

    /**
     * 执行带锁的业务逻辑
     * @param lockKey 锁的key
     * @param leaseTime 锁的持有时间
     * @param timeUnit 时间单位
     * @param action 业务逻辑
     * @return 执行结果
     */
    public <T> T executeWithLock(String lockKey, long leaseTime, TimeUnit timeUnit, 
                                java.util.function.Supplier<T> action) {
        return redissonLockUtil.executeWithLock(lockKey, leaseTime, timeUnit, action);
    }

    /**
     * 执行带锁的业务逻辑
     * @param lockKey 锁的key
     * @param waitTime 等待时间
     * @param leaseTime 锁的持有时间
     * @param timeUnit 时间单位
     * @param action 业务逻辑
     * @return 执行结果
     */
    public <T> T executeWithLock(String lockKey, long waitTime, long leaseTime, TimeUnit timeUnit, 
                                java.util.function.Supplier<T> action) {
        return redissonLockUtil.executeWithLock(lockKey, waitTime, leaseTime, timeUnit, action);
    }
}