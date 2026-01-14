package com.seckill.service.impl;

import com.seckill.mapper.CartRedisDAO;
import com.seckill.service.CartService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seckill.dto.*;
import com.seckill.entity.CartData;
import com.seckill.entity.Product; // 假设你有一个Product实体
import com.seckill.mapper.CartMapper;
import com.seckill.service.CartService;
import com.seckill.service.ProductService; // 假设你有一个ProductService来获取商品信息
import com.seckill.vo.ProductVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class CartServiceImpl implements CartService {
    @Autowired
    private CartRedisDAO cartRedisDAO;
    @Autowired
    private CartMapper cartMapper;
    @Autowired
    private ObjectMapper objectMapper;
    // 注入ProductService以获取商品详细信息，这是构建购物车视图所必需的
    @Autowired
    private ProductService productService;
    @Override
    public void addItemToCart(Long userId, CartItemAddDTO itemDTO) {
        // 检查商品是否存在、库存是否充足等
         ProductVO product = productService.getProductById(Long.valueOf(itemDTO.getProductId()));
         if (product == null || product.getStock() < itemDTO.getQuantity()) {
             throw new RuntimeException("商品不存在或库存不足");
         }

        CartItemDTO item = new CartItemDTO(itemDTO.getQuantity(), System.currentTimeMillis());

        // 1. 实时操作Redis
        cartRedisDAO.addItem(userId, itemDTO.getProductId(), item);

        // 2. 触发异步任务，将购物车数据同步到HBase
        this.syncCartToHBase(userId);
    }
    @Override
    public CartViewDTO getCartView(Long userId) {
        // 1. 永远从Redis读取实时购物车数据
        Map<String, CartItemDTO> redisCart = cartRedisDAO.getCart(userId);

        CartViewDTO cartView = new CartViewDTO();
        List<CartItemViewDTO> itemViews = new ArrayList<>();
        BigDecimal totalPrice = BigDecimal.ZERO;
        int totalItems = 0;
        if (redisCart.isEmpty()) {
            cartView.setItems(itemViews);
            cartView.setTotalItems(0);
            cartView.setTotalPrice(BigDecimal.ZERO);
            return cartView;
        }

        // 2. 遍历购物车，并聚合商品信息以构建视图
        for (Map.Entry<String, CartItemDTO> entry : redisCart.entrySet()) {
            String productId = entry.getKey();
            CartItemDTO cartItem = entry.getValue();
            // ！！！这里需要调用商品服务获取商品详细信息！！！
            ProductVO product = productService.getProductById(Long.valueOf(productId));
            if (product == null) {
                log.warn("购物车中商品ID {} 不存在，跳过展示", productId);
                continue; // 如果商品被下架或删除，不在购物车中显示
            }

            CartItemViewDTO itemView = new CartItemViewDTO();
            itemView.setProductId(productId);
            itemView.setName(product.getProductName());
             itemView.setImage(product.getImgUrl());
            itemView.setPrice(product.getPrice());
            itemView.setQuantity(cartItem.getQuantity());

            BigDecimal subTotal = product.getPrice().multiply(new BigDecimal(cartItem.getQuantity()));
            itemView.setSubTotal(subTotal);

            itemViews.add(itemView);
            totalPrice = totalPrice.add(subTotal);
            totalItems += cartItem.getQuantity();
        }

        cartView.setItems(itemViews);
        cartView.setTotalItems(totalItems);
        cartView.setTotalPrice(totalPrice);

        return cartView;
    }
    @Override
    public void updateItemQuantity(Long userId, CartItemUpdateDTO itemDTO) {
        log.info("用户id为：{}",userId);
        if (itemDTO.getQuantity() <= 0) {
            // 如果数量小于等于0，则视为删除操作
            removeItemFromCart(userId, itemDTO.getProductId());
            return;
        }

        // 1. 实时操作Redis
        cartRedisDAO.updateItemQuantity(userId, itemDTO.getProductId(), itemDTO.getQuantity());
        // 2. 触发异步同步
        this.syncCartToHBase(userId);
    }
    @Override
    public void removeItemFromCart(Long userId, String productId) {
        // 1. 实时操作Redis
        cartRedisDAO.removeItem(userId, productId);

        // 2. 触发异步同步
        this.syncCartToHBase(userId);
    }
    @Override
    public void clearCart(Long userId) {
        // 1. 实时操作Redis
        cartRedisDAO.clearCart(userId);

        // 2. 对于清空操作，可以直接异步删除HBase中的记录，更直接
        log.info("用户 {} 清空购物车，准备异步删除HBase数据", userId);
        // 注意：这里也应该是异步的，避免阻塞主线程
        cartMapper.deleteByUserId(userId);
    }

    /**
     * 异步将购物车数据从Redis持久化到HBase。
     * 使用@Async注解，此方法将在独立的线程中执行，不会阻塞主业务流程。
     * 注意：@Async方法不能是private，且调用方和被调用方不能在同一个类中直接通过`this`调用，
     * 否则AOP代理会失效。最规范的做法是将其放在一个独立的@Component中。
     * 但在当前类中作为public方法，通过代理对象调用通常也能生效。
     */
    @Async
    @Override
    public void syncCartToHBase(Long userId) {
        log.info("开始异步同步用户 {} 的购物车到HBase...", userId);
        try {
            // 从Redis获取最新购物车数据
            Map<String, CartItemDTO> redisCart = cartRedisDAO.getCart(userId);

            if (redisCart.isEmpty()) {
                // 如果Redis中已无数据，则逻辑删除HBase中的记录
                cartMapper.deleteByUserId(userId);
                log.info("用户 {} 的购物车为空，已在HBase中标记为删除", userId);
                return;
            }

            // 构建HBase实体
            CartData cartData = new CartData();
            cartData.setUserId(userId);
            cartData.setCartItems(objectMapper.writeValueAsString(redisCart));
            cartData.setUpdateTime(LocalDateTime.now());

            // 保存到HBase
            cartMapper.saveOrUpdate(cartData);
            log.info("成功将用户 {} 的购物车同步到HBase", userId);

        } catch (Exception e) {
            log.error("异步同步用户 {} 的购物车到HBase时发生错误", userId, e);
            // 可以在此处加入重试机制或告警通知
        }
    }
}
