<script setup>
import { onMounted, ref } from 'vue';
import { ElMessageBox, ElMessage } from 'element-plus';
import { fetchUsers, updateUserStatus } from '../../api/user';
import dayjs from 'dayjs';

const users = ref([]);
const loading = ref(false);
const total = ref(0);
const page = ref(1);
const size = ref(10);

const load = async () => {
  loading.value = true;
  try {
    const res = await fetchUsers({ pageNum: page.value, pageSize: size.value });
    
    // 修改处：判断 res 是否为数组
    if (Array.isArray(res)) {
      users.value = res;
      // 如果后端没返回总数，暂时只能用当前列表长度，或者设为0
      total.value = res.length; 
    } else {
      // 兼容逻辑：万一后端改回了分页结构
      users.value = res?.records || [];
      total.value = res?.total || 0;
    }
  } catch (error) {
    console.error("加载失败", error);
  } finally {
    loading.value = false;
  }
};

onMounted(load);

const handleStatus = (row, status) => {
  ElMessageBox.confirm(`确认将用户 ${row.username} 设置为 ${status === 1 ? '启用' : '禁用'} 吗？`, '提示', {
    type: 'warning',
  })
    .then(async () => {
      await updateUserStatus(row.id, status);
      ElMessage.success('已更新');
      load();
    })
    .catch(() => {});
};

const formatTime = (t) => (t ? dayjs(t).format('YYYY-MM-DD HH:mm') : '-');
</script>

<template>
  <div class="page">
    <div class="flex-between" style="margin-bottom: 12px">
      <h3>用户管理</h3>
      <div class="muted">共 {{ total }} 位</div>
    </div>
    <el-table :data="users" border v-loading="loading">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="username" label="用户名" min-width="140" />
      <el-table-column prop="email" label="邮箱" min-width="180" />
      <el-table-column prop="phone" label="手机号" min-width="140" />
      <el-table-column prop="role" label="角色" width="100">
        <template #default="scope">
          <el-tag :type="scope.row.role === 1 ? 'success' : 'info'">
            {{ scope.row.role === 1 ? '管理员' : '用户' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="注册时间" width="170">
        <template #default="scope">
          {{ formatTime(scope.row.createTime) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160">
        <template #default="scope">
          <el-button
            size="small"
            type="success"
            plain
            @click="handleStatus(scope.row, 1)"
          >
            启用
          </el-button>
          <el-button
            size="small"
            type="danger"
            plain
            @click="handleStatus(scope.row, 0)"
          >
            禁用
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
        @current-change="(p) => { page.value = p; load(); }"
      />
    </div>
  </div>
</template>
