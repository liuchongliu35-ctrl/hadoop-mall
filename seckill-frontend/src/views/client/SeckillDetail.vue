<script setup>
import { onMounted, onBeforeUnmount, ref, computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import dayjs from 'dayjs';
import { fetchActivityDetail } from '../../api/activity';
import { fetchProductDetail } from '../../api/product';
import { createSeckillOrder } from '../../api/order';
import { addItemToCart } from '../../api/cart'; // 新增：导入加入购物车的 API
import { useUserStore } from '../../store/user';

const route = useRoute();
const router = useRouter();
const userStore = useUserStore();
const id = route.params.id;

const activity = ref(null);
const product = ref(null);
const loading = ref(false);
const joinLoading = ref(false);
const cartLoading = ref(false); // 新增：加入购物车的加载状态
const quantity = ref(1);
let timer = null;
const joinError = ref('');
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
    const data = await fetchActivityDetail(id);
    activity.value = data;
    if (data?.productId) {
      // 在之前的代码中，product的详情是用另一个接口获取的
      // 我们复用这个逻辑
      product.value = await fetchProductDetail(data.productId);
    }
  } finally {
    loading.value = false;
  }
};

const statusText = computed(() => {
  if (!activity.value) return '';
  const status = activity.value.status;
  if (status === 0) return '未开始';
  if (status === 1) return '进行中';
  return '已结束';
});

const countdown = computed(() => {
  if (!activity.value) return '';
  const now = dayjs();
  if (activity.value.status === 0) {
    const diff = dayjs(activity.value.startTime).diff(now, 'second');
    if (diff <= 0) return '即将开始';
    const h = Math.floor(diff / 3600);
    const m = Math.floor((diff % 3600) / 60);
    const s = diff % 60;
    return `距离开始 ${h}小时${m}分${s}秒`;
  }
  if (activity.value.status === 2) return '已结束';
  const diff = dayjs(activity.value.endTime).diff(now, 'second');
  const h = Math.max(0, Math.floor(diff / 3600));
  const m = Math.max(0, Math.floor((diff % 3600) / 60));
  const s = Math.max(0, diff % 60);
  return `距离结束 ${h}小时${m}分${s}秒`;
});

const canJoin = computed(() => {
  if (!activity.value) return false;
  if (joinError.value) return false;
  return activity.value.status === 1 && (activity.value.remainingStock ?? activity.value.seckillStock) > 0;
});

// 新增：判断是否可以加入购物车
const canAddToCart = computed(() => {
    // 只要商品信息加载出来，并且普通库存大于0，就可以加入购物车
    return product.value && product.value.stock > 0;
});


const handleJoin = async () => {
  if (!activity.value || !product.value) return;
  if (!userStore.isLoggedIn) {
    ElMessageBox.confirm('需要登录后才能下单，是否前往登录？', '提示', { type: 'warning' })
      .then(() => router.push({ name: 'Login', query: { redirect: route.fullPath } }))
      .catch(() => {});
    return;
  }
  joinLoading.value = true;
  try {
    const orderId = await createSeckillOrder({
      activityId: activity.value.id,
      productId: product.value.id,
      quantity: quantity.value,
      // userId: userStore.user?.id, // 后端已通过 @RequestAttribute 获取，前端无需传递
    });
    ElMessage.success('下单成功，订单ID：' + orderId);
    router.push('/orders');
    joinError.value = '';
  } catch (e) {
    const msg = e?.msg || e?.message || '';
    if (msg.includes('seckill_order') || msg.includes('不存在')) {
      joinError.value = '订单服务未就绪（缺少 seckill_order 表），请联系后台';
    } else {
      joinError.value = msg || '下单失败';
    }
    ElMessage.error(joinError.value);
  } finally {
    joinLoading.value = false;
  }
};

// 新增：处理加入购物车的逻辑
const handleAddToCart = async () => {
  if (!product.value) return;
  
  if (!userStore.isLoggedIn) {
    ElMessageBox.confirm('需要登录后才能操作，是否前往登录？', '提示', { type: 'warning' })
      .then(() => router.push({ name: 'Login', query: { redirect: route.fullPath } }))
      .catch(() => {});
    return;
  }
  
  cartLoading.value = true;
  try {
    await addItemToCart({
      productId: product.value.id,
      quantity: quantity.value
    });
    ElMessage.success('已成功加入购物车！');
    router.push('/cart');
  } catch(e) {
    ElMessage.error(e?.message || '加入购物车失败，请稍后再试');
  } finally {
    cartLoading.value = false;
  }
};

onMounted(() => {
  load();
  timer = setInterval(() => {
    // 强制刷新响应式对象，触发倒计时重算
    if (activity.value) {
      activity.value = { ...activity.value };
    }
  }, 1000);
});

onBeforeUnmount(() => {
  if (timer) clearInterval(timer);
});

const formatTime = (t) => (t ? dayjs(t).format('YYYY-MM-DD HH:mm:ss') : '-');
</script>

