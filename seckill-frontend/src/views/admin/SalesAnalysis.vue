<script setup>
import { ref, onMounted, computed } from 'vue';
import { fetchTodayDashboard, fetchTodayHotProducts, fetchDailySalesHistory } from '../../api/sales';
import { fetchProducts } from '../../api/product'; // 复用获取商品列表的接口
import { ElMessage } from 'element-plus';
import dayjs from 'dayjs';

// 引入 ECharts
import { use } from 'echarts/core';
import { CanvasRenderer } from 'echarts/renderers';
import { LineChart } from 'echarts/charts';
import { TitleComponent, TooltipComponent, GridComponent, LegendComponent } from 'echarts/components';
import VChart from 'vue-echarts';

use([CanvasRenderer, LineChart, TitleComponent, TooltipComponent, GridComponent, LegendComponent]);

// --- 状态定义 ---
// 加载状态
const dashboardLoading = ref(true);
const hotProductsLoading = ref(true);
const historyLoading = ref(false);

// 数据
const dashboardData = ref({ orderCount: 0, totalAmount: 0 });
const hotProducts = ref([]);
const productList = ref([]); // 用于历史数据筛选

// 历史数据筛选条件
const historyFilters = ref({
  dateRange: [dayjs().subtract(7, 'day').toDate(), dayjs().toDate()], // 默认最近7天
  productId: null,
});

// ECharts 配置
const chartOption = ref({});

// --- API 调用 ---
const loadDashboard = async () => {
  dashboardLoading.value = true;
  try {
    const data = await fetchTodayDashboard();
    dashboardData.value = data;
  } catch (error) {
    console.error('Failed to load dashboard data:', error);
  } finally {
    dashboardLoading.value = false;
  }
};

const loadHotProducts = async () => {
  hotProductsLoading.value = true;
  try {
    const data = await fetchTodayHotProducts({ top: 10 });
    hotProducts.value = data;
  } catch (error) {
    console.error('Failed to load hot products:', error);
  } finally {
    hotProductsLoading.value = false;
  }
};

const loadProductList = async () => {
  try {
    // 获取所有商品用于下拉筛选，这里假设商品总数不多
    const res = await fetchProducts({ pageNum: 1, pageSize: 1000 });
    productList.value = res?.records || [];
  } catch (error) {
    console.error('Failed to load product list:', error);
  }
};

const loadHistoryData = async () => {
  if (!historyFilters.value.dateRange || historyFilters.value.dateRange.length !== 2) {
    ElMessage.warning('请选择日期范围');
    return;
  }
  historyLoading.value = true;
  try {
    const params = {
      startDate: dayjs(historyFilters.value.dateRange[0]).format('YYYY-MM-DD'),
      endDate: dayjs(historyFilters.value.dateRange[1]).format('YYYY-MM-DD'),
      productId: historyFilters.value.productId,
    };
    const data = await fetchDailySalesHistory(params);
    updateChart(data);
  } catch (error) {
    console.error('Failed to load sales history:', error);
  } finally {
    historyLoading.value = false;
  }
};

// --- ECharts 更新逻辑 ---
const updateChart = (data) => {
  const dates = data.map(item => item.date);
  const amounts = data.map(item => item.saleAmount);
  const counts = data.map(item => item.saleCount);

  chartOption.value = {
    backgroundColor: 'transparent', // 适应暗色背景
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'cross',
        crossStyle: {
          color: '#999'
        }
      }
    },
    legend: {
      data: ['销售额', '销量'],
      textStyle: {
        color: '#ccc' // 图例文字颜色
      }
    },
    xAxis: [
      {
        type: 'category',
        data: dates,
        axisPointer: {
          type: 'shadow'
        },
        axisLabel: {
          color: '#ccc' // X轴文字颜色
        }
      }
    ],
    yAxis: [
      {
        type: 'value',
        name: '销售额 (元)',
        min: 0,
        axisLabel: {
          formatter: '￥ {value}',
          color: '#ccc' // Y轴文字颜色
        }
      },
      {
        type: 'value',
        name: '销量 (件)',
        min: 0,
        axisLabel: {
          formatter: '{value} 件',
          color: '#ccc' // Y轴文字颜色
        }
      }
    ],
    series: [
      {
        name: '销售额',
        type: 'line',
        tooltip: {
          valueFormatter: value => `￥ ${value.toFixed(2)}`
        },
        data: amounts,
        smooth: true,
      },
      {
        name: '销量',
        type: 'line',
        yAxisIndex: 1,
        tooltip: {
          valueFormatter: value => `${value} 件`
        },
        data: counts,
        smooth: true,
      }
    ],
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true
    }
  };
};

