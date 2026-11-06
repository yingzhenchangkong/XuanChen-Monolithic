import router, { notFoundRoute } from "./router";
import { postAction } from '@/utils/httpAction';
import { bootstrapSessionByRefreshToken } from '@/utils/httpRequest';
import { useAuthStore, useMenuStore } from "@/stores";
import type { RouteRecordRaw } from "vue-router";
import { isAxiosError } from 'axios';
import { defineComponent, h, markRaw } from 'vue';
import { Button, Result } from 'ant-design-vue';
import Layout from '@/layout/Index.vue';
import type { MenuRecord, Result as ApiResult } from '@/types/api';
const modules = import.meta.glob('./views/**/*.vue');

// 白名单（无需登录会话即可访问）
const whiteList = ['login', 'register', 'forceChangePassword', 'shareView']

// 强制改密专用业务码，需与后端 JwtAuthenticationFilter.PWD_RESET_REQUIRED_CODE 保持一致
const PWD_RESET_REQUIRED_CODE = 4030

/**
 * 菜单加载中的 Promise：并发导航（如登录守卫重入、登录页 preload 与守卫同时发起）
 * 复用同一次请求，避免对 /system/menu/authList 重复打多次、每条都走 addRoute/setMenuList。
 * 每次请求结束后清空，下次守卫检测到菜单仍未加载时会重新发起。
 */
let loadMenusPromise: Promise<void> | null = null;

/**
 * 加载菜单并注入动态路由。返回 Promise，可在守卫或登录流程中调用。
 * 内部做了并发去重：同一时刻多次调用只会发起一次后端请求。
 * 失败时抛出原始错误，调用方（守卫或 Login.vue）按错误类型处置。
 */
export const loadMenus = (): Promise<void> => {
  if (loadMenusPromise) {
    return loadMenusPromise;
  }
  loadMenusPromise = (async () => {
    const authStore = useAuthStore();
    const menuStore = useMenuStore();
    const res = await postAction<MenuRecord[]>('/system/menu/authList');
    if (res.code !== 200) {
      authStore.clearAuth();
      throw new Error(res.msg || '菜单加载失败');
    }
    const dynamicRoutes: RouteRecordRaw = {
      path: '/',
      component: markRaw(Layout),
      name: 'parent',
      children: [
        {
          path: '',
          name: 'home',
          component: () => import('@/views/system/index/Dashboard.vue'),
          meta: {
            title: '首页',
            icon: 'HomeOutlined',
          },
          children: [],
        },
        {
          // 大屏管理页（原「列表」菜单）：已从菜单树摘除，仅由大屏设计画廊
          // 右上角「列表管理」按钮进入，承载发布/停用/复制/删除等列表级管理功能
          path: '/visualization/manage',
          name: 'visualizationlist',
          component: () => import('@/views/visualization/VisualizationList.vue'),
          meta: {
            title: '大屏管理',
          },
          children: [],
        },
      ],
    };
    const dynamicRoutesNew = addDynamicRoutes(res.data ?? []);
    dynamicRoutesNew.forEach((item) => {
      dynamicRoutes.children?.push(item);
    });
    router.addRoute(dynamicRoutes);
    if (!router.hasRoute('404')) {
      router.addRoute(notFoundRoute);
    }
    menuStore.setMenuList(res.data);
    prefetchRouteComponents();
  })().finally(() => {
    loadMenusPromise = null;
  });
  return loadMenusPromise;
};

/** 免登录页（与守卫 whiteList 对应）。此处只能用 path 判断：动态路由此时还没注入，按 name 解析不到目标 */
const PUBLIC_PATHS = ['/login', '/register', '/force-change-password', '/share'];

/** 预注入路由的最长等待时间（ms）：超时即放弃等待、正常挂载，避免后端起不来时首屏被拖死 */
const PREPARE_ROUTE_TIMEOUT = 5000;

