<script setup>
import { onMounted, ref } from 'vue';
import { useRoute } from 'vue-router';
import { fetchProductDetail } from '../../api/product';
import { fetchActiveActivities } from '../../api/activity';
import dayjs from 'dayjs';

const route = useRoute();
const product = ref(null);
const activities = ref([]);
const loading = ref(false);
const fallbackImages = [
  'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?auto=format&fit=crop&w=1200&q=60',
  'https://images.unsplash.com/photo-1498050108023-c5249f4df085?auto=format&fit=crop&w=1200&q=60',
  'https://images.unsplash.com/photo-1523275335684-37898b6baf30?auto=format&fit=crop&w=1200&q=60',
  'https://images.unsplash.com/photo-1512499617640-c2f999098c01?auto=format&fit=crop&w=1200&q=60',
];
const pickImage = (key) => fallbackImages[key % fallbackImages.length];

const load = async () => {
  loading.value = true;
  try {
    const [p, acts] = await Promise.all([
      fetchProductDetail(route.params.id),
      fetchActiveActivities(),
    ]);
    product.value = p;
    activities.value = (acts || []).filter((a) => a.productId === p?.id);
  } finally {
    loading.value = false;
  }
};

onMounted(load);

const formatTime = (t) => (t ? dayjs(t).format('YYYY-MM-DD HH:mm') : '-');
</script>

<template>
  <div class="page" v-loading="loading">
    <el-breadcrumb separator="/">
      <el-breadcrumb-item to="/">首页</el-breadcrumb-item>
      <el-breadcrumb-item to="/products">商品列表</el-breadcrumb-item>
      <el-breadcrumb-item>{{ product?.productName || '详情' }}</el-breadcrumb-item>
    </el-breadcrumb>

    <div v-if="product" class="panel">
      <div class="gallery glass">
        <el-image
          v-if="product.imgUrl"
          :src="product.imgUrl || pickImage(product.id)"
          fit="cover"
        >
          <template #error>
            <div class="image-placeholder">
              <span>图片加载失败</span>
            </div>
          </template>
        </el-image>
        <div v-else class="placeholder">暂无图片</div>
      </div>
      <div class="info">
        <h2>{{ product.productName }}</h2>
        <p class="muted">{{ product.productDesc }}</p>
        <div class="price">￥{{ product.price }}</div>
        <div class="muted small">库存：<span class="stock-number">{{ product.stock }}</span> · 分类：{{ product.categoryName || '-' }}</div>
        <div class="badge">商品 ID：{{ product.id }}</div>
      </div>
    </div>

    <el-divider />

    <div>
      <h4>相关秒杀</h4>
      <el-timeline v-if="activities.length">
        <el-timeline-item
          v-for="a in activities"
          :key="a.id"
          :timestamp="`${formatTime(a.startTime)} → ${formatTime(a.endTime)}`"
          :type="a.status === 1 ? 'success' : a.status === 0 ? 'info' : 'warning'"
        >
          <div class="flex-between">
            <div>
              <strong>{{ a.activityName }}</strong>
              <div class="muted small">秒杀价 ￥{{ a.seckillPrice }} · 库存 <span class="stock-number">{{ a.seckillStock }}</span></div>
            </div>
            <el-tag :type="a.status === 1 ? 'success' : a.status === 0 ? 'info' : 'warning'">
              {{ a.statusDesc || (a.status === 1 ? '进行中' : a.status === 0 ? '未开始' : '已结束') }}
            </el-tag>
          </div>
        </el-timeline-item>
      </el-timeline>
      <div v-else class="muted">当前商品暂无秒杀活动。</div>
    </div>
  </div>
</template>

<style scoped>
.panel {
  margin-top: 12px;
  display: grid;
  grid-template-columns: 1.2fr 1fr;
  gap: 16px;
  align-items: center;
}

.gallery {
  background: #fff;
  border-radius: 12px;
  padding: 12px;
  border: 1px solid #e5e7eb;
}

.gallery :deep(.el-image) {
  width: 100%;
  height: 320px;
  object-fit: cover;
  border-radius: 10px;
}

.placeholder {
  width: 100%;
  height: 320px;
  border: 1px dashed #d1d5db;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #9ca3af;
}

.image-placeholder {
  width: 100%;
  height: 320px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f3f4f6;
  border: 1px dashed #d1d5db;
  border-radius: 10px;
  color: #6b7280;
  font-size: 14px;
}

.info {
  background: linear-gradient(135deg, #ffffff, #f8fbff);
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  padding: 16px;
  box-shadow: 0 15px 45px rgba(59, 130, 246, 0.1);
}

.info h2 {
  color: #1a1a1a; /* 更深的颜色 */
  font-weight: 600;
}

.price {
  font-size: 28px;
  font-weight: 800;
  color: #ef4444;
  margin: 8px 0;
}

.badge {
  display: inline-block;
  padding: 6px 10px;
  background: #eef2ff;
  color: #4f46e5;
  border-radius: 10px;
  font-weight: 600;
  margin-top: 8px;
}

.small {
  font-size: 13px;
}

/* 库存数字颜色加深 */
.stock-number {
  color: #1a1a1a !important;
  font-weight: 600;
}

.info .muted.small {
  color: #374151;
}

/* 相关秒杀部分的库存颜色 */
.el-timeline .muted.small {
  color: #374151;
}

@media (max-width: 960px) {
  .panel {
    grid-template-columns: 1fr;
  }
}
</style>
