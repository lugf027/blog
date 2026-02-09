import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { ElMessage } from 'element-plus'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    name: 'Home',
    component: () => import('@/views/Home.vue')
  },
  {
    path: '/post/:id',
    name: 'PostDetail',
    component: () => import('@/views/PostDetail.vue')
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue')
  },
  {
    path: '/admin',
    name: 'Admin',
    component: () => import('@/views/Admin.vue'),
    meta: { requiresAuth: true },
    children: [
      {
        path: '',
        name: 'AdminList',
        component: () => import('@/views/admin/PostList.vue'),
        meta: { requiresAuth: true }
      },
      {
        path: 'new',
        name: 'AdminNew',
        component: () => import('@/views/admin/PostEditor.vue'),
        meta: { requiresAuth: true }
      },
      {
        path: 'edit/:id',
        name: 'AdminEdit',
        component: () => import('@/views/admin/PostEditor.vue'),
        meta: { requiresAuth: true }
      },
      {
        path: 'access-logs',
        name: 'AccessLogs',
        component: () => import('@/views/admin/AccessLogs.vue'),
        meta: { requiresAuth: true }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  
  // 如果路由需要认证
  if (to.meta.requiresAuth) {
    if (token) {
      // 有token，允许访问
      next()
    } else {
      // 没有token，跳转到登录页
      ElMessage.warning('请先登录')
      next('/login')
    }
  } else {
    // 不需要认证的路由直接放行
    next()
  }
})

export default router