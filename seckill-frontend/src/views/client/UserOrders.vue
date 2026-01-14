<script setup>
import { onMounted, ref, computed } from 'vue';
import { fetchUserOrders, cancelOrder, payOrder } from '../../api/order';
import dayjs from 'dayjs';
import { ElMessage, ElMessageBox } from 'element-plus';

const orders = ref([]);
const loading = ref(false);
const total = ref(0);
const page = ref(1);
const size = ref(10);
const errorMsg = ref('');

const load = async () => {
  loading.value = true;
  try {
    const res = await fetchUserOrders({});
    orders.value = res || [];
    total.value = orders.value.length;
    errorMsg.value = '';
  } catch (e) {
    errorMsg.value = e?.msg || e?.message || '订单接口不可用';
  } finally {
    loading.value = false;
  }
};

onMounted(load);

const statusMap = {
  0: { label: '待支付', type: 'warning' },
  1: { label: '已支付', type: 'success' },
  2: { label: '已取消', type: 'info' },
};

const formatTime = (t) => (t ? dayjs(t).format('YYYY-MM-DD HH:mm') : '-');

const pagedOrders = computed(() => {
  const start = (page.value - 1) * size.value;
  return orders.value.slice(start, start + size.value);
});

const handlePage = (p) => {
  page.value = p;
};

const handleCancel = (orderId) => {
  ElMessageBox.confirm('确认取消该订单吗？', '提示', { type: 'warning' })
    .then(async () => {
      await cancelOrder(orderId);
      ElMessage.success('已取消');
      load();
    })
    .catch(() => {});
};

const handlePay = (orderId) => {
  ElMessageBox.confirm('确认支付该订单吗？', '提示', { type: 'warning' })
    .then(async () => {
      await payOrder(orderId);
      ElMessage.success('支付成功');
      load();
    })
    .catch(() => {});
};
</script>

<template>
  <div class="page">
    <h3>我的订单</h3>
    <el-alert
      v-if="errorMsg"
      type="warning"
      :closable="false"
      :title="errorMsg"
      style="margin-bottom: 10px"
    />
    <el-table :data="pagedOrders" border v-loading="loading">
      <el-table-column prop="orderNo" label="订单号" min-width="160" />
      <el-table-column label="商品" min-width="160">
        <template #default="scope">
          <div class="flex-between">
            <span>{{ scope.row.productName }}</span>
            <el-image
              v-if="scope.row.productImage || scope.row.imgUrl"
              :src="scope.row.productImage || scope.row.imgUrl || 'https://images.unsplash.com/photo-1523275335684-37898b6baf30?auto=format&fit=crop&w=1200&q=60'"
              fit="cover"
              style="width: 50px; height: 50px; border-radius: 8px"
            >
              <template #error>
                <div class="image-placeholder-small">
                  <span>加载失败</span>
                </div>
              </template>
            </el-image>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="seckillPrice" label="秒杀价" width="90" />
      <el-table-column prop="quantity" label="数量" width="80" />
      <el-table-column prop="totalAmount" label="总金额" width="100" />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="scope">
          <el-tag :type="statusMap[scope.row.status]?.type">
            {{ statusMap[scope.row.status]?.label || '未知' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="170">
        <template #default="scope">
          {{ formatTime(scope.row.createTime) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="150">
        <template #default="scope">
          <el-button
            v-if="scope.row.status === 0"
            type="danger"
            size="small"
            @click="handleCancel(scope.row.id)"
          >
            取消订单
          </el-button>
          <el-button
            v-if="scope.row.status === 0"
            type="primary"
            plain
            size="small"
            @click="handlePay(scope.row.id)"
            style="margin-left: 6px"
          >
            支付
          </el-button>
          <el-button
            v-else
            size="small"
            plain
            @click="$router.push(`/orders/${scope.row.id}`)"
            style="margin-left: 6px"
          >
            查看
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-empty v-if="!orders.length && !loading" description="暂无订单或接口未开启" />
    <div style="margin-top: 12px; text-align: right">
      <el-pagination
        background
        layout="prev, pager, next"
        :current-page="page"
        :page-size="size"
        :total="total"
        @current-change="handlePage"
      />
    </div>
  </div>
</template>

<style scoped>
.image-placeholder-small {
  width: 50px;
  height: 50px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f3f4f6;
  border: 1px dashed #d1d5db;
  border-radius: 8px;
  color: #6b7280;
  font-size: 10px;
}
</style>
