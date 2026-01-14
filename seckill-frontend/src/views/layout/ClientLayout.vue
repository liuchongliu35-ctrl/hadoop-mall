<script setup>
import { useRouter, useRoute, RouterLink, RouterView } from 'vue-router';
import { computed } from 'vue';
import { useUserStore } from '../../store/user';
import { ElMessageBox, ElMessage } from 'element-plus';

const router = useRouter();
const route = useRoute();
const userStore = useUserStore();

const navs = [
  { path: '/', label: '首页' },
  { path: '/seckill', label: '秒杀活动' },
  { path: '/products', label: '商品列表' },
  { path: '/categories', label: '分类' },
  { path: '/cart', label: '购物车' },
  { path: '/orders', label: '我的订单' },
];

const isActive = (path) => route.path === path;

const handleLogout = () => {
  ElMessageBox.confirm('确认退出登录吗？', '提示', { type: 'warning' })
    .then(() => {
      userStore.logout();
      ElMessage.success('已退出');
      router.push('/login');
    })
    .catch(() => {});
};

const welcome = computed(() => {
  if (!userStore.isLoggedIn) return '游客';
  const role = userStore.roleCode === 1 ? '管理员' : '用户';
  return `${userStore.username} (${role})`;
});
</script>

<template>
  <div class="layout">
    <header class="topbar">
      <div class="brand" @click="router.push('/')">
        <span class="dot"></span>
        <strong>闪秒抢购</strong>
      </div>
      <nav class="nav">
        <RouterLink
          v-for="item in navs"
          :key="item.path"
          :to="item.path"
          class="nav-item"
          :class="{ active: isActive(item.path) }"
        >
          {{ item.label }}
        </RouterLink>
        <RouterLink
          v-if="userStore.roleCode === 1"
          to="/admin"
          class="nav-item"
          :class="{ active: route.path.startsWith('/admin') }"
        >
          管理端
        </RouterLink>
      </nav>
      <div class="user">
        <span class="muted">你好，{{ welcome }}</span>
        <el-button v-if="!userStore.isLoggedIn" type="primary" @click="router.push('/login')">
          登录
        </el-button>
        <el-button v-if="!userStore.isLoggedIn" text @click="router.push('/register')">
          注册
        </el-button>
        <el-button v-else text @click="handleLogout">退出</el-button>
      </div>
    </header>
    <main class="content">
      <RouterView />
    </main>
  </div>
</template>

<style scoped>
.layout {
  min-height: 100vh;
}

.topbar {
  position: sticky;
  top: 0;
  z-index: 10;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 20px;
  background: rgba(15, 23, 42, 0.72);
  backdrop-filter: blur(14px);
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
  color: #e2e8f0;
}

.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 18px;
  cursor: pointer;
}

.dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: linear-gradient(135deg, #6c7cff, #00d9a6);
  box-shadow: 0 0 16px rgba(108, 124, 255, 0.9);
}

.nav {
  display: flex;
  gap: 12px;
}

.nav-item {
  padding: 8px 12px;
  border-radius: 12px;
  color: #cbd5e1;
  transition: all 0.2s;
}

.nav-item:hover {
  background: rgba(255, 255, 255, 0.08);
  color: #fff;
}

.nav-item.active {
  background: linear-gradient(135deg, #6c7cff, #00d9a6);
  color: #0b1224;
}

.user {
  display: flex;
  align-items: center;
  gap: 10px;
}

.content {
  padding: 16px 18px 40px;
}
</style>
