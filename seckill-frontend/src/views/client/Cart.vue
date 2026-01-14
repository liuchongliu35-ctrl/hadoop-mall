<script setup>
import { ref, onMounted, computed } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { fetchMyCart, updateCartItem, removeCartItem, clearCart } from '../../api/cart';
import { Delete, ShoppingCart } from '@element-plus/icons-vue';

const router = useRouter();
const loading = ref(true);
const cart = ref({
  items: [],
  totalPrice: 0,
  totalItems: 0,
});

// 使用 fallback 图片，与你项目其他部分保持一致
const fallbackImages = [
  'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?auto=format&fit=crop&w=400&q=60',
  'https://images.unsplash.com/photo-1523275335684-37898b6baf30?auto=format&fit=crop&w=400&q=60',
];
const pickImage = (key) => fallbackImages[key % fallbackImages.length];

const isCartEmpty = computed(() => !cart.value.items || cart.value.items.length === 0);

// 加载购物车数据
const loadCart = async () => {
  loading.value = true;
  try {
    const data = await fetchMyCart();
    cart.value = data || { items: [], totalPrice: 0, totalItems: 0 };
  } catch (error) {
    console.error('Failed to load cart:', error);
    ElMessage.error('加载购物车失败');
  } finally {
    loading.value = false;
  }
};

// 数量变更处理
const handleQuantityChange = async (item, newQuantity) => {
  // el-input-number 可能会传入 undefined，做个保护
  if (newQuantity === undefined || newQuantity === null) return;
  
  try {
    await updateCartItem({ productId: item.productId, quantity: newQuantity });
    ElMessage.success('数量已更新');
    // 重新加载整个购物车以保证数据同步（特别是总价）
    await loadCart();
  } catch (error) {
    console.error('Failed to update quantity:', error);
    ElMessage.error('更新失败');
    // 失败时也刷新一次，以恢复到服务器上的正确状态
    await loadCart();
  }
};

// 移除单个商品
const handleRemoveItem = (item) => {
  ElMessageBox.confirm(`确定要从购物车中移除「${item.name}」吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(async () => {
    try {
      await removeCartItem(item.productId);
      ElMessage.success('商品已移除');
      await loadCart();
    } catch (error) {
      ElMessage.error('移除失败');
    }
  }).catch(() => {});
};

// 清空购物车
const handleClearCart = () => {
  ElMessageBox.confirm('确定要清空购物车中的所有商品吗？', '清空确认', {
    confirmButtonText: '全部清空',
    cancelButtonText: '取消',
    type: 'danger',
  }).then(async () => {
    try {
      await clearCart();
      ElMessage.success('购物车已清空');
      await loadCart();
    } catch (error) {
      ElMessage.error('操作失败');
    }
  }).catch(() => {});
};

onMounted(loadCart);
</script>

<template>
  <div class="cart-page">
    <h3>我的购物车</h3>

    <el-card shadow="never" class="cart-card" v-loading="loading">
      <div v-if="isCartEmpty && !loading">
        <el-empty description="购物车还是空的">
          <el-button type="primary" @click="router.push('/products')">去逛逛</el-button>
        </el-empty>
      </div>

      <div v-else>
        <!-- 商品列表 -->
        <el-table :data="cart.items" style="width: 100%">
          <el-table-column label="商品信息" min-width="300">
            <template #default="{ row }">
              <div class="product-info">
                <img :src="row.image || pickImage(row.productId)" alt="product image" class="product-image" />
                <span class="product-name">{{ row.name }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="单价" width="150" align="center">
            <template #default="{ row }">￥{{ row.price.toFixed(2) }}</template>
          </el-table-column>
          <el-table-column label="数量" width="180" align="center">
            <template #default="{ row }">
              <el-input-number
                :model-value="row.quantity"
                @change="(val) => handleQuantityChange(row, val)"
                :min="1"
                :max="99" 
                size="small"
                controls-position="right"
              />
            </template>
          </el-table-column>
          <el-table-column label="小计" width="150" align="center">
            <template #default="{ row }">
              <span class="subtotal">￥{{ row.subTotal.toFixed(2) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="100" align="center">
            <template #default="{ row }">
              <el-button type="danger" :icon="Delete" circle plain @click="handleRemoveItem(row)" />
            </template>
          </el-table-column>
        </el-table>

        <!-- 购物车底部操作栏 -->
        <div class="cart-footer">
          <div class="footer-left">
            <el-button type="danger" plain @click="handleClearCart">清空购物车</el-button>
            <span class="muted summary-text">共 {{ cart.totalItems }} 件商品</span>
          </div>
          <div class="footer-right">
            <span>合计：</span>
            <span class="total-price">￥{{ cart.totalPrice.toFixed(2) }}</span>
            <el-button type="primary" :icon="ShoppingCart" style="margin-left: 16px;" @click="ElMessage.info('结算功能开发中...')">
              去结算
            </el-button>
          </div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.cart-page h3 {
  color: #e2e8f0;
  margin-bottom: 16px;
}

.cart-card {
  background-color: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.06);
  color: #e2e8f0;
}

:deep(.el-table),
:deep(.el-table th),
:deep(.el-table tr),
:deep(.el-table__inner-wrapper),
:deep(.el-table__empty-block) {
  background: transparent !important;
  color: #cbd5e1;
}
:deep(.el-table th) {
  color: #e2e8f0;
  font-weight: 600;
}
:deep(.el-table--enable-row-hover .el-table__body tr:hover > td) {
  background-color: rgba(255, 255, 255, 0.05) !important;
}

.product-info {
  display: flex;
  align-items: center;
  gap: 12px;
}
.product-image {
  width: 80px;
  height: 80px;
  object-fit: cover;
  border-radius: 8px;
  background-color: #f3f4f6;
}
.product-name {
  font-weight: 500;
}

.subtotal {
  color: #ef4444;
  font-weight: 600;
}

.cart-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 20px;
  padding: 16px;
  background-color: rgba(255, 255, 255, 0.02);
  border-top: 1px solid rgba(255, 255, 255, 0.08);
}

.footer-left,
.footer-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.summary-text {
  font-size: 14px;
}

.total-price {
  font-size: 24px;
  font-weight: bold;
  color: #ef4444;
}

.muted {
  color: #94a3b8;
}
</style>
