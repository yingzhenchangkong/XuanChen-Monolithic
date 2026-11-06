import axios from "axios";
import router from "@/router";
import { message } from "ant-design-vue";
import { useAuthStore, useMenuStore, useWebSocketStore } from "@/stores";
import type { LoginResult, Result } from "@/types/api";

// 强制改密专用业务码，需与后端 JwtAuthenticationFilter.PWD_RESET_REQUIRED_CODE 保持一致
const PWD_RESET_REQUIRED_CODE = 4030

//创建一个 axios 实例
const service = axios.create({
  baseURL: import.meta.env.APP_BASE_URL,//所有的请求地址的前缀部分
  timeout: 60000,//请求超时时间(毫秒)
  withCredentials: true,//刷新令牌走 httpOnly Cookie，需携带 Cookie
  // 🔴 不能在这里设默认 Content-Type: application/json——axios 1.x 的 transformRequest
  // 会对"JSON Content-Type + FormData"的请求把 FormData 序列化成 JSON 字符串，
  // 导致后端 @RequestParam 全部绑定失败（MissingServletRequestParameterException → HTTP 500）。
  // 纯对象载荷 axios 会自动补 application/json 头；FormData 则由浏览器自动生成带 boundary 的 multipart。
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

/**
 * 会话过期统一处置：关 WS → 清会话 → 跳登录页 → 清菜单与动态路由（与手动退出登录同一套顺序）。
 * 导出供 WebSocket store 在收到服务端 1008（未认证/会话已被终止）关闭码时复用同一套处置，
 * 内部 isHandling401 去重保证 WS 与并发 HTTP 401 不会重复弹提示/跳转。
 *
 * @param reason 服务端下发的失效原因（如"您已被管理员强制退出"），缺省时使用通用提示
 */
export const handleUnauthorized = (reason?: string) => {
  if (isHandling401) {
    return;
  }
  // 已在登录页（如登录接口自身被匿名拦截）无需再提示与跳转，避免噪音
  if (router.currentRoute.value.path === '/login') {
    return;
  }
  isHandling401 = true;
  message.error(reason || '登录已过期，请重新登录');
  // 必须先关 WebSocket：它在 query 上携带旧 token，会话清空后若不关闭会按退避策略不断重连
  useWebSocketStore().closeConnection();
  // 同步清空响应式会话状态（token/userInfo ref），否则守卫仍会读到内存中的旧凭据
  useAuthStore().clearAuth();
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

/**
 * 网络层错误提示节流：后端重启/不可达时刷新页面会瞬间并发十几个请求，
 * 每个失败都弹一次会在屏幕上叠出一长串相同提示。对相同文案做短时间窗去重，
 * 一次故障只提示一条，故障持续存在时每隔该窗口最多再提示一条
 */
const NETWORK_ERROR_TOAST_INTERVAL_MS = 3000;
let lastNetworkToastAt = 0;
let lastNetworkToastText = '';
const showNetworkToast = (text: string) => {
  const now = Date.now();
  if (text === lastNetworkToastText && now - lastNetworkToastAt < NETWORK_ERROR_TOAST_INTERVAL_MS) {
    return;
  }
  lastNetworkToastText = text;
  lastNetworkToastAt = now;
  message.error(text);
};

/** 刷新令牌进行中标志：并发多个 401 时只发起一次刷新，其余排队等待 */
let isRefreshing = false;
/** 刷新期间排队的请求：刷新成功后用新 token 重试，失败后统一 reject */
let pendingRequests: Array<{
  resolve: (value: any) => void;
  reject: (reason?: any) => void;
  config: any;
}> = [];

/**
 * 尝试用 httpOnly cookie 中的刷新令牌静默换取新访问令牌。
 * 成功则更新本地 token 并重试原请求（及排队请求），失败则走登出流程。
 */
const tryRefreshToken = async (originalError: any): Promise<any> => {
  if (isRefreshing) {
    return new Promise((resolve, reject) => {
      pendingRequests.push({ resolve, reject, config: originalError.config });
    });
  }

  isRefreshing = true;

  try {
    // 响应拦截器已把 axios 响应解包为后端 Result，这里与 httpAction.requestJson 采用同一套类型收口
    const res = (await service.post('/auth/refresh', {}, { _isRefreshRequest: true } as any)) as unknown as Result<LoginResult>;
    if (res?.code === 200) {
      const { token, ...userInfo } = res.data;
      useAuthStore().setToken(token);
      useAuthStore().setUserInfo(userInfo);

      for (const { resolve, config } of pendingRequests) {
        resolve(service(config));
      }
      pendingRequests = [];

      return service(originalError.config);
    } else {
      for (const { reject } of pendingRequests) {
        reject(originalError);
      }
      pendingRequests = [];
      handleUnauthorized();
      return Promise.reject(originalError);
    }
  } catch (refreshError) {
    for (const { reject } of pendingRequests) {
      reject(refreshError);
    }
    pendingRequests = [];
    handleUnauthorized();
    return Promise.reject(refreshError);
  } finally {
    isRefreshing = false;
  }
};

/** 冷启动静默刷新进行中的 Promise：并发导航复用同一次请求，避免重复打 /auth/refresh */
let bootstrapPromise: Promise<boolean> | null = null;

/**
 * 冷启动静默刷新：页面首次加载、本地无访问令牌时，尝试用 httpOnly Cookie 中的刷新令牌
 * 恢复会话——这正是"记住我"要实现的跨浏览器会话免登录。
 *
 * 与 tryRefreshToken 的区别：后者是"已登录但访问令牌过期"的被动续期，本函数是"冷启动
 * 本地无任何凭据"时的主动恢复。二者复用同一 service 实例，并用 _isRefreshRequest 抑制失败
 * 提示：未勾选/已过期的用户本就该静默落到登录页，不应弹出"登录已过期"。
 *
 * 成功写入 token/userInfo 并返回 true；无 Cookie、已过期、账号异常等一律返回 false（不抛错）。
 */
export const bootstrapSessionByRefreshToken = (): Promise<boolean> => {
  if (!bootstrapPromise) {
    bootstrapPromise = (service.post('/auth/refresh', {}, { _isRefreshRequest: true } as any) as unknown as Promise<Result<LoginResult>>)
      .then((res) => {
        if (res?.code === 200 && res.data?.token) {
          const { token, ...userInfo } = res.data;
          useAuthStore().setToken(token);
          useAuthStore().setUserInfo(userInfo);
          return true;
        }
        return false;
      })
      .catch(() => false)
      .finally(() => {
        // 复位以便下次冷启动重新尝试；本次导航仍会拿到已解析的结果
        bootstrapPromise = null;
      });
  }
  return bootstrapPromise;
};

//响应拦截器
service.interceptors.response.use(
  (response) => { //对响应数据做点什么
    return response.data;
  },
  async (error) => { //对响应错误做点什么
    // 刷新令牌请求自身的错误不弹提示，由 tryRefreshToken 统一处理（成功重试/失败登出）
    if (error.config?._isRefreshRequest) {
      return Promise.reject(error);
    }
    // 网络层失败（断网、DNS 失败、后端不可达、连接被拒、超时）时没有 error.response，
    // 直接读 error.response.status 会抛二次异常、掩盖原始错误，必须先分叉处理
    if (!error.response) {
      if (error.code === 'ECONNABORTED' || /timeout/i.test(error.message || '')) {
        showNetworkToast('请求超时，请稍后重试');
      } else {
        // 浏览器 XHR 失败统一为 ERR_NETWORK；Node 等环境会透传 ECONNREFUSED/ECONNRESET 等系统码，
        // 均属传输层失败，控制台保留原始错误便于排查，界面只给统一友好提示
        console.warn('【httpRequest】网络层请求失败:', error.code, error.message);
        showNetworkToast('网络异常，无法连接到服务器，请检查网络或稍后重试');
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
        return tryRefreshToken(error);
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