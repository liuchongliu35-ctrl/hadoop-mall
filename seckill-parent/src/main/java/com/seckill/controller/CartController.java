package com.seckill.controller;

import com.seckill.common.Result;
import com.seckill.dto.CartItemAddDTO;
import com.seckill.dto.CartItemUpdateDTO;
import com.seckill.dto.CartViewDTO;
import com.seckill.service.CartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/cart")
public class CartController {
    @Autowired
    private CartService cartService;

    /**
     * 获取当前用户的购物车
     */
    @GetMapping("/cartList")
    public Result<CartViewDTO> getMyCart(@RequestAttribute("userId") Long userId) {
        CartViewDTO cartView = cartService.getCartView(userId);
        return Result.success(cartView);
    }

    /**
     * 添加商品到购物车
     */
    @PostMapping("/add")
    public Result<?> addItem(@Validated @RequestBody CartItemAddDTO itemDTO,@RequestAttribute("userId") Long userId) {
        cartService.addItemToCart(userId, itemDTO);
        return Result.success();
    }
    /**
     * 更新购物车中商品的数量
     */
    @PutMapping("/update")
    public Result<?> updateItem(@Validated @RequestBody CartItemUpdateDTO itemDTO,@RequestAttribute("userId") Long userId) {
        cartService.updateItemQuantity(userId, itemDTO);
        return Result.success();
    }

    /**
     * 从购物车中删除单个商品
     * @param productId 商品ID
     */
    @DeleteMapping("/items/{productId}")
    public Result<?> removeItem(@PathVariable String productId,@RequestAttribute("userId") Long userId) {
        cartService.removeItemFromCart(userId, productId);
        return Result.success();
    }

    /**
     * 清空购物车
     */
    @DeleteMapping("removeAll")
    public Result<?> clearCart(@RequestAttribute("userId") Long userId) {
        cartService.clearCart(userId);
        return Result.success();
    }

}
