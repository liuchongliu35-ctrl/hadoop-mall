<script setup>
import { onMounted, ref } from 'vue';
import { fetchCategories, fetchProducts } from '../../api/product';

const categories = ref([]);
const hotProducts = ref([]);
const loading = ref(false);
const selectedCategory = ref(null);
const fallbackImages = [
  'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?auto=format&fit=crop&w=1200&q=60',
  'https://images.unsplash.com/photo-1498050108023-c5249f4df085?auto=format&fit=crop&w=1200&q=60',
  'https://images.unsplash.com/photo-1523275335684-37898b6baf30?auto=format&fit=crop&w=1200&q=60',
  'https://images.unsplash.com/photo-1512499617640-c2f999098c01?auto=format&fit=crop&w=1200&q=60',
];
const pickImage = (key) => fallbackImages[key % fallbackImages.length];

const load = async (categoryName = '') => {
  loading.value = true;
  const prevProducts = [...hotProducts.value];
  try {
    const [cats, prods] = await Promise.all([
      fetchCategories(),
      fetchProducts({ pageNum: 1, pageSize: 6, productName: categoryName || undefined }),
    ]);
    categories.value = cats || [];
    hotProducts.value = prods?.records || [];
  } catch (e) {
    // 保留之前的数据，避免空白
    hotProducts.value = prevProducts;
  } finally {
    loading.value = false;
  }
};

onMounted(() => load());

const onCategoryClick = (cat) => {
  selectedCategory.value = cat.id;
  load(cat.categoryName);
};
</script>

<template>
  <div class="page" v-loading="loading">
    <div class="flex-between" style="margin-bottom: 12px">
      <h3>商品分类</h3>
      <div class="muted">快速按类查看商品，创建秒杀时可参考</div>
    </div>
    <div class="category-grid">
      <button
        v-for="c in categories"
        :key="c.id"
        class="category-card hoverable"
        :class="{ active: selectedCategory === c.id }"
        @click="onCategoryClick(c)"
      >
        <div class="dot"></div>
        <div>
          <strong>{{ c.categoryName }}</strong>
          <div class="muted small">点击筛选该类商品</div>
        </div>
        <el-tag :type="selectedCategory === c.id ? 'success' : 'info'" size="small">ID: {{ c.id }}</el-tag>
      </button>
    </div>

    <h4 style="margin: 16px 0 8px">热销推荐</h4>
    <el-row :gutter="12">
      <el-col v-for="p in hotProducts" :key="p.id" :xs="24" :sm="12" :md="6">
        <el-card class="card hoverable">
          <div class="image-wrapper">
            <img
              :src="p.imgUrl || pickImage(p.id)"
              class="cover"
              @error="(e) => { e.target.style.display = 'none'; e.target.nextElementSibling.style.display = 'flex'; }"
            />
            <div class="image-placeholder" style="display: none;">
              <span>图片加载失败</span>
            </div>
          </div>
          <div class="flex-between">
            <h4>{{ p.productName }}</h4>
            <el-tag size="small" type="success">￥{{ p.price }}</el-tag>
          </div>
          <p class="muted small">{{ p.productDesc }}</p>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<style scoped>
.category-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 12px;
}

.category-card {
  background: linear-gradient(135deg, rgba(108, 124, 255, 0.12), rgba(0, 217, 166, 0.08));
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 12px;
  padding: 12px;
  display: flex;
  gap: 10px;
  align-items: center;
  box-shadow: 0 10px 30px rgba(59, 130, 246, 0.08);
  cursor: pointer;
}

.dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: linear-gradient(135deg, #06b6d4, #6366f1);
  box-shadow: 0 0 12px rgba(6, 182, 212, 0.5);
}

.image-wrapper {
  position: relative;
  width: 100%;
  height: 150px;
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

.small {
  font-size: 13px;
}

.active {
  border-color: #00d9a6;
  box-shadow: 0 0 0 2px rgba(0, 217, 166, 0.15);
}
</style>
