<script setup>
import { useRouter, useRoute, RouterLink, RouterView } from 'vue-router';
import { useUserStore } from '../../store/user';
import { ElMessage } from 'element-plus';

const router = useRouter();
const route = useRoute();
const userStore = useUserStore();

if (!userStore.isLoggedIn || userStore.roleCode !== 1) {
  ElMessage.warning('请先使用管理员账号登录');
  router.replace('/login');
}

const menus = [
  { path: '/admin', label: '控制台' },
  { path: '/admin/products', label: '商品管理' },
  { path: '/admin/categories', label: '分类管理' },
  { path: '/admin/activities', label: '秒杀活动' },
  { path: '/admin/users', label: '用户管理' },
  { path: '/admin/orders', label: '订单管理' },
  { path: '/admin/sales', label: '销售分析' },
];
</script>

<template>
  <div class="admin">
    <aside class="sidebar">
      <div class="logo">
        <span class="dot"></span>
        <span>后台管理</span>
      </div>
      <nav class="menu">
        <RouterLink
          v-for="item in menus"
          :key="item.path"
          :to="item.path"
          class="menu-item"
          :class="{ active: route.path === item.path }"
        >
          {{ item.label }}
        </RouterLink>
      </nav>
      <el-button type="primary" size="small" @click="router.push('/')">返回前台</el-button>
    </aside>
    <section class="panel">
      <header class="panel-header">
        <h3>管理员工作台</h3>
        <div class="muted">{{ userStore.username }}</div>
      </header>
      <div class="panel-body">
        <RouterView />
      </div>
    </section>
  </div>
</template>

<style scoped>
.admin {
  min-height: 100vh;
  display: grid;
  grid-template-columns: 230px 1fr;
  background: #f8fafc;
}

.sidebar {
  background: linear-gradient(180deg, #ffffff, #f1f5f9);
  color: #1e293b;
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  border-right: 1px solid rgba(0, 0, 0, 0.1);
}

.logo {
  display: flex;
  align-items: center;
  gap: 10px;
  font-weight: 700;
}

.dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: linear-gradient(135deg, #a855f7, #06b6d4);
  box-shadow: 0 0 12px rgba(6, 182, 212, 0.5);
}

.menu {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.menu-item {
  padding: 10px 12px;
  border-radius: 8px;
  color: #475569;
  transition: background 0.2s;
}

.menu-item:hover {
  background: rgba(0, 0, 0, 0.06);
}

.menu-item.active {
  background: #2563eb;
  color: #fff;
}

.panel {
  background: #f8fafc;
}

.panel-header {
  padding: 16px 20px;
  background: rgba(255, 255, 255, 0.8);
  border-bottom: 1px solid rgba(0, 0, 0, 0.1);
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: #1e293b;
}

.panel-body {
  padding: 16px 20px 32px;
}
</style>
