import router from "./router";
import { postAction } from '@/utils/httpAction';
import { useAuthStore, useMenuStore } from "@/stores";
import type { RouteRecordRaw } from "vue-router";
import { markRaw } from 'vue';
import Layout from '@/layout/Index.vue';
const modules = import.meta.glob('./views/**/*.vue');

// 白名单
const whiteList = ['login']

// 前置路由守卫
router.beforeEach(async (to) => {
  const authStore = useAuthStore();
  const token = authStore.getToken();
  const menuStore = useMenuStore();

  if (token) {
    if (to.name === 'login') {
      return { name: 'home' };
    } else {
      const hasMenu = menuStore.getMenuList() && menuStore.getMenuList().length > 0;
      if (hasMenu) {
        return true;
      } else {
        await postAction('/system/menu/authList', {}).then((res: any) => {
          if (res.code === 200) {
            const dynamicRoutes = {
              path: '/',
              component: markRaw(Layout),
              name: 'parent',
              children: [
                {
                  path: '',
                  name: 'home',
                  component: () => import('@/views/system/index/Index.vue'),
                  meta: {
                    title: '首页',
                    icon: 'HomeOutlined',
                  },
                  children: [],
                },
              ],
            };
            let dynamicRoutesNew = addDynamicRoutes(res.data, router);
            dynamicRoutesNew.forEach((item: any) => {
              dynamicRoutes.children.push(item);
            })
            router.addRoute(dynamicRoutes);
            menuStore.setMenuList(res.data);
            router.push({ name: 'home', replace: true });
          }
        }).catch((error) => {
          sessionStorage.clear();
          return { name: 'login' };
        })
      }
    }
  } else {
    if (whiteList.includes(to.name as string)) {
      return true;
    } else {
      return { name: 'login' };
    }
  }
})
// 后置路由守卫
router.afterEach((to, from) => {

})

/** 生成动态路由 */
export function addDynamicRoutes(routes: RouteRecordRaw[], router: any) {
  const res: Array<RouteRecordRaw> = [];
  routes.forEach((route: any) => {
    const temp = { ...route };
    if (route.component !== null && route.component !== '') {
      temp.component = modules[`./views/${temp.component}.vue`];
    }
    if (temp.children && temp.children.length > 0) {
      temp.children = addDynamicRoutes(temp.children, router);
    }
    res.push(temp);
  })
  return res;
}