import dayjs from 'dayjs';
import request from './request';

// 获取今日销售看板数据
export function fetchTodayDashboard() {
  return request({
    url: '/api/sales/dashboard/today',
    method: 'get',
  });
}

// 获取今日热门商品排行
export function fetchTodayHotProducts(params) {
  return request({
    url: '/api/sales/hot/daily',
    method: 'get',
    params,
  });
}

// 获取历史每日销售数据
export function fetchDailySalesHistory(params) {
  return request({
    url: '/api/sales/history/daily',
    method: 'get',
    params,
  });
}