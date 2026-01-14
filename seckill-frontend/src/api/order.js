import request from './request';

export const createSeckillOrder = (data) => request.post('/api/seckill/order/create', data);

export const fetchUserOrders = (params) => request.get('/api/seckill/order/user/list', { params });

export const fetchOrderDetail = (orderId) => request.get(`/api/seckill/order/detail/${orderId}`);

export const cancelOrder = (orderId) => request.post(`/api/seckill/order/cancel/${orderId}`);

export const payOrder = (orderId) => request.post(`/api/seckill/order/pay/${orderId}`);

export const fetchAdminOrders = (params) => request.get('/api/seckill/order/list', { params });
