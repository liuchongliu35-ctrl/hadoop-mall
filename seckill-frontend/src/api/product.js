import request from './request';

export const fetchProducts = (params) => request.get('/api/product/list', { params });

export const fetchProductDetail = (id) => request.get(`/api/product/${id}`);

export const createProduct = (data) => request.post('/api/product', data);

export const updateProduct = (id, data) => request.put(`/api/product/${id}`, data);

export const deleteProduct = (id) => request.delete(`/api/product/${id}`);

export const uploadProductImage = (file) => {
  const form = new FormData();
  form.append('file', file);
  // 让浏览器自动设置 multipart boundary
  return request.post('/api/product/upload', form);
};

export const fetchCategories = () => request.get('/api/product/category/list');

export const createCategory = (data) => request.post('/api/product/category', data);
