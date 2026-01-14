package com.seckill.util;

import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Redisson分布式锁工具类
 * 提供基于Redisson的分布式锁功能，支持自动续期、可重入等特性
 */
@Slf4j
@Component
public class RedissonLockUtil {

    @Autowired
    private RedissonClient redissonClient;

    /**
     * 获取锁（默认锁时间30秒，不自动续期）
     * @param lockKey 锁的key
     * @return 是否获取成功
     */
    public boolean tryLock(String lockKey) {
        return tryLock(lockKey, 30, TimeUnit.SECONDS, false);
    }

    /**
     * 获取锁并自动续期
     * @param lockKey 锁的key
     * @param leaseTime 锁的持有时间
     * @param timeUnit 时间单位
     * @return 是否获取成功
     */
    public boolean tryLock(String lockKey, long leaseTime, TimeUnit timeUnit) {
        return tryLock(lockKey, leaseTime, timeUnit, true);
    }

    /**
     * 获取锁
     * @param lockKey 锁的key
     * @param leaseTime 锁的持有时间
     * @param timeUnit 时间单位
     * @param autoRenewal 是否自动续期
     * @return 是否获取成功
     */
    public boolean tryLock(String lockKey, long leaseTime, TimeUnit timeUnit, boolean autoRenewal) {
        try {
            RLock lock = redissonClient.getLock(lockKey);
            
            if (autoRenewal) {
                // 使用lock方法，会自动续期
                lock.lock(leaseTime, timeUnit);
                return true;
            } else {
                // 使用tryLock方法，不会自动续期
                return lock.tryLock(0, leaseTime, timeUnit);
            }
        } catch (InterruptedException e) {
            log.error("获取分布式锁失败: {}", lockKey, e);
            Thread.currentThread().interrupt();
            return false;
        } catch (Exception e) {
            log.error("获取分布式锁异常: {}", lockKey, e);
            return false;
        }
    }

    /**
     * 尝试获取锁（带等待时间）
     * @param lockKey 锁的key
     * @param waitTime 等待时间
     * @param leaseTime 锁的持有时间
     * @param timeUnit 时间单位
     * @return 是否获取成功
     */
    public boolean tryLock(String lockKey, long waitTime, long leaseTime, TimeUnit timeUnit) {
        try {
            RLock lock = redissonClient.getLock(lockKey);
            return lock.tryLock(waitTime, leaseTime, timeUnit);
        } catch (InterruptedException e) {
            log.error("获取分布式锁失败: {}", lockKey, e);
            Thread.currentThread().interrupt();
            return false;
        } catch (Exception e) {
            log.error("获取分布式锁异常: {}", lockKey, e);
            return false;
        }
    }

    /**
     * 释放锁
     * @param lockKey 锁的key
     */
    public void unlock(String lockKey) {
        try {
            RLock lock = redissonClient.getLock(lockKey);
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.debug("释放分布式锁成功: {}", lockKey);
            } else {
                log.warn("当前线程不持有锁: {}", lockKey);
            }
        } catch (Exception e) {
            log.error("释放分布式锁异常: {}", lockKey, e);
        }
    }

    /**
     * 检查是否持有锁
     * @param lockKey 锁的key
     * @return 是否持有锁
     */
    public boolean isLocked(String lockKey) {
        try {
            RLock lock = redissonClient.getLock(lockKey);
            return lock.isHeldByCurrentThread();
        } catch (Exception e) {
            log.error("检查锁状态异常: {}", lockKey, e);
            return false;
        }
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
        if (tryLock(lockKey, waitTime, leaseTime, timeUnit)) {
            try {
                return action.get();
            } finally {
                unlock(lockKey);
            }
        } else {
            throw new RuntimeException("获取锁失败: " + lockKey);
        }
    }

    /**
     * 执行带锁的业务逻辑（无等待时间）
     * @param lockKey 锁的key
     * @param leaseTime 锁的持有时间
     * @param timeUnit 时间单位
     * @param action 业务逻辑
     * @return 执行结果
     */
    public <T> T executeWithLock(String lockKey, long leaseTime, TimeUnit timeUnit, 
                                 java.util.function.Supplier<T> action) {
        return executeWithLock(lockKey, 0, leaseTime, timeUnit, action);
    }

    /**
     * 执行带锁的业务逻辑（默认30秒）
     * @param lockKey 锁的key
     * @param action 业务逻辑
     * @return 执行结果
     */
    public <T> T executeWithLock(String lockKey, java.util.function.Supplier<T> action) {
        return executeWithLock(lockKey, 30, TimeUnit.SECONDS, action);
    }
}