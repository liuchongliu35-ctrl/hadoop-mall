package com.seckill.mapper;

import com.seckill.entity.CartData;

public interface CartMapper {
    /**
     * 将购物车数据保存或更新到HBase
     * @param cartData 包含用户ID和购物车所有商品信息的实体
     * @return 1 表示成功, 0 表示失败
     */
    int saveOrUpdate(CartData cartData);
    /**
     * 从HBase中根据用户ID查找购物车数据
     * @param userId 用户ID
     * @return CartData 实体
     */
    CartData findByUserId(Long userId);
    /**
     * 从HBase中逻辑删除用户的购物车数据
     * @param userId 用户ID
     * @return 1 表示成功, 0 表示失败
     */
    int deleteByUserId(Long userId);
}
