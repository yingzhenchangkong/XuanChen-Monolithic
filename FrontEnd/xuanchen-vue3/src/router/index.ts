import { createRouter, createWebHistory } from 'vue-router';
import { markRaw } from 'vue';
import Layout from '@/layout/Index.vue';

export const routes = [
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/auth/Login.vue'),
    hidden: true,
  },
  {
    // 首次登录/被重置密码后的强制改密页：独立于 Layout，不依赖菜单等业务接口
    path: '/force-change-password',
    name: 'forceChangePassword',
    component: () => import('@/views/auth/ForceChangePassword.vue'),
    hidden: true,
  },
  {
    // 自助注册页：独立于 Layout，无需登录会话（已加入路由守卫白名单）
    path: '/register',
    name: 'register',
    component: () => import('@/views/auth/Register.vue'),
    hidden: true,
  },
  {
    path: '/system/usercenter',
    component: markRaw(Layout),
    children: [
      {
        path: '',
        name: 'userCenter',
        component: () => import('@/views/system/usercenter/Index.vue'),
        meta: {
          title: '用户中心',
        },
        hidden: true,
      },
    ]
  },
]

/**
 * 404 兜底路由：不能随初始路由一起注册。
 * 刷新内页时首跳发生在动态路由注入之前，若 catch-all 先注册，配合守卫中
 * “注入后重导航”链路之外的边界场景会被错误命中；必须在权限守卫 addRoute
 * 完所有动态路由之后再追加，保证具体路由优先于 catch-all 匹配。
 */
export const notFoundRoute = {
  path: '/:pathMatch(.*)*',
  name: '404',
  component: () => import('@/views/error/404.vue'),
  hidden: true,
}

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: routes,
})

export default router
