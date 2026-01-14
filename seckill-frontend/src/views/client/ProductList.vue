<script setup>
import { onMounted, ref } from 'vue';
import { fetchProducts } from '../../api/product';

const loading = ref(false);
const fallbackImages = [
  'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?auto=format&fit=crop&w=1200&q=60',
  'https://images.unsplash.com/photo-1498050108023-c5249f4df085?auto=format&fit=crop&w=1200&q=60',
  'https://images.unsplash.com/photo-1523275335684-37898b6baf30?auto=format&fit=crop&w=1200&q=60',
  'https://images.unsplash.com/photo-1512499617640-c2f999098c01?auto=format&fit=crop&w=1200&q=60',
];
const pickImage = (key) => fallbackImages[key % fallbackImages.length];
const products = ref([]);
const total = ref(0);
const page = ref(1);
const size = ref(10);

const load = async () => {
  loading.value = true;
  try {
    const res = await fetchProducts({ pageNum: page.value, pageSize: size.value });
    products.value = res?.records || [];
    total.value = res?.total || 0;
  } finally {
    loading.value = false;
  }
};

onMounted(load);

const handlePage = (p) => {
  page.value = p;
  load();
};
</script>

<template>
  <div class="page">
    <div class="flex-between" style="margin-bottom: 10px">
      <h3>商品列表</h3>
      <div class="muted">共 {{ total }} 件</div>
    </div>
    <el-row :gutter="12" v-loading="loading">
      <el-col v-for="item in products" :key="item.id" :xs="24" :sm="12" :md="6">
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
          <h4>{{ item.productName }}</h4>
          <p class="muted small">{{ item.productDesc }}</p>
          <div class="flex-between">
            <span class="price">￥{{ item.price }}</span>
            <el-tag :type="item.status === 1 ? 'success' : 'info'">
              {{ item.statusDesc || (item.status === 1 ? '上架' : '下架') }}
            </el-tag>
          </div>
          <div class="muted small">库存 {{ item.stock }}</div>
          <el-button type="primary" plain size="small" style="margin-top: 8px" @click="$router.push(`/products/${item.id}`)">
            查看详情
          </el-button>
        </el-card>
      </el-col>
    </el-row>
    <div style="margin-top: 12px; text-align: right">
      <el-pagination
        background
        layout="prev, pager, next"
        :page-size="size"
        :total="total"
        :current-page="page"
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
  height: 180px;
  margin-bottom: 6px;
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
  color: #00d9a6;
  font-weight: 700;
}

.small {
  font-size: 13px;
}
</style>

