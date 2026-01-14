<script setup>
import { onMounted, ref, computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { fetchOrderDetail } from '../../api/order';
import dayjs from 'dayjs';

const route = useRoute();
const router = useRouter();
const order = ref(null);
const loading = ref(false);
const errorMsg = ref('');

const statusMap = {
  0: { label: '待支付', type: 'warning' },
  1: { label: '已支付', type: 'success' },
  2: { label: '已取消', type: 'info' },
};

const load = async () => {
  loading.value = true;
  try {
    order.value = await fetchOrderDetail(route.params.id);
    errorMsg.value = '';
  } catch (e) {
    errorMsg.value = e?.msg || e?.message || '订单详情获取失败';
  } finally {
    loading.value = false;
  }
};

onMounted(load);

const formatTime = (t) => (t ? dayjs(t).format('YYYY-MM-DD HH:mm') : '-');
const statusTag = computed(() => statusMap[order.value?.status] || {});
</script>

<template>
  <div class="page" v-loading="loading">
    <el-breadcrumb separator="/">
      <el-breadcrumb-item to="/">首页</el-breadcrumb-item>
      <el-breadcrumb-item to="/orders">我的订单</el-breadcrumb-item>
      <el-breadcrumb-item>订单详情</el-breadcrumb-item>
    </el-breadcrumb>

    <el-alert
      v-if="errorMsg"
      type="error"
      :title="errorMsg"
      :closable="false"
      show-icon
      style="margin: 10px 0"
    />

    <el-card v-if="order" class="glass detail-card">
      <h3>订单号：{{ order.orderNo }}</h3>
      <el-descriptions :column="2" border class="desc-dark" style="margin-top: 12px">
        <el-descriptions-item label="商品">{{ order.productName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="statusTag.type">{{ statusTag.label || '未知' }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="秒杀价">￥{{ order.seckillPrice }}</el-descriptions-item>
        <el-descriptions-item label="数量">{{ order.quantity }}</el-descriptions-item>
        <el-descriptions-item label="总金额">￥{{ order.totalAmount }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ formatTime(order.createTime) }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ formatTime(order.updateTime) }}</el-descriptions-item>
      </el-descriptions>
      <div style="margin-top: 12px">
        <el-button type="primary" plain @click="router.push('/orders')">返回列表</el-button>
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.detail-card {
  color: #e2e8f0;
}

.desc-dark :deep(.el-descriptions__label),
.desc-dark :deep(.el-descriptions__content) {
  background: rgba(15, 23, 42, 0.6);
  color: #e2e8f0;
  border-color: rgba(255, 255, 255, 0.08);
}
</style>