// --- 生命周期钩子 ---
onMounted(() => {
  loadDashboard();
  loadHotProducts();
  loadProductList();
  loadHistoryData(); // 页面加载时获取默认时间范围的数据
});
</script>

<template>
  <div class="sales-analysis-page">
    <!-- 1. 今日看板 -->
    <el-row :gutter="16">
      <el-col :span="12">
        <el-card shadow="never" v-loading="dashboardLoading" class="stat-card">
          <el-statistic title="今日订单数" :value="dashboardData.orderCount" />
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="never" v-loading="dashboardLoading" class="stat-card">
          <el-statistic title="今日销售额 (元)" :value="dashboardData.totalAmount" :precision="2" prefix="￥" />
        </el-card>
      </el-col>
    </el-row>

    <!-- 2. 今日热门商品 -->
    <el-card shadow="never" class="box-card" style="margin-top: 16px;">
      <template #header>
        <div class="card-header">
          <span>今日热门商品 Top 10</span>
        </div>
      </template>
      <el-table :data="hotProducts" stripe style="width: 100%" v-loading="hotProductsLoading">
        <el-table-column type="index" label="排名" width="80" />
        <el-table-column prop="productName" label="商品名称" />
        <el-table-column prop="quantity" label="销量" width="120" align="right" />
        <el-table-column prop="price" label="单价 (元)" width="120" align="right">
          <template #default="{ row }">
            ￥{{ row.price?.toFixed(2) }}
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 3. 历史销售趋势 -->
    <el-card shadow="never" class="box-card" style="margin-top: 16px;">
      <template #header>
        <div class="card-header">
          <span>历史销售趋势</span>
        </div>
      </template>
      <!-- 筛选区域 -->
      <el-form :inline="true" :model="historyFilters" class="filter-form">
        <el-form-item label="日期范围">
          <el-date-picker
            v-model="historyFilters.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
          />
        </el-form-item>
        <el-form-item label="商品">
          <el-select
            v-model="historyFilters.productId"
            placeholder="全部商品"
            clearable
            filterable
          >
            <el-option
              v-for="item in productList"
              :key="item.id"
              :label="item.productName"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadHistoryData">查询</el-button>
        </el-form-item>
      </el-form>

      <!-- 图表区域 -->
      <div v-loading="historyLoading">
        <v-chart class="chart" :option="chartOption" autoresize />
      </div>
    </el-card>
  </div>
</template>

<style scoped>

.sales-analysis-page :deep(.el-statistic__head) {
  color: #a0aec0;
}

.sales-analysis-page :deep(.el-statistic__content) {
  color: #e2e8f0;
}

.sales-analysis-page :deep(.el-card) {
  background-color: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.06);
  color: #e2e8f0;
}

.sales-analysis-page :deep(.el-card__header) {
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
}

.sales-analysis-page :deep(.el-table),
.sales-analysis-page :deep(.el-table th),
.sales-analysis-page :deep(.el-table tr),
.sales-analysis-page :deep(.elgutter) {
  background-color: transparent;
  color: #e2e8f0;
}
.sales-analysis-page :deep(.el-table tr:hover) {
    background: rgba(255,255,255,0.05) !important;
}

.sales-analysis-page :deep(.el-table--striped .el-table__body tr.el-table__row--striped td.el-table__cell) {
    background: rgba(255,255,255,0.02);
}

.sales-analysis-page :deep(.el-form-item__label) {
  color: #a0aec0;
}

.stat-card {
  text-align: center;
}

.chart {
  height: 400px;
  width: 100%;
}

.sales-analysis-page :deep(.el-statistic__content) {
  color: #3b3a3a;
  font-weight: 600; 
}
</style>