/**
 * 在 router 安装之前预热路由表。
 *
 * vue-router 会在 app.use(router) 的那一刻就用当前地址做首次匹配，而本项目的菜单路由是
 * 登录后才注入的动态路由；若此时尚未注入，深链接刷新（如 F5 停在 /system/menu）会先打出
 * [VUE_ROUTER_R0004] No match found —— 功能上靠守卫"注入后重导航"仍能落位，但每次刷新内页
 * 都会刷一条告警，且首屏白屏期间多一次"空匹配 → 重导航"的空转。
 *
 * 这里按会话状态提前把菜单路由注册好，让首次匹配直接命中。任何失败都不在此处置（不弹提示、
 * 不抛错），统一交回路由守卫按原逻辑兜底（4030 → 改密页、401 → 登录页、5xx → 保留会话）。
 * 用超时兜底而不是无限等待：超时后仍在飞的菜单请求会被守卫里的 loadMenus() 复用
 * （内部 Promise 去重），不会重复打接口。
 */
export const prepareRouteTable = async (): Promise<void> => {
  const base = (import.meta.env.BASE_URL || '/').replace(/\/$/, '');
  const { pathname } = window.location;
  const initialPath = base && pathname.startsWith(base) ? pathname.slice(base.length) || '/' : pathname;
  if (PUBLIC_PATHS.some((p) => initialPath === p || initialPath.startsWith(`${p}/`))) {
    return;
  }
  if (!useAuthStore().getToken()) {
    // 冷启动 + "记住我"：先用 httpOnly Cookie 静默恢复会话；失败即视为未登录，交给守卫踢到登录页
    if (!(await bootstrapSessionByRefreshToken())) {
      return;
    }
  }
  if (Array.isArray(useMenuStore().getMenuList())) {
    return;
  }
  let timer: number | undefined;
  try {
    await Promise.race([
      loadMenus(),
      new Promise<void>((resolve) => {
        timer = window.setTimeout(resolve, PREPARE_ROUTE_TIMEOUT);
      }),
    ]);
  } catch {
    // 失败一律由守卫兜底，这里只保证不阻塞挂载
  } finally {
    if (timer !== undefined) {
      window.clearTimeout(timer);
    }
  }
};

