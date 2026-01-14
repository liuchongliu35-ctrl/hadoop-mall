<script setup>
import { reactive, ref } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { useUserStore } from '../../store/user';
import { ElMessage } from 'element-plus';

const form = reactive({
  username: '',
  password: '',
});

const formRef = ref();

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
};

const router = useRouter();
const route = useRoute();
const userStore = useUserStore();

const onSubmit = async () => {
  if (!formRef.value) return;
  await formRef.value.validate(async (valid) => {
    if (!valid) return;
    try {
      await userStore.login(form);
      ElMessage.success('登录成功');
      const redirect = route.query.redirect || '/';
      router.replace(redirect);
    } catch (e) {
      // handled by interceptor
    }
  });
};
</script>

<template>
  <div class="auth">
    <div class="bg-shape"></div>
    <section class="card glass hoverable">
      <h2>账号登录</h2>
      <p class="muted">输入账号密码登录，管理秒杀与订单。</p>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent>
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" show-password placeholder="请输入密码" />
        </el-form-item>
        <el-button type="primary" :loading="userStore.loading" style="width: 100%" @click="onSubmit">
          登录
        </el-button>
        <div class="muted" style="margin-top: 12px">
          还没有账号？
          <el-link type="primary" @click="router.push('/register')">立即注册</el-link>
        </div>
      </el-form>
    </section>
  </div>
  <div class="floating-balls">
    <span></span><span></span><span></span>
  </div>
</template>

<style scoped>
.auth {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 80px 16px;
  overflow: hidden;
}

.bg-shape {
  position: absolute;
  inset: 0;
  background: radial-gradient(circle at 20% 20%, rgba(108, 124, 255, 0.2), transparent 35%),
    radial-gradient(circle at 80% 80%, rgba(0, 217, 166, 0.2), transparent 30%);
  filter: blur(40px);
  z-index: 0;
}

.card {
  position: relative;
  width: 420px;
  padding: 28px;
  border-radius: 16px;
  color: #e2e8f0;
  z-index: 1;
}

.floating-balls span {
  position: absolute;
  display: block;
  width: 160px;
  height: 160px;
  background: rgba(255, 255, 255, 0.04);
  border-radius: 50%;
  animation: float 10s ease-in-out infinite;
}
.floating-balls span:nth-child(1) {
  top: 10%;
  left: 15%;
}
.floating-balls span:nth-child(2) {
  bottom: 10%;
  right: 20%;
  animation-duration: 12s;
}
.floating-balls span:nth-child(3) {
  top: 30%;
  right: 40%;
  animation-duration: 14s;
}

@keyframes float {
  0% {
    transform: translateY(0) scale(1);
  }
  50% {
    transform: translateY(-10px) scale(1.05);
  }
  100% {
    transform: translateY(0) scale(1);
  }
}
</style>
