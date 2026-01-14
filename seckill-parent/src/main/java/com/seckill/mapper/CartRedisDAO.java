package com.seckill.mapper;

import com.seckill.dto.CartItemDTO;

import java.util.Map;

public interface CartRedisDAO {
    /**
     * 向购物车中添加商品
     * @param userId 用户ID
     * @param productId 商品ID
     * @param item 商品信息（数量等）
     */
    void addItem(Long userId, String productId, CartItemDTO item);
    /**
     * 从购物车中移除商品
     * @param userId 用户ID
     * @param productId 商品ID
     */
    void removeItem(Long userId, String productId);
    /**
     * 获取用户的整个购物车
     * @param userId 用户ID
     * @return Map<商品ID, 商品信息>
     */
    Map<String, CartItemDTO> getCart(Long userId);
    /**
     * 更新购物车中商品的数量
     * @param userId 用户ID
     * @param productId 商品ID
     * @param quantity 新的数量
     */
    void updateItemQuantity(Long userId, String productId, int quantity);

    /**
     * 清空用户的购物车
     * @param userId 用户ID
     */
    void clearCart(Long userId);
    /**
     * 获取购物车中单个商品信息
     * @param userId 用户ID
     * @param productId 商品ID
     * @return CartItemDTO
     */
    CartItemDTO getCartItem(Long userId, String productId);
}
