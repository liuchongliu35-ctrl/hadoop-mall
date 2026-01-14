<script setup>
import { onMounted, ref } from 'vue';
import dayjs from 'dayjs';
import { fetchAdminOrders } from '../../api/order';

const orders = ref([]);
const total = ref(0);
const page = ref(1);
const size = ref(10);
const loading = ref(false);
const errorMsg = ref('');

const load = async () => {
  loading.value = true;
  try {
    const res = await fetchAdminOrders({ pageNum: page.value, pageSize: size.value });
    orders.value = res?.records || [];
    total.value = res?.total || 0;
    errorMsg.value = '';
  } catch (e) {
    orders.value = [];
    total.value = 0;
    errorMsg.value = e?.msg || e?.message || '订单接口不可用';
  } finally {
    loading.value = false;
  }
};

onMounted(load);

const formatTime = (t) => (t ? dayjs(t).format('YYYY-MM-DD HH:mm') : '-');

const handlePage = (p) => {
  page.value = p;
  load();
};

const statusMap = {
  0: { label: '待支付', type: 'warning' },
  1: { label: '已支付', type: 'success' },
  2: { label: '已取消', type: 'info' },
};
</script>

<template>
  <div class="page">
    <div class="flex-between" style="margin-bottom: 10px">
      <h3>订单管理</h3>
      <div class="muted">共 {{ total }} 条</div>
    </div>
    <el-alert
      v-if="errorMsg"
      type="warning"
      :title="errorMsg"
      :closable="false"
      show-icon
      style="margin-bottom: 10px"
    />
    <el-table :data="orders" border v-loading="loading">
      <el-table-column prop="orderNo" label="订单号" min-width="160" />
      <el-table-column prop="productName" label="商品" min-width="140" />
      <el-table-column prop="username" label="用户" width="120" />
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
      <el-table-column label="操作" width="120">
        <template #default="scope">
          <el-button size="small" plain @click="$router.push(`/orders/${scope.row.id}`)">
            查看详情
          </el-button>
        </template>
      </el-table-column>
    </el-table>
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
