import { defineStore } from 'pinia';
import { login, register, getProfile } from '../api/user';

const TOKEN_KEY = 'seckill_token';
const USER_KEY = 'seckill_user';

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem(TOKEN_KEY) || '',
    user: localStorage.getItem(USER_KEY) ? JSON.parse(localStorage.getItem(USER_KEY)) : null,
    loading: false,
  }),
  getters: {
    isLoggedIn: (state) => !!state.token,
    username: (state) => state.user?.username || '',
    roleCode: (state) => state.user?.role,
    roleDesc: (state) => (state.user?.role === 1 ? '管理员' : '用户'),
  },
  actions: {
    setToken(token) {
      this.token = token;
      if (token) {
        localStorage.setItem(TOKEN_KEY, token);
      } else {
        localStorage.removeItem(TOKEN_KEY);
      }
    },
    setUser(user) {
      this.user = user;
      if (user) {
        localStorage.setItem(USER_KEY, JSON.stringify(user));
      } else {
        localStorage.removeItem(USER_KEY);
      }
    },
    async login(payload) {
      this.loading = true;
      try {
        const res = await login(payload);
        if (res?.token) {
          this.setToken(res.token);
          this.setUser(res.user);
        }
        return res;
      } finally {
        this.loading = false;
      }
    },
    async register(payload) {
      this.loading = true;
      try {
        return await register(payload);
      } finally {
        this.loading = false;
      }
    },
    async fetchProfile() {
      if (!this.token) return null;
      const data = await getProfile();
      this.setUser(data);
      return data;
    },
    logout() {
      this.setToken('');
      this.setUser(null);
    },
  },
});
