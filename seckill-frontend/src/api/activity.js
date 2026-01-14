import request from './request';

export const fetchActivities = (params) => request.get('/api/seckill/activity/list', { params });

export const fetchActivityDetail = (id) => request.get(`/api/seckill/activity/${id}`);

export const createActivity = (data) => request.post('/api/seckill/activity', data);

export const updateActivity = (id, data) => request.put(`/api/seckill/activity/${id}`, data);

export const updateActivityStatus = (id, status) =>
  request.put(`/api/seckill/activity/status/${id}`, null, { params: { status } });

export const fetchActiveActivities = () => request.get('/api/seckill/activity/active');
