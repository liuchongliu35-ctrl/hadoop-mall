import request from './request';

// 获取当前用户的购物车视图
export function fetchMyCart() {
  return request({
    url: '/api/cart/cartList',
    method: 'get',
  });
}

// 添加商品到购物车
export function addItemToCart(data) {
  return request({
    url: '/api/cart/add',
    method: 'post',
    data,
  });
}

// 更新购物车中商品的数量
export function updateCartItem(data) {
  return request({
    url: '/api/cart/update',
    method: 'put',
    data,
  });
}

// 从购物车中移除单个商品
export function removeCartItem(productId) {
  return request({
    url: `/api/cart/items/${productId}`,
    method: 'delete',
  });
}

// 清空用户的购物车
export function clearCart() {
  return request({
    url: '/api/cart/removeAll',
    method: 'delete',
  });
}
