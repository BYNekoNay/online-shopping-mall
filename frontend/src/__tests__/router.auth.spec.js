import { describe, it, expect, beforeEach } from 'vitest'
import { createRouter, createWebHistory } from 'vue-router'

// 纯 mock 组件，避免 jsdom 中加载 .vue 文件
const MockComp = { template: '<div />' }

const createTestRouter = (overrides = {}) => {
  const routes = [
    {
      path: '/',
      component: { template: '<div>ConsumerLayout</div>' },
      meta: { requiresAuth: false },
      children: [
        { path: '', name: 'Home', component: MockComp },
        { path: 'cart', name: 'Cart', meta: { requiresAuth: true }, component: MockComp },
      ],
    },
    {
      path: '/merchant',
      component: MockComp,
      meta: { requiresAuth: true, roles: [2] },
      children: [
        { path: 'products', name: 'MerchantProducts', component: MockComp },
      ],
    },
    {
      path: '/admin',
      component: MockComp,
      meta: { requiresAuth: true, roles: [3] },
      children: [
        { path: 'users', name: 'AdminUsers', component: MockComp },
      ],
    },
    { path: '/login', name: 'Login', component: MockComp },
    { path: '/403', name: 'Forbidden', component: MockComp },
  ]

  const router = createRouter({
    history: createWebHistory(),
    routes,
  })

  // 复制项目 beforeEach 守卫逻辑
  router.beforeEach((to, _from, next) => {
    const userStore = JSON.parse(localStorage.getItem('user') || '{}')
    let requiresAuth = false
    let allowedRoles = null
    for (const record of to.matched) {
      if (record.meta.requiresAuth) requiresAuth = true
      if (record.meta.roles) allowedRoles = record.meta.roles
    }

    if (requiresAuth && !userStore.token) {
      next({ path: '/login', query: { redirect: to.fullPath } })
    } else if (allowedRoles && !allowedRoles.includes(userStore.role)) {
      next('/403')
    } else {
      next()
    }
  })

  return router
}

describe('Router Auth Guard', () => {
  beforeEach(() => {
    localStorage.clear()
  })

  it('allows public route without token', async () => {
    const router = createTestRouter()
    await router.push('/')
    await router.isReady()
    expect(router.currentRoute.value.path).toBe('/')
  })

  it('redirects to login when accessing auth route without token', async () => {
    const router = createTestRouter()
    await router.push('/cart')
    await router.isReady()
    expect(router.currentRoute.value.path).toBe('/login')
    expect(router.currentRoute.value.query.redirect).toBe('/cart')
  })

  it('allows auth route when token present (consumer role)', async () => {
    localStorage.setItem('user', JSON.stringify({ token: 'abc', role: 1 }))
    const router = createTestRouter()
    await router.push('/cart')
    await router.isReady()
    expect(router.currentRoute.value.path).toBe('/cart')
  })

  it('redirects to 403 when role mismatches merchant route', async () => {
    localStorage.setItem('user', JSON.stringify({ token: 'abc', role: 1 }))
    const router = createTestRouter()
    await router.push('/merchant/products')
    await router.isReady()
    expect(router.currentRoute.value.path).toBe('/403')
  })

  it('allows merchant route for merchant role', async () => {
    localStorage.setItem('user', JSON.stringify({ token: 'abc', role: 2 }))
    const router = createTestRouter()
    await router.push('/merchant/products')
    await router.isReady()
    expect(router.currentRoute.value.path).toBe('/merchant/products')
  })

  it('redirects to 403 when consumer accesses admin route', async () => {
    localStorage.setItem('user', JSON.stringify({ token: 'abc', role: 1 }))
    const router = createTestRouter()
    await router.push('/admin/users')
    await router.isReady()
    expect(router.currentRoute.value.path).toBe('/403')
  })

  it('allows admin route for admin role', async () => {
    localStorage.setItem('user', JSON.stringify({ token: 'abc', role: 3 }))
    const router = createTestRouter()
    await router.push('/admin/users')
    await router.isReady()
    expect(router.currentRoute.value.path).toBe('/admin/users')
  })
})
