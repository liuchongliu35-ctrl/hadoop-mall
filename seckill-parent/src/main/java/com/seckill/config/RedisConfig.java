package com.seckill.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule; // 关键依赖
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    /**
     * 自定义 RedisTemplate，仅当容器中无该 Bean 时生效
     * 解决自动装配问题：依赖 Spring 自动配置的 RedisConnectionFactory
     */
    @Bean
    @ConditionalOnMissingBean(name = "redisTemplate") // 避免与默认 RedisTemplate 冲突
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        // 1. 定义 ObjectMapper
        ObjectMapper mapper = new ObjectMapper();

        // 【关键步骤】注册 Java 8 时间模块
        mapper.registerModule(new JavaTimeModule());

        // 可选：禁用将日期写为时间戳（这样存入 Redis 的是 "2024-01-06T20:30:00" 字符串，而不是数组，可读性更好）
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        // 设置可见性（允许序列化私有字段）
        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);

        // 激活默认类型信息（为了反序列化时能知道是哪个类）
        // Spring Boot 2.x/3.x 推荐写法
        mapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL);
        // 2. 使用配置好的 ObjectMapper 创建序列化器
        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(mapper);
        // 3. 设置 Key 和 Value 的序列化方式
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(serializer);

        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(serializer);
        template.afterPropertiesSet();
        return template;
    }
}