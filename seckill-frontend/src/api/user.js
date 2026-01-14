import request from './request';

export const register = (data) => request.post('/api/user/register', data);

export const login = (data) => request.post('/api/user/login', data);

export const getProfile = () => request.get('/api/user/current');

export const fetchUsers = (params) => request.get('/api/user/list', { params });

export const updateUserStatus = (id, status) => request.put(`/api/user/status/${id}`, null, { params: { status } });
