package com.seckill.mapper.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.seckill.dto.CartItemDTO;
import com.seckill.mapper.CartRedisDAO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Repository
@Slf4j
public class CartRedisDAOImpl implements CartRedisDAO {

    private static final String CART_KEY_PREFIX = "cart:";
    private static final long CART_EXPIRE_DAYS = 7; // 购物车7天过期
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private ObjectMapper objectMapper;

    private String getCartKey(Long userId) {
        return CART_KEY_PREFIX + userId;
    }
//  todo 向缓存购物车添加商品
//    todo 实时性：没有写缓存，没有异步队列，没有等待写入磁盘，代码运行完这一行，Redis里就有数据了！！！
    @Override
    public void addItem(Long userId, String productId, CartItemDTO item) {
        String cartKey = getCartKey(userId);
        try {
            HashOperations<String, String, String> hasOps = stringRedisTemplate.opsForHash();
//        如果商品存在就累加数量
            if(hasOps.hasKey(cartKey,productId)){
                CartItemDTO existItem = getCartItem(userId, productId);
                if (existItem != null) {
                   item.setQuantity(item.getQuantity());
                }
            }
            String itemJson = objectMapper.writeValueAsString(item);
            hasOps.put(cartKey,productId,itemJson);
//            重置过期时间
            stringRedisTemplate.expire(cartKey,CART_EXPIRE_DAYS, TimeUnit.DAYS);
        } catch (JsonProcessingException e) {
            log.error("添加购物车到Redis失败, userId={}, productId={}", userId, productId, e);
        }
    }

//    从购物车删除商品
    @Override
    public void removeItem(Long userId, String productId) {stringRedisTemplate.opsForHash().delete(getCartKey(userId),productId);}

//  todo 从redis获取购物车数据
    @Override
    public Map<String, CartItemDTO> getCart(Long userId) {
        String cartKey = getCartKey(userId);
        Map<Object, Object> rawCart = stringRedisTemplate.opsForHash().entries(cartKey);
        if(rawCart.isEmpty()){
            return Collections.emptyMap();
        }
//        todo 将字符串Map转成HashMap,重点!!!
        return rawCart.entrySet().stream()
                .map(entry -> {
                    try {
                        // 1. 直接将 String 类型的 value 传给 readValue
                        CartItemDTO itemDTO = objectMapper.readValue((String) entry.getValue(), CartItemDTO.class);
                        // 2. 返回一个临时包装对象，包含 Key 和解析后的 Value
                        String key = String.valueOf(entry.getKey());
                        return new AbstractMap.SimpleEntry<>(key, itemDTO);
                    } catch (IOException e) {
                        log.error("解析Redis购物车商品失败, key={}, value={}", entry.getKey(), entry.getValue(), e);
                        // 解析失败则返回 null
                        return null;
                    }
                })
                // 3. 过滤掉所有解析失败的条目
                .filter(Objects::nonNull)
                // 4. 使用简洁的方法引用，不加任何强制类型转换
                .collect(Collectors.toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue));
    }

//    更新商品数量
    @Override
    public void updateItemQuantity(Long userId, String productId, int quantity) {
        CartItemDTO cartItem = getCartItem(userId, productId);
        if (cartItem!=null){
            cartItem.setQuantity(quantity);
            addItem(userId, productId, cartItem);
        }
    }
// todo 清空缓存中的购物车数据
    @Override
    public void clearCart(Long userId) {
        stringRedisTemplate.delete(getCartKey(userId));
    }

//    todo 根据商品ID从redis获取商品
    @Override
    public CartItemDTO getCartItem(Long userId, String productId) {
        try {
            String itemJson = (String)stringRedisTemplate.opsForHash().get(getCartKey(userId), productId);
            if (itemJson==null){
                return null;
            }
            return objectMapper.readValue(itemJson,CartItemDTO.class);
        } catch (JsonProcessingException e) {
            log.error("从Redis获取购物车商品失败, userId={}, productId={}", userId, productId, e);
            return null;
        }
    }
}