<template>
  <div class="page" v-loading="loading">
    <el-breadcrumb separator="/">
      <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
      <el-breadcrumb-item :to="{ path: '/seckill' }">秒杀活动</el-breadcrumb-item>
      <el-breadcrumb-item>{{ activity?.activityName || '活动详情' }}</el-breadcrumb-item>
    </el-breadcrumb>

    <div v-if="activity" class="detail">
      <div class="gallery">
        <el-image
          :src="activity.imgUrl || pickImage(activity.id)"
          fit="cover"
        >
          <template #error>
            <div class="image-placeholder">
              <span>图片加载失败</span>
            </div>
          </template>
        </el-image>
      </div>
      <div class="info">
        <div class="flex-between">
          <h2>{{ activity.activityName }}</h2>
          <el-tag :type="activity.status === 1 ? 'success' : activity.status === 0 ? 'info' : 'warning'">
            {{ statusText }}
          </el-tag>
        </div>
        <p class="muted">{{ activity.productDesc }}</p>
        <div class="price-box">
          <div>
            <div class="label">秒杀价</div>
            <div class="seckill">￥{{ activity.seckillPrice }}</div>
          </div>
          <div>
            <div class="label">原价</div>
            <div class="origin">￥{{ activity.originalPrice }}</div>
          </div>
          <div>
            <div class="label">秒杀库存</div>
            <div class="stock">{{ activity.seckillStock }}</div>
          </div>
        </div>
        <div class="muted">开始：{{ formatTime(activity.startTime) }}</div>
        <div class="muted">结束：{{ formatTime(activity.endTime) }}</div>
        <div class="countdown">{{ countdown }}</div>
        <el-alert v-if="joinError" type="warning" :title="joinError" :closable="false" show-icon style="margin: 10px 0" />
        
        <div class="actions">
          <el-input-number v-model="quantity" :min="1" :max="5" size="large" />
          <el-button type="primary" :disabled="!canJoin" :loading="joinLoading" @click="handleJoin" size="large">
            立即秒杀
          </el-button>
          
          <!-- 新增：加入购物车按钮 -->
          <el-button type="warning" plain :disabled="!canAddToCart" :loading="cartLoading" @click="handleAddToCart" size="large">
            加入购物车
          </el-button>
        </div>
        <div class="action-tips" v-if="!canJoin">
            <el-tag type="info" effect="plain">活动未开始或已结束，无法秒杀</el-tag>
        </div>
      </div>
    </div>
    
    <el-divider v-if="product"/>

    <div v-if="product" class="product-info-card">
        <h3>商品详情</h3>
        <p><strong>{{ product.productName }}</strong></p>
        <p class="muted">{{ product.productDesc }}</p>
        <div class="muted">常规售价：￥{{ product.price }} · 常规库存: <span class="stock-number">{{ product.stock }}</span></div>
    </div>
  </div>
</template>

<style scoped>
.detail {
  display: grid;
  grid-template-columns: 1.3fr 2fr;
  gap: 18px;
  margin-top: 12px;
}

.gallery {
  background: #fff;
  padding: 12px;
  border-radius: 12px;
  border: 1px solid #e5e7eb;
  display: flex;
  justify-content: center;
  align-items: center;
}

.gallery :deep(.el-image) {
  width: 100%;
  height: 340px;
  object-fit: cover;
  border-radius: 10px;
}

.placeholder {
  width: 100%;
  height: 340px;
  border: 1px dashed #d1d5db;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #9ca3af;
}

.image-placeholder {
  width: 100%;
  height: 340px;
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
  background: #fff;
  border-radius: 12px;
  padding: 16px;
  border: 1px solid #e5e7eb;
}

.info h2 {
  color: #1a1a1a; /* 更深的颜色 */
  font-weight: 600;
}

.price-box {
  display: flex;
  gap: 16px;
  padding: 12px;
  background: #f9fafb;
  border-radius: 10px;
}

.label {
  color: #6b7280;
  font-size: 13px;
}

.seckill {
  color: #ef4444;
  font-weight: 800;
  font-size: 22px;
}

.origin {
  text-decoration: line-through;
  color: #6b7280;
}

.stock {
  font-weight: 700;
  color: #1a1a1a; /* 更深的颜色 */
}

.countdown {
  margin-top: 10px;
  font-weight: 600;
  color: #2563eb;
}

.actions {
  margin-top: 12px;
  display: flex;
  gap: 10px;
  align-items: center;
}

.product {
  background: #fff;
  padding: 12px;
  border-radius: 12px;
  border: 1px solid #e5e7eb;
}

.product h3 {
  color: #1a1a1a; /* 更深的颜色 */
  font-weight: 600;
}

.product strong {
  color: #1a1a1a; /* 更深的颜色 */
  font-weight: 600;
}

.product .muted {
  color: #374151;
}

.product .stock-number {
  color: #1a1a1a !important;
  font-weight: 600;
}

@media (max-width: 960px) {
  .detail {
    grid-template-columns: 1fr;
  }
}
</style>
