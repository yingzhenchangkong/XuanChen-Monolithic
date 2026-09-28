import axios from "axios";
import router from "@/router";
import { message } from "ant-design-vue";
import { useAuthStore, useMenuStore, useWebSocketStore } from "@/stores";

// 强制改密专用业务码，需与后端 JwtAuthenticationFilter.PWD_RESET_REQUIRED_CODE 保持一致
const PWD_RESET_REQUIRED_CODE = 4030

//创建一个 axios 实例
const service = axios.create({
  baseURL: import.meta.env.APP_BASE_URL,//所有的请求地址的前缀部分
  timeout: 60000,//请求超时时间(毫秒)
  headers: {//设置后端需要的传参类型
    'Content-Type': 'application/json;charset=UTF-8'
  }
})

//请求拦截器
service.interceptors.request.use(
  (config) => { //在发送请求之前做些什么
    const token = useAuthStore().getToken();
    if (token) {
      config.headers['XC-ACCESS-TOKEN'] = token;
    }
    return config;
  },
  (error) => { //对请求错误做些什么
    return Promise.reject(error)
  }
)

/**
 * 401 登出处理进行中标记：页面初始化常并发多个接口，会话过期时它们会同时收到 401，
 * 不去重会导致重复弹提示、重复跳转与重复清理。首轮处理结束（导航到登录页）后复位
 */
let isHandling401 = false;

/** 会话过期统一处置：关 WS → 清会话 → 跳登录页 → 清菜单与动态路由（与手动退出登录同一套顺序） */
const handleUnauthorized = () => {
  if (isHandling401) {
    return;
  }
  // 已在登录页（如登录接口自身被匿名拦截）无需再提示与跳转，避免噪音
  if (router.currentRoute.value.path === '/login') {
    return;
  }
  isHandling401 = true;
  message.error('登录已过期，请重新登录');
  // 必须先关 WebSocket：它在 query 上携带旧 token，会话清空后若不关闭会按退避策略不断重连
  useWebSocketStore().closeConnection();
  // 同步清空响应式会话状态（token/userInfo ref），否则守卫仍会读到内存中的旧凭据
  useAuthStore().clearAuth();
  window.sessionStorage.clear();
  // 先跳静态登录页，再清理菜单状态与动态路由，避免在激活路由上 removeRoute 触发 No match 错误
  router.push('/login').then(() => {
    useMenuStore().setMenuList(undefined);
    if (router.hasRoute('parent')) {
      router.removeRoute('parent');
    }
  }).catch(() => {
    // 导航被重定向/取消时不影响调用方拿到原始 reject
  }).finally(() => {
    isHandling401 = false;
  });
};

//响应拦截器
service.interceptors.response.use(
  (response) => { //对响应数据做点什么
    return response.data;
  },
  async (error) => { //对响应错误做点什么
    // 网络层失败（断网、DNS 失败、后端不可达、连接被拒、超时）时没有 error.response，
    // 直接读 error.response.status 会抛二次异常、掩盖原始错误，必须先分叉处理
    if (!error.response) {
      if (error.code === 'ECONNABORTED' || /timeout/i.test(error.message || '')) {
        message.error('请求超时，请稍后重试');
      } else {
        // 浏览器 XHR 失败统一为 ERR_NETWORK；Node 等环境会透传 ECONNREFUSED/ECONNRESET 等系统码，
        // 均属传输层失败，控制台保留原始错误便于排查，界面只给统一友好提示
        console.warn('【httpRequest】网络层请求失败:', error.code, error.message);
        message.error('网络异常，无法连接到服务器，请检查网络或稍后重试');
      }
      return Promise.reject(error);
    }

    let { status, data } = error.response;
    // 文件下载（responseType=blob）失败时，后端的 JSON 错误体同样会被包成 Blob，
    // 不反序列化就拿不到后端 msg，界面只能显示笼统的"服务器内部错误/下载失败"
    if (data instanceof Blob && /json/i.test(data.type || '')) {
      try {
        data = JSON.parse(await data.text());
        // 让调用方 catch 到的 error 也带上解析后的业务体，业务层可直接读取 res.code/msg
        error.response.data = data;
      } catch {
        // Blob 内容不是合法 JSON，保持原始 Blob，走状态码兜底提示
      }
    }
    // 优先展示后端返回的业务提示，缺失时再按状态码兜底
    const backendMsg: string | undefined = data?.msg;
    switch (status) {
      case 401:
        handleUnauthorized();
        break;
      case 403:
        if (data?.code === PWD_RESET_REQUIRED_CODE) {
          // 必须先改密：不弹"权限不足"，统一静默引导到独立改密页（token 保留，改密仍需使用）
          if (router.currentRoute.value.name !== 'forceChangePassword') {
            router.push({ name: 'forceChangePassword' });
          }
        } else {
          message.error(backendMsg || '权限不足，禁止访问！');
        }
        break;
      case 404:
        message.error(backendMsg || '请求的资源不存在（404）');
        break;
      case 500:
        message.error(backendMsg || '服务器内部错误（500），请稍后重试');
        break;
      case 502:
      case 503:
      case 504:
        message.error(backendMsg || '服务暂时不可用，请稍后重试');
        break;
      default:
        message.error(backendMsg || `请求失败（${status}）`);
    }
    return Promise.reject(error);
  }
)

export default service;