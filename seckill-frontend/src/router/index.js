import { createRouter, createWebHistory } from 'vue-router';
import { useUserStore } from '../store/user';

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/auth/Login.vue'),
    meta: { public: true, title: '登录' },
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('../views/auth/Register.vue'),
    meta: { public: true, title: '注册' },
  },
  {
    path: '/',
    component: () => import('../views/layout/ClientLayout.vue'),
    children: [
      {
        path: '',
        name: 'Home',
        component: () => import('../views/client/Home.vue'),
        meta: { title: '首页' },
      },
      {
        path: 'products',
        name: 'ProductList',
        component: () => import('../views/client/ProductList.vue'),
        meta: { title: '商品列表' },
      },
      {
        path: 'products/:id',
        name: 'ProductDetail',
        component: () => import('../views/client/ProductDetail.vue'),
        meta: { title: '商品详情' },
      },
      {
        path: 'categories',
        name: 'CategoryList',
        component: () => import('../views/client/CategoryList.vue'),
        meta: { title: '商品分类' },
      },
      {
        path: 'seckill',
        name: 'SeckillList',
        component: () => import('../views/client/SeckillList.vue'),
        meta: { title: '秒杀活动' },
      },
      {
        path: 'seckill/:id',
        name: 'SeckillDetail',
        component: () => import('../views/client/SeckillDetail.vue'),
        meta: { title: '活动详情' },
      },
      {
        path: 'orders',
        name: 'UserOrders',
        component: () => import('../views/client/UserOrders.vue'),
        meta: { title: '我的订单' },
      },
      {
        path: 'orders/:id',
        name: 'OrderDetail',
        component: () => import('../views/client/OrderDetail.vue'),
        meta: { title: '订单详情' },
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('../views/client/Profile.vue'),
        meta: { title: '个人信息' },
      },
      {
        path: 'cart',
        name: 'Cart',
        component: () => import('../views/client/Cart.vue'),
        meta: { title: '我的购物车' }, 
      },
    ],
  },
  {
    path: '/admin',
    component: () => import('../views/layout/AdminLayout.vue'),
    meta: { requiresAdmin: true },
    children: [
      {
        path: '',
        name: 'AdminDashboard',
        component: () => import('../views/admin/AdminDashboard.vue'),
        meta: { title: '控制台', requiresAdmin: true },
      },
      {
        path: 'products',
        name: 'AdminProducts',
        component: () => import('../views/admin/ProductManage.vue'),
        meta: { title: '商品管理', requiresAdmin: true },
      },
      {
        path: 'categories',
        name: 'AdminCategories',
        component: () => import('../views/admin/CategoryManage.vue'),
        meta: { title: '分类管理', requiresAdmin: true },
      },
      {
        path: 'activities',
        name: 'AdminActivities',
        component: () => import('../views/admin/ActivityManage.vue'),
        meta: { title: '秒杀活动管理', requiresAdmin: true },
      },
      {
        path: 'users',
        name: 'AdminUsers',
        component: () => import('../views/admin/UserManage.vue'),
        meta: { title: '用户管理', requiresAdmin: true },
      },
      {
        path: 'orders',
        name: 'AdminOrders',
        component: () => import('../views/admin/OrderManage.vue'),
        meta: { title: '订单管理', requiresAdmin: true },
      },
      {
      path: 'sales',
      name: 'AdminSalesAnalysis',
      component: () => import('../views/admin/SalesAnalysis.vue'),
      meta: { title: '销售分析', requiresAdmin: true },
    },
    ],
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('../views/NotFound.vue'),
    meta: { public: true, title: '页面未找到' },
  },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

router.beforeEach(async (to, from, next) => {
  const userStore = useUserStore();
  if (to.meta.title) {
    document.title = `${to.meta.title} - 秒杀系统`;
  }

  if (to.meta.public) {
    return next();
  }

  if (!userStore.isLoggedIn) {
    return next({ name: 'Login', query: { redirect: to.fullPath } });
  }

  if (to.meta.requiresAdmin && userStore.roleCode !== 1) {
    return next({ name: 'Home' });
  }

  return next();
});

export default router;
