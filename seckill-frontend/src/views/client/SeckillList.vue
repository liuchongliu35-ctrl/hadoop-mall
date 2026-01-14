<script setup>
import { onMounted, onBeforeUnmount, ref } from 'vue';
import { useRouter } from 'vue-router';
import { fetchActivities } from '../../api/activity';
import dayjs from 'dayjs';

const router = useRouter();
const activities = ref([]);
const total = ref(0);
const page = ref(1);
const size = ref(9);
const loading = ref(false);
let timer = null;
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
    const res = await fetchActivities({ pageNum: page.value, pageSize: size.value });
    activities.value = res?.records || [];
    total.value = res?.total || 0;
  } finally {
    loading.value = false;
  }
};

const diffText = (start, end, status) => {
  const now = dayjs();
  if (status === 2) return '已结束';
  if (status === 0) {
    const diff = dayjs(start).diff(now, 'second');
    if (diff <= 0) return '即将开始';
    const h = Math.floor(diff / 3600);
    const m = Math.floor((diff % 3600) / 60);
    const s = diff % 60;
    return `距离开始 ${h}小时${m}分${s}秒`;
  }
  const diff = dayjs(end).diff(now, 'second');
  const h = Math.max(0, Math.floor(diff / 3600));
  const m = Math.max(0, Math.floor((diff % 3600) / 60));
  const s = Math.max(0, diff % 60);
  return `距离结束 ${h}小时${m}分${s}秒`;
};

onMounted(() => {
  load();
  timer = setInterval(() => {
    activities.value = [...activities.value];
  }, 1000);
});

onBeforeUnmount(() => {
  if (timer) clearInterval(timer);
});

const handlePage = (p) => {
  page.value = p;
  load();
};
</script>

<template>
  <div class="page">
    <div class="flex-between" style="margin-bottom: 12px">
      <h3>秒杀活动</h3>
      <div class="muted">共 {{ total }} 场</div>
    </div>
    <el-row :gutter="12" v-loading="loading">
      <el-col v-for="item in activities" :key="item.id" :xs="24" :sm="12" :md="8">
        <el-card class="card hoverable">
          <div class="image-wrapper">
            <img
              :src="item.imgUrl || pickImage(item.id)"
              class="cover"
              @error="(e) => { e.target.style.display = 'none'; e.target.nextElementSibling.style.display = 'flex'; }"
            />
            <div class="image-placeholder" style="display: none;">
              <span>图片加载失败</span>
            </div>
          </div>
          <div class="flex-between">
            <div>
              <h4>{{ item.activityName }}</h4>
                <div class="muted small">{{ item.productName }}</div>
              </div>
            <el-tag :type="item.status === 1 ? 'success' : item.status === 0 ? 'info' : 'warning'">
              {{ item.statusDesc || (item.status === 1 ? '进行中' : item.status === 0 ? '未开始' : '已结束') }}
            </el-tag>
          </div>
          <p class="muted small">{{ item.productName }}</p>
          <div class="price">
            <span class="seckill">￥{{ item.seckillPrice }}</span>
            <span class="origin">￥{{ item.originalPrice }}</span>
          </div>
          <div class="muted small">
            {{ diffText(item.startTime, item.endTime, item.status) }} · 库存 {{ item.seckillStock }}
          </div>
          <el-button type="primary" size="small" style="margin-top: 10px" @click="router.push(`/seckill/${item.id}`)">
            查看详情
          </el-button>
        </el-card>
      </el-col>
    </el-row>
    <el-empty v-if="!activities.length && !loading" description="暂无活动，试试在后台创建一条吧" />
    <div style="margin-top: 12px; text-align: right">
      <el-pagination
        background
        layout="prev, pager, next"
        :page-size="size"
        :current-page="page"
        :total="total"
        @current-change="handlePage"
      />
    </div>
  </div>
</template>

<style scoped>
.card {
  margin-bottom: 12px;
}

.image-wrapper {
  position: relative;
  width: 100%;
  height: 160px;
  margin-bottom: 8px;
}

.cover {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 10px;
}

.image-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f3f4f6;
  border: 1px dashed #d1d5db;
  border-radius: 10px;
  color: #6b7280;
  font-size: 14px;
}

.price {
  display: flex;
  align-items: center;
  gap: 10px;
}

.seckill {
  color: #ef4444;
  font-weight: 700;
  font-size: 18px;
}

.origin {
  text-decoration: line-through;
  color: #6b7280;
}

.small {
  font-size: 13px;
}
</style>
