<script setup>
import { onMounted, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { fetchCategories, createCategory } from '../../api/product';

const categories = ref([]);
const loading = ref(false);
const submitting = ref(false);

const form = ref({
  categoryName: '',
  parentId: 0,
});

const load = async () => {
  loading.value = true;
  try {
    categories.value = (await fetchCategories()) || [];
  } finally {
    loading.value = false;
  }
};

onMounted(load);

const handleCreate = async () => {
  submitting.value = true;
  try {
    await createCategory(form.value);
    ElMessage.success('分类已创建');
    Object.assign(form.value, { categoryName: '', parentId: 0 });
    load();
  } finally {
    submitting.value = false;
  }
};
</script>

<template>
  <div class="page" v-loading="loading">
    <div class="flex-between" style="margin-bottom: 12px">
      <h3>分类管理</h3>
      <div class="muted">共 {{ categories.length }} 个</div>
    </div>

    <el-card style="margin-bottom: 12px">
      <h4>新增分类</h4>
      <el-form label-width="90px" :model="form">
        <el-form-item label="分类名">
          <el-input v-model="form.categoryName" placeholder="如：数码产品" />
        </el-form-item>
        <el-form-item label="父ID">
          <el-input v-model.number="form.parentId" type="number" />
        </el-form-item>
        <el-button type="primary" :loading="submitting" @click="handleCreate">提交</el-button>
      </el-form>
    </el-card>

    <el-table :data="categories" border>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="categoryName" label="分类名" min-width="150" />
      <el-table-column prop="parentId" label="父ID" width="100" />
    </el-table>
  </div>
</template>
