<script setup>
import { useUserStore } from '../../store/user';

const userStore = useUserStore();
</script>

<template>
  <div class="page dashboard">
    <div class="hero-card hoverable">
      <div>
        <p class="badge">Admin Panel</p>
        <h2>欢迎回来，{{ userStore.username || '管理员' }}</h2>
        <p class="muted">
          左侧切换商品、分类、活动、用户与订单管理。确保使用管理员 Token 进行写操作。
        </p>
      </div>
      <div class="numbers">
        <div class="block">
          <span class="label">角色</span>
          <strong>管理员</strong>
        </div>
        <div class="block">
          <span class="label">状态</span>
          <el-tag type="success">在线</el-tag>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.dashboard .hero-card {
  background: linear-gradient(135deg, #111827, #1f2937);
  color: #e5e7eb;
  display: flex;
  justify-content: space-between;
  gap: 20px;
  border-radius: 16px;
  padding: 18px;
  box-shadow: 0 20px 45px rgba(0, 0, 0, 0.35);
}

.badge {
  display: inline-block;
  padding: 6px 10px;
  background: rgba(59, 130, 246, 0.15);
  color: #bfdbfe;
  border-radius: 12px;
  font-weight: 700;
}

.numbers {
  display: flex;
  gap: 16px;
  align-items: center;
}

.block {
  background: rgba(255, 255, 255, 0.06);
  padding: 10px 14px;
  border-radius: 12px;
  min-width: 120px;
}

.label {
  display: block;
  color: #9ca3af;
  font-size: 12px;
}
</style>
