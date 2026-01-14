<script setup>
import { onMounted, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  fetchProducts,
  createProduct,
  uploadProductImage,
  updateProduct,
  deleteProduct,
} from '../../api/product';
import { useUserStore } from '../../store/user';

const normalizeImg = (url) => (url ? url.split('?')[0] : '');

const products = ref([]);
const total = ref(0);
const page = ref(1);
const size = ref(10);
const loading = ref(false);
const submitting = ref(false);
const editVisible = ref(false);
const editForm = ref({});

const form = ref({
  productName: '',
  productDesc: '',
  price: null,
  stock: null,
  imgUrl: '',
});

const fallbackImages = [
  'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?auto=format&fit=crop&w=1200&q=60',
  'https://images.unsplash.com/photo-1498050108023-c5249f4df085?auto=format&fit=crop&w=1200&q=60',
  'https://images.unsplash.com/photo-1512499617640-c2f999098c01?auto=format&fit=crop&w=1200&q=60',
];
const pickImage = (key) => fallbackImages[Math.abs(key || 0) % fallbackImages.length];
const getImg = (row) => normalizeImg(row?.imgUrl) || pickImage(row?.id);

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

const handleCreate = async () => {
  submitting.value = true;
  try {
    await createProduct({
      ...form.value,
      imgUrl: normalizeImg(form.value.imgUrl),
    });
    ElMessage.success('创建成功');
    Object.assign(form.value, { productName: '', productDesc: '', price: null, stock: null, imgUrl: '' });
    load();
  } finally {
    submitting.value = false;
  }
};

const handleUpload = async (file, target = 'create') => {
  const userStore = useUserStore();
  if (userStore.roleCode !== 1) {
    ElMessage.error('请用管理员账号登录后再上传');
    return;
  }
  try {
    const res = await uploadProductImage(file.raw);
    const url = typeof res === 'string' ? res : res?.url;
    if (!url) throw new Error('上传接口未返回url');
    if (target === 'edit') {
      editForm.value.imgUrl = normalizeImg(url);
    } else {
      form.value.imgUrl = normalizeImg(url);
    }
    ElMessage.success('上传成功');
  } catch (e) {
    const msg = e?.msg || e?.message || '图片上传失败，请确认已用管理员账号登录且 MinIO 可访问';
    ElMessage.error(msg);
  }
};

const handlePage = (p) => {
  page.value = p;
  load();
};

const openEdit = (row) => {
  editForm.value = { ...row, imgUrl: normalizeImg(row.imgUrl) };
  editVisible.value = true;
};

const submitEdit = async () => {
  try {
    await updateProduct(editForm.value.id, {
      productName: editForm.value.productName,
      productDesc: editForm.value.productDesc,
      price: editForm.value.price,
      stock: editForm.value.stock,
      imgUrl: normalizeImg(editForm.value.imgUrl),
    });
    ElMessage.success('更新成功');
    editVisible.value = false;
    load();
  } catch (e) {
    // 提示由拦截器处理
  }
};

const removeProduct = (row) => {
  ElMessageBox.confirm(`确认删除商品「${row.productName}」吗？`, '提示', { type: 'warning' })
    .then(async () => {
      await deleteProduct(row.id);
      ElMessage.success('已删除');
      load();
    })
    .catch(() => {});
};
</script>

<template>
  <div class="page">
    <div class="flex-between" style="margin-bottom: 10px">
      <h3>商品管理</h3>
      <div class="muted">共 {{ total }} 件</div>
    </div>

    <el-card style="margin-bottom: 12px">
      <h4>新增商品</h4>
      <el-form label-width="90px" :model="form">
        <el-form-item label="名称">
          <el-input v-model="form.productName" placeholder="商品名称" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.productDesc" type="textarea" />
        </el-form-item>
        <el-form-item label="价格">
          <el-input v-model.number="form.price" type="number" />
        </el-form-item>
        <el-form-item label="库存">
          <el-input v-model.number="form.stock" type="number" />
        </el-form-item>
        <el-form-item label="图片">
          <el-input v-model="form.imgUrl" placeholder="上传后自动填充" />
          <el-upload
            :auto-upload="false"
            :show-file-list="false"
            accept="image/*"
            @change="(file) => handleUpload(file, 'create')"
            style="margin-left: 8px"
          >
            <el-button>选择图片并上传</el-button>
          </el-upload>
        </el-form-item>
        <el-button type="primary" :loading="submitting" @click="handleCreate">提交</el-button>
      </el-form>
    </el-card>

    <el-table :data="products" border v-loading="loading">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column label="图片" width="120">
        <template #default="scope">
          <el-image
            :src="getImg(scope.row)"
            fit="cover"
            style="width: 90px; height: 90px; border-radius: 8px"
          >
            <template #error>
              <div class="image-placeholder-admin">
                <span>加载失败</span>
              </div>
            </template>
          </el-image>
        </template>
      </el-table-column>
      <el-table-column prop="productName" label="名称" min-width="160" />
      <el-table-column prop="productDesc" label="描述" min-width="200" show-overflow-tooltip />
      <el-table-column prop="price" label="价格" width="100" />
      <el-table-column prop="stock" label="库存" width="90" />
      <el-table-column label="操作" width="180">
        <template #default="scope">
          <el-button size="small" type="primary" plain @click="openEdit(scope.row)">编辑</el-button>
          <el-button size="small" type="danger" plain @click="removeProduct(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <div style="margin-top: 10px; text-align: right">
      <el-pagination
        background
        layout="prev, pager, next"
        :current-page="page"
        :page-size="size"
        :total="total"
        @current-change="handlePage"
      />
    </div>

    <el-dialog v-model="editVisible" title="编辑商品" width="520px">
      <el-form label-width="90px" :model="editForm">
        <el-form-item label="名称">
          <el-input v-model="editForm.productName" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="editForm.productDesc" type="textarea" />
        </el-form-item>
        <el-form-item label="价格">
          <el-input v-model.number="editForm.price" type="number" />
        </el-form-item>
        <el-form-item label="库存">
          <el-input v-model.number="editForm.stock" type="number" />
        </el-form-item>
        <el-form-item label="图片">
          <el-input v-model="editForm.imgUrl" />
          <el-upload
            :auto-upload="false"
            :show-file-list="false"
            accept="image/*"
            @change="(file) => handleUpload(file, 'edit')"
            style="margin-left: 8px"
          >
            <el-button>上传</el-button>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" @click="submitEdit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.image-placeholder-admin {
  width: 90px;
  height: 90px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f3f4f6;
  border: 1px dashed #d1d5db;
  border-radius: 8px;
  color: #6b7280;
  font-size: 12px;
}
</style>