// 前置路由守卫
router.beforeEach(async (to) => {
  const authStore = useAuthStore();
  const token = authStore.getToken();
  const menuStore = useMenuStore();
  // 登录响应中的 pwdResetRequired 标志随 userInfo 一起存在 sessionStorage
  const pwdResetRequired = authStore.getUserInfo()?.pwdResetRequired === 1;

  if (token) {
    // 强制改密页对"已登录"会话无条件放行：该页只会调用后端过滤器白名单内的 /changePassword。
    // 必须放在 pwdResetRequired 本地标志判断之前——管理员重置密码后，会话内 userInfo 的标志
    // 仍是登录时的旧值（0），实际是靠后端 4030 兜底引导到本页；若本页再走菜单加载分支，
    // 就会形成 authList(4030) → 重定向本页 → 守卫重入再拉 authList 的死循环（请求不停）
    if (to.name === 'forceChangePassword') {
      return true;
    }
    // 强制改密闭环：标志位用户只能停留在独立改密页，任何其他路由（含登录页）一律重定向过去，
    // 且绝不加载菜单等业务接口，避免被后端 403 拦死
    if (pwdResetRequired) {
      // 能走到这里说明 to 不是改密页（已在上面无条件放行），一律重定向
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
      }
      // 用抽出来的 loadMenus：并发导航（如守卫重入 + Login.vue 预加载）复用同一次请求
      try {
        await loadMenus();
        // 关键：addRoute 不会重新解析"当前这次"导航——to 是在注入前匹配的，
        // 刷新内页时它是空匹配。必须中断本次导航、按原始地址重新解析。
        // 不能复用 to（可能带 name:'404'，会按名字重新落回 404），也不能把
        // fullPath 整个塞进 path（query/hash 会被丢弃），需显式拆开携带；
        // replace 避免历史栈里留下一条注入前的无效记录
        return { path: to.path, query: to.query, hash: to.hash, replace: true };
      } catch (error) {
        // 后端告知"必须先改密"（如会话早于本次标志位建立、userInfo 中无标志的场景）：
        // 先把本地标志补齐（否则后续每轮守卫读到的仍是旧值），再引导到改密页；
        // 改密页已在守卫顶部无条件放行，不会再回头拉菜单
        if (isAxiosError(error) && (error.response?.data as ApiResult | undefined)?.code === PWD_RESET_REQUIRED_CODE) {
          const currentUserInfo = authStore.getUserInfo();
          if (currentUserInfo) {
            authStore.setUserInfo({ ...currentUserInfo, pwdResetRequired: 1 });
          }
          return { name: 'forceChangePassword' };
        }
        // 仅 401（会话失效/未登录）才清会话回登录页。
        // 5xx、网关重启、超时、断网等临时故障不得踢登录：清掉会话会让用户在发布重启/网络抖动后
        // 被迫重新登录；保留会话并中断本次导航，下次导航或刷新会重新拉取菜单
        if (isAxiosError(error) && error.response?.status === 401) {
          authStore.clearAuth();
          return { name: 'login' };
        }
        console.warn('[permission] 菜单加载失败（非鉴权类故障，保留会话）:', error);
        return false;
      }
    }
  } else {
    if (whiteList.includes(to.name as string)) {
      return true;
    }
    // 冷启动恢复会话（"记住我"）：本地无访问令牌时，先尝试用 httpOnly Cookie 中的刷新令牌
    // 静默换取新令牌。勾选过"记住我"的用户关闭浏览器再打开即可免密进入；
    // 未勾选/已过期/账号异常时该请求静默失败，再落到登录页。
    // 注意位置：放在白名单判断之后——登录/注册/改密页不应触发这次恢复请求
    const restored = await bootstrapSessionByRefreshToken();
    if (restored) {
      // 令牌已写入内存，必须中断本次导航并按原始地址重新解析（与菜单注入后的重导航同理）：
      // 不能复用 to（此刻仍是空匹配），需显式拆开携带 query/hash，replace 避免留下无效历史记录
      return { path: to.path, query: to.query, hash: to.hash, replace: true };
    }
    return { name: 'login' };
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

/** 预取只执行一次的标记：登录会话内菜单仅注入一次，重复调用无需重复排队 */
let routeComponentsPrefetched = false;

/**
 * 浏览器空闲时批量预取全部异步页面模块。
 * 必须等菜单注入成功后再调用（模块清单虽在启动期就绪，但预取应避开登录首屏
 * 与菜单请求的网络竞争）；requestIdleCallback 保证不阻塞交互，timeout 兜底
 * 保证空闲事件迟迟不来时（如页面持续繁忙）最迟也会执行。
 *
 * 仅在生产环境执行：dev 下 vite.config 的 server.warmup 已在 dev server 启动时
 * 于服务端预热全部视图模块；若浏览器再空闲时一次性拉起上百个模块的动态 import，
 * 会与用户第一次真实点击的按需编译请求抢队列，反而拖慢首次点击。
 */
function prefetchRouteComponents() {
  if (routeComponentsPrefetched || import.meta.env.DEV) {
    return;
  }
  routeComponentsPrefetched = true;
  const idle = (cb: () => void) => {
    if (typeof window.requestIdleCallback === 'function') {
      window.requestIdleCallback(() => cb(), { timeout: 5000 });
    } else {
      // Safari 等未实现 requestIdleCallback 的环境按短延时兜底
      window.setTimeout(cb, 2000);
    }
  };
  idle(() => {
    void Promise.allSettled(Object.values(modules).map((load) => load()));
  });
}

/** 生成动态路由：后端菜单节点（component 为相对路径字符串）映射为 vue-router 路由 */
export function addDynamicRoutes(routes: MenuRecord[]): RouteRecordRaw[] {  const res: RouteRecordRaw[] = [];
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
