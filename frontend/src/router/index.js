import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/store/user'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/common/Login.vue')
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/common/Register.vue')
  },
  {
    path: '/403',
    name: 'Forbidden',
    component: () => import('@/views/common/Forbidden.vue')
  },
  {
    path: '/',
    component: () => import('@/layouts/ConsumerLayout.vue'),
    children: [
      { path: '', name: 'Home', component: () => import('@/views/consumer/home/Index.vue') },
      { path: 'product/:id', name: 'ProductDetail', component: () => import('@/views/consumer/product/Detail.vue') },
      {
        path: 'cart',
        name: 'Cart',
        component: () => import('@/views/consumer/cart/Index.vue'),
        meta: { requiresAuth: true }
      },
      { path: 'search', name: 'Search', component: () => import('@/views/consumer/search/Index.vue') },
      { path: 'category', name: 'Category', component: () => import('@/views/consumer/category/Index.vue') },
      {
        path: 'order/confirm',
        name: 'OrderConfirm',
        component: () => import('@/views/consumer/order/Confirm.vue'),
        meta: { requiresAuth: true }
      },
      {
        path: 'order/pay/:id',
        name: 'OrderPay',
        component: () => import('@/views/consumer/order/Pay.vue'),
        meta: { requiresAuth: true }
      },
      {
        path: 'order/batch-pay',
        name: 'OrderBatchPay',
        component: () => import('@/views/consumer/order/BatchPay.vue'),
        meta: { requiresAuth: true }
      },
      {
        path: 'orders',
        name: 'OrderList',
        component: () => import('@/views/consumer/order/List.vue'),
        meta: { requiresAuth: true }
      },
      {
        path: 'orders/:id',
        name: 'OrderDetail',
        component: () => import('@/views/consumer/order/Detail.vue'),
        meta: { requiresAuth: true }
      },
      {
        path: 'user/profile',
        name: 'UserProfile',
        component: () => import('@/views/consumer/user/Profile.vue'),
        meta: { requiresAuth: true }
      },
      {
        path: 'user/addresses',
        name: 'UserAddresses',
        component: () => import('@/views/consumer/user/Addresses.vue'),
        meta: { requiresAuth: true }
      },
      {
        path: 'user/favorites',
        name: 'UserFavorites',
        component: () => import('@/views/consumer/user/Favorites.vue'),
        meta: { requiresAuth: true }
      },
      {
        path: 'user/history',
        name: 'UserHistory',
        component: () => import('@/views/consumer/user/History.vue'),
        meta: { requiresAuth: true }
      },
      {
        path: 'user/coupons',
        name: 'UserCoupons',
        component: () => import('@/views/consumer/user/Coupons.vue'),
        meta: { requiresAuth: true }
      },
      {
        path: 'user/points',
        name: 'UserPoints',
        component: () => import('@/views/consumer/user/Points.vue'),
        meta: { requiresAuth: true }
      },
      {
        path: 'user/points-mall',
        name: 'UserPointsMall',
        component: () => import('@/views/consumer/user/PointsMall.vue'),
        meta: { requiresAuth: true }
      }
    ]
  },
  {
    path: '/merchant',
    redirect: '/merchant/products',
    component: () => import('@/layouts/MerchantLayout.vue'),
    meta: { requiresAuth: true, roles: [2] },
    children: [
      { path: '', redirect: '/merchant/products' },
      // H-17 修复：入驻申请页面向消费者(role=1)，不能被父路由的 roles:[2] 拦截
      {
        path: 'apply',
        name: 'MerchantApply',
        component: () => import('@/views/merchant/apply/Apply.vue'),
        meta: { roles: [1, 2] }
      },
      {
        path: 'apply-pending',
        name: 'MerchantApplyPending',
        component: () => import('@/views/merchant/apply/Pending.vue'),
        meta: { roles: [1, 2] }
      },
      { path: 'shop/info', name: 'MerchantShopInfo', component: () => import('@/views/merchant/shop/Info.vue') },
      {
        path: 'shop/decoration',
        name: 'MerchantDecoration',
        component: () => import('@/views/merchant/shop/Decoration.vue')
      },
      { path: 'shop/freight', name: 'MerchantFreight', component: () => import('@/views/merchant/shop/Freight.vue') },
      { path: 'products', name: 'MerchantProducts', component: () => import('@/views/merchant/product/List.vue') },
      {
        path: 'products/edit',
        name: 'MerchantProductEdit',
        component: () => import('@/views/merchant/product/Edit.vue')
      },
      { path: 'orders', name: 'MerchantOrders', component: () => import('@/views/merchant/order/List.vue') },
      { path: 'refunds', name: 'MerchantRefunds', component: () => import('@/views/merchant/order/Refunds.vue') },
      { path: 'orders/:id', name: 'MerchantOrderDetail', component: () => import('@/views/merchant/order/List.vue') },
      {
        path: 'statistics',
        name: 'MerchantStatistics',
        component: () => import('@/views/merchant/statistics/Dashboard.vue')
      }
    ]
  },
  {
    path: '/admin',
    redirect: '/admin/dashboard',
    component: () => import('@/layouts/AdminLayout.vue'),
    meta: { requiresAuth: true, roles: [3] },
    children: [
      { path: '', redirect: '/admin/dashboard' },
      { path: 'dashboard', name: 'AdminDashboard', component: () => import('@/views/admin/dashboard/Index.vue') },
      { path: 'users', name: 'AdminUsers', component: () => import('@/views/admin/user/List.vue') },
      { path: 'products', name: 'AdminProducts', component: () => import('@/views/admin/product/List.vue') },
      { path: 'shops', name: 'AdminShops', component: () => import('@/views/admin/shop/List.vue') },
      { path: 'categories', name: 'AdminCategories', component: () => import('@/views/admin/product/Categories.vue') },
      { path: 'coupons', name: 'AdminCoupons', component: () => import('@/views/admin/marketing/Coupons.vue') },
      {
        path: 'promotions',
        name: 'AdminPromotions',
        component: () => import('@/views/admin/marketing/Promotions.vue')
      },
      {
        path: 'points-goods',
        name: 'AdminPointsGoods',
        component: () => import('@/views/admin/marketing/PointsGoods.vue')
      },
      {
        path: 'logistics-companies',
        name: 'AdminLogisticsCompanies',
        component: () => import('@/views/admin/logistics/Companies.vue')
      },
      { path: 'logs', name: 'AdminLogs', component: () => import('@/views/admin/system/Logs.vue') },
      { path: 'dicts', name: 'AdminDicts', component: () => import('@/views/admin/system/Dicts.vue') },
      { path: 'config', name: 'AdminConfig', component: () => import('@/views/admin/system/Config.vue') }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, _from, next) => {
  const userStore = useUserStore()
  const requiresAuth = to.matched.some((r) => r.meta.requiresAuth)
  // H-17 修复：roles 取 matched 链中最深层的声明（子路由可覆盖父路由），未声明则不限制角色
  const allowedRoles = to.matched
    .map((r) => r.meta.roles)
    .filter(Boolean)
    .pop()

  if (requiresAuth && !userStore.token) {
    next({ path: '/login', query: { redirect: to.fullPath } })
  } else if (allowedRoles && !allowedRoles.includes(userStore.role)) {
    next('/403')
  } else {
    next()
  }
})

export default router
