package com.seckill.service;

import com.seckill.dto.CartItemAddDTO;
import com.seckill.dto.CartItemUpdateDTO;
import com.seckill.dto.CartViewDTO;

public interface CartService {
    /**
     * 添加商品到购物车
     * @param userId 用户ID
     * @param itemDTO 商品信息
     */
    void addItemToCart(Long userId, CartItemAddDTO itemDTO);
    /**
     * 获取用户的购物车视图
     * @param userId 用户ID
     * @return 购物车视图对象
     */
    CartViewDTO getCartView(Long userId);
    /**
     * 更新购物车中商品的数量
     * @param userId 用户ID
     * @param itemDTO 更新的商品信息
     */
    void updateItemQuantity(Long userId, CartItemUpdateDTO itemDTO);
    /**
     * 从购物车中移除单个商品
     * @param userId 用户ID
     * @param productId 商品ID
     */
    void removeItemFromCart(Long userId, String productId);
    /**
     * 清空用户的购物车
     * @param userId 用户ID
     */
    void clearCart(Long userId);
    /**
     * [异步任务] 将指定用户的购物车数据从Redis同步到HBase
     * @param userId 用户ID
     */
    void syncCartToHBase(Long userId);
}
