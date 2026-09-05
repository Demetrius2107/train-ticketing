import { createRouter, createWebHistory } from 'vue-router'
import store from '@/store'

const routes = [
  {
    path: '/',
    name: 'ticket-query',
    component: () => import('../views/TicketQuery.vue'),
    meta: { title: '车票查询', auth: true }
  },
  {
    path: '/orders',
    name: 'orders',
    component: () => import('../views/Orders.vue'),
    meta: { title: '我的订单', auth: true }
  },
  {
    path: '/login',
    name: 'login',
    component: () => import('../views/login.vue'),
    meta: { title: '登录' }
  }
]

const router = createRouter({
  history: createWebHistory(process.env.BASE_URL),
  routes
})

// 登录守卫：需要登录态的页面未登录时跳登录页
router.beforeEach((to) => {
  if (to.meta.auth && !store.getters.isLogin) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
})

export default router
