import router, { notFoundRoute } from "./router";
import { postAction } from '@/utils/httpAction';
import { useAuthStore, useMenuStore } from "@/stores";
import type { RouteRecordRaw } from "vue-router";
import { isAxiosError } from 'axios';
import { defineComponent, h, markRaw } from 'vue';
import { Button, Result } from 'ant-design-vue';
import Layout from '@/layout/Index.vue';
import type { MenuRecord, Result as ApiResult } from '@/types/api';
const modules = import.meta.glob('./views/**/*.vue');

// 白名单（无需登录会话即可访问）
const whiteList = ['login', 'register', 'forceChangePassword']

// 强制改密专用业务码，需与后端 JwtAuthenticationFilter.PWD_RESET_REQUIRED_CODE 保持一致
const PWD_RESET_REQUIRED_CODE = 4030

// 前置路由守卫
router.beforeEach(async (to) => {
  const authStore = useAuthStore();
  const token = authStore.getToken();
  const menuStore = useMenuStore();
  // 登录响应中的 pwdResetRequired 标志随 userInfo 一起存在 sessionStorage
  const pwdResetRequired = authStore.getUserInfo()?.pwdResetRequired === 1;

  if (token) {
    // 强制改密闭环：标志位用户只能停留在独立改密页，任何其他路由（含登录页）一律重定向过去，
    // 且绝不加载菜单等业务接口，避免被后端 403 拦死
    if (pwdResetRequired) {
      if (to.name === 'forceChangePassword') {
        return true;
      }
      return { name: 'forceChangePassword' };
    }
    if (to.name === 'login') {
      // 动态路由此刻尚未注入，'home' 命名路由还不存在，按 name 重定向会解析失败，
      // 必须用 path 让守卫下一轮在路由注入后重新解析到首页
      return { path: '/' };
    } else {
      // 注意：只能用"是否已加载"判断，不能要求 length>0——零菜单账号（如尚未分配角色）
      // 会被当成"永远没加载"导致守卫反复请求 authList 形成死循环
      const loadedMenus = menuStore.getMenuList();
      if (Array.isArray(loadedMenus)) {
        return true;
      } else {
        try {
          const res = await postAction<MenuRecord[]>('/system/menu/authList');
          if (res.code !== 200) {
            // 业务失败不能"静默放行"：菜单未写入会导致后续每轮导航都重新拉取形成死循环
            authStore.clearAuth();
            sessionStorage.clear();
            return { name: 'login' };
          }
          const dynamicRoutes: RouteRecordRaw = {
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
          const dynamicRoutesNew = addDynamicRoutes(res.data ?? []);
          dynamicRoutesNew.forEach((item) => {
            dynamicRoutes.children?.push(item);
          })
          router.addRoute(dynamicRoutes);
          // 404 兜底必须在动态路由之后注册（刷新内页的首跳发生在注入之前，
          // 此时 to 可能是空匹配/旧 catch-all），保证真实路由优先命中
          if (!router.hasRoute('404')) {
            router.addRoute(notFoundRoute);
          }
          menuStore.setMenuList(res.data);
          // 关键：addRoute 不会重新解析“当前这次”导航——to 是在注入前匹配的，
          // 刷新内页时它是空匹配。必须中断本次导航、按原始地址重新解析。
          // 不能复用 to（可能带 name:'404'，会按名字重新落回 404），也不能把
          // fullPath 整个塞进 path（query/hash 会被丢弃），需显式拆开携带；
          // replace 避免历史栈里留下一条注入前的无效记录
          return { path: to.path, query: to.query, hash: to.hash, replace: true };
        } catch (error) {
          // 后端告知"必须先改密"（如会话早于本次标志位建立、userInfo 中无标志的场景）：
          // 保留会话并引导到改密页
          if (isAxiosError(error) && (error.response?.data as ApiResult | undefined)?.code === PWD_RESET_REQUIRED_CODE) {
            return { name: 'forceChangePassword' };
          }
          // 仅 401（会话失效/未登录）才清会话回登录页。
          // 5xx、网关重启、超时、断网等临时故障不得踢登录：清掉会话会让用户在发布重启/网络抖动后
          // 被迫重新登录；保留会话并中断本次导航，下次导航或刷新会重新拉取菜单
          if (isAxiosError(error) && error.response?.status === 401) {
            authStore.clearAuth();
            sessionStorage.clear();
            return { name: 'login' };
          }
          console.warn('[permission] 菜单加载失败（非鉴权类故障，保留会话）:', error);
          return false;
        }
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

/**
 * 菜单 component 配置在前端找不到对应文件时的占位组件。
 * import.meta.glob 未命中 key 时拿到的是 undefined，若直接注册进路由，
 * vue-router 无组件可渲染会整页白屏且无任何提示，这里给出可见的错误页
 */
const createMissingComponent = (componentPath: string) => markRaw(
  defineComponent({
    name: 'RouteComponentMissing',
    render() {
      return h(
        Result,
        {
          status: 'error',
          title: '页面组件不存在',
          'sub-title': `菜单配置的组件文件 views/${componentPath}.vue 未找到，请联系管理员检查后台菜单 component 配置`,
        },
        {
          extra: () => h(
            Button,
            { type: 'primary', onClick: () => router.push('/') },
            () => '返回首页',
          ),
        },
      );
    },
  }),
);

/** 生成动态路由：后端菜单节点（component 为相对路径字符串）映射为 vue-router 路由 */
export function addDynamicRoutes(routes: MenuRecord[]): RouteRecordRaw[] {
  const res: RouteRecordRaw[] = [];
  routes.forEach((route) => {
    const temp: Record<string, unknown> = { ...route };
    if (route.component != null && route.component !== '') {
      const componentKey = `./views/${route.component}.vue`;
      const matchedComponent = modules[componentKey];
      if (!matchedComponent) {
        console.warn(`[permission] 动态路由组件未命中，已使用占位组件：${componentKey}（菜单 name=${route.name}, path=${route.path}）`);
        temp.component = createMissingComponent(route.component);
      } else {
        temp.component = matchedComponent;
      }
    }
    if (route.children && route.children.length > 0) {
      temp.children = addDynamicRoutes(route.children);
    }
    res.push(temp as unknown as RouteRecordRaw);
  })
  return res;
}
