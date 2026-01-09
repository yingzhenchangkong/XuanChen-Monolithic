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
  {
    path: '/:pathMatch(.*)*',
    name: '404',
    component: () => import('@/views/error/404.vue'),
    hidden: true,
  }
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: routes,
})

export default router
