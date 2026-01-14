<script setup>
import { onMounted, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { fetchActivities, createActivity, updateActivity, updateActivityStatus } from '../../api/activity';
import { fetchProducts } from '../../api/product';
import dayjs from 'dayjs';

const activities = ref([]);
const total = ref(0);
const page = ref(1);
const size = ref(9);
const loading = ref(false);
const submitting = ref(false);
const products = ref([]);
const editVisible = ref(false);
const editForm = ref({});

const form = ref({
  activityName: '',
  productId: null,
  seckillPrice: null,
  seckillStock: null,
  startTime: '',
  endTime: '',
  status: null,
});

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

const loadProducts = async () => {
  const res = await fetchProducts({ pageNum: 1, pageSize: 200 });
  products.value = res?.records || [];
};

onMounted(() => {
  load();
  loadProducts();
});

const handleCreate = async () => {
  submitting.value = true;
  try {
    await createActivity({
      activityName: form.value.activityName,
      productId: form.value.productId,
      seckillPrice: form.value.seckillPrice,
      seckillStock: form.value.seckillStock,
      startTime: form.value.startTime ? dayjs(form.value.startTime).format('YYYY-MM-DDTHH:mm:ss') : '',
      endTime: form.value.endTime ? dayjs(form.value.endTime).format('YYYY-MM-DDTHH:mm:ss') : '',
      status: form.value.status,
    });
    ElMessage.success('创建成功');
    Object.assign(form.value, {
      activityName: '',
      productId: null,
      seckillPrice: null,
      seckillStock: null,
      startTime: '',
      endTime: '',
      status: null,
    });
    load();
  } finally {
    submitting.value = false;
  }
};

const openEdit = (row) => {
  editForm.value = { ...row };
  editVisible.value = true;
};

const submitEdit = async () => {
  try {
    await updateActivity(editForm.value.id, {
      activityName: editForm.value.activityName,
      productId: editForm.value.productId,
      seckillPrice: editForm.value.seckillPrice,
      seckillStock: editForm.value.seckillStock,
      startTime: editForm.value.startTime,
      endTime: editForm.value.endTime,
      status: editForm.value.status,
    });
    ElMessage.success('更新成功');
    editVisible.value = false;
    load();
  } catch (e) {
    // 错误交给拦截器
  }
};

const changeStatus = (row, status) => {
  ElMessageBox.confirm(`确认将活动状态改为 ${status === 1 ? '进行中' : status === 0 ? '未开始' : '已结束'} 吗？`, '提示', {
    type: 'warning',
  })
    .then(async () => {
      await updateActivityStatus(row.id, status);
      ElMessage.success('状态已更新');
      load();
    })
    .catch(() => {});
};

const handlePage = (p) => {
  page.value = p;
  load();
};

const statusTag = (status) => (status === 1 ? 'success' : status === 0 ? 'info' : 'warning');
</script>

<template>
  <div class="page">
    <div class="flex-between" style="margin-bottom: 12px">
      <h3>秒杀活动管理</h3>
      <div class="muted">共 {{ total }} 场</div>
    </div>

    <el-card style="margin-bottom: 12px">
      <h4>创建新活动</h4>
      <el-form label-width="100px" :model="form">
        <el-form-item label="活动名称">
          <el-input v-model="form.activityName" />
        </el-form-item>
        <el-form-item label="关联商品">
          <el-select v-model="form.productId" placeholder="选择商品">
            <el-option v-for="p in products" :key="p.id" :label="p.productName" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="秒杀价">
          <el-input v-model.number="form.seckillPrice" type="number" />
        </el-form-item>
        <el-form-item label="秒杀库存">
          <el-input v-model.number="form.seckillStock" type="number" />
        </el-form-item>
        <el-form-item label="开始时间">
          <el-date-picker
            v-model="form.startTime"
            type="datetime"
            value-format="YYYY-MM-DDTHH:mm:ss"
            placeholder="选择开始时间"
          />
        </el-form-item>
        <el-form-item label="结束时间">
          <el-date-picker
            v-model="form.endTime"
            type="datetime"
            value-format="YYYY-MM-DDTHH:mm:ss"
            placeholder="选择结束时间"
          />
        </el-form-item>
        <el-form-item label="初始状态">
          <el-select v-model="form.status" placeholder="默认未开始">
            <el-option label="未开始" :value="0" />
            <el-option label="进行中" :value="1" />
            <el-option label="已结束" :value="2" />
          </el-select>
        </el-form-item>
        <el-button type="primary" :loading="submitting" @click="handleCreate">提交</el-button>
      </el-form>
    </el-card>

    <el-table :data="activities" border v-loading="loading">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="activityName" label="名称" min-width="150" />
      <el-table-column prop="productName" label="商品" min-width="150" />
      <el-table-column prop="seckillPrice" label="秒杀价" width="100" />
      <el-table-column prop="seckillStock" label="库存" width="90" />
      <el-table-column prop="startTime" label="开始时间" min-width="150" />
      <el-table-column prop="endTime" label="结束时间" min-width="150" />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="scope">
          <el-tag :type="statusTag(scope.row.status)">
            {{ scope.row.statusDesc || (scope.row.status === 1 ? '进行中' : scope.row.status === 0 ? '未开始' : '已结束') }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200">
        <template #default="scope">
          <el-button size="small" type="primary" plain @click="openEdit(scope.row)">编辑</el-button>
          <el-dropdown>
            <el-button size="small" type="success" plain>状态</el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="changeStatus(scope.row,0)">未开始</el-dropdown-item>
                <el-dropdown-item @click="changeStatus(scope.row,1)">进行中</el-dropdown-item>
                <el-dropdown-item @click="changeStatus(scope.row,2)">已结束</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </template>
      </el-table-column>
    </el-table>
    <div style="margin-top: 10px; text-align: right">
      <el-pagination
        background
        layout="prev, pager, next"
        :page-size="size"
        :current-page="page"
        :total="total"
        @current-change="handlePage"
      />
    </div>

    <el-dialog v-model="editVisible" title="编辑活动" width="520px">
      <el-form label-width="90px" :model="editForm">
        <el-form-item label="名称">
          <el-input v-model="editForm.activityName" />
        </el-form-item>
        <el-form-item label="关联商品">
          <el-select v-model="editForm.productId" placeholder="选择商品">
            <el-option v-for="p in products" :key="p.id" :label="p.productName" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="秒杀价">
          <el-input v-model.number="editForm.seckillPrice" type="number" />
        </el-form-item>
        <el-form-item label="秒杀库存">
          <el-input v-model.number="editForm.seckillStock" type="number" />
        </el-form-item>
        <el-form-item label="开始时间">
          <el-date-picker v-model="editForm.startTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" />
        </el-form-item>
        <el-form-item label="结束时间">
          <el-date-picker v-model="editForm.endTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="editForm.status">
            <el-option label="未开始" :value="0" />
            <el-option label="进行中" :value="1" />
            <el-option label="已结束" :value="2" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" @click="submitEdit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>
