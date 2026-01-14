<script setup>
import { onMounted } from 'vue';
import { useUserStore } from '../../store/user';
import { ElDescriptions, ElDescriptionsItem } from 'element-plus';

const userStore = useUserStore();

onMounted(() => {
  if (userStore.isLoggedIn && !userStore.user?.email) {
    userStore.fetchProfile();
  }
});
</script>

<template>
  <div class="page">
    <h3>个人信息</h3>
    <el-descriptions v-if="userStore.user" :column="1" border style="max-width: 520px">
      <el-descriptions-item label="用户名">
        {{ userStore.user.username }}
      </el-descriptions-item>
      <el-descriptions-item label="邮箱">
        {{ userStore.user.email || '-' }}
      </el-descriptions-item>
      <el-descriptions-item label="手机号">
        {{ userStore.user.phone || '-' }}
      </el-descriptions-item>
      <el-descriptions-item label="角色">
        {{ userStore.user.roleDesc || (userStore.user.role === 1 ? '管理员' : '用户') }}
      </el-descriptions-item>
      <el-descriptions-item label="注册时间">
        {{ userStore.user.createTime || '-' }}
      </el-descriptions-item>
    </el-descriptions>
    <div v-else class="muted">请先登录</div>
  </div>
</template>
