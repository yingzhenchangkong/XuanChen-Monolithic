import { defineStore } from 'pinia';
import { useAuthStore } from '@/stores';

/** 心跳间隔：小于 Nginx 默认 proxy_read_timeout(60s)，保证空闲连接不被代理切断 */
const HEARTBEAT_INTERVAL = 25000;
/** 发出 ping 后等待 pong 的最长时间，超时即判定连接已死（半开/TCP 黑洞），主动断开重连 */
const PONG_TIMEOUT = 10000;
/** 重连退避：5s 起指数翻倍，封顶 60s，避免后端宕机时疯狂重连 */
const RECONNECT_BASE_DELAY = 5000;
const RECONNECT_MAX_DELAY = 60000;
/** 应用层心跳报文（浏览器 WebSocket API 无法发送协议级 Ping 帧，只能用文本消息） */
const PING_MESSAGE = 'ping';
const PONG_MESSAGE = 'pong';

/**
 * 连接生命周期诊断日志：仅开发环境输出，不进入生产包；
 * warn/error 属真实异常信号，仍直接走 console，不受此开关限制
 */
const log = (...args: unknown[]) => {
  if (import.meta.env.DEV) {
    console.log(...args);
  }
};

export const useWebSocketStore = defineStore('websocket', () => {
  let ws: WebSocket | null = null;
  /** 唯一的重连定时器：onerror 不再独立调度（规范保证 error 后必跟 close），杜绝双定时器叠加 */
  let reconnectTimer: number | null = null;
  let heartbeatTimer: number | null = null;
  let pongWaitTimer: number | null = null;
  /** 手动关闭（退出登录）：不自动重连；createConnection 被显式调用时复位（重新登录场景） */
  let isManualClose = false;
  let reconnectAttempts = 0;
  /** 最近一次注册的消息处理器，重连后新连接继续沿用，无需调用方反复注册。
   *  推送报文结构由各业务自行收窄，store 层不假定形状，统一为 unknown */
  let messageHandler: ((data: unknown) => void) | null = null;

  // 构造 WebSocket 地址，token 随握手查询参数传递
  // （浏览器 WebSocket API 不支持自定义请求头，后端按 Redis 会话校验该 token）
  const buildWsUrl = () => {
    const token = useAuthStore().getToken();
    if (!token) {
      return null;
    }
    const apiBase = import.meta.env.APP_BASE_URL.replace(/\/+$/, '');
    // APP_BASE_URL 既可能是完整跨域地址（http(s)://host），也可能是同源相对前缀（如 /api，
    // 由 Vite dev server / Nginx 反代）。WebSocket 构造器只接受绝对地址，
    // 相对前缀场景按当前页面协议推导（https→wss、http→ws），避免写出浏览器无法解析的 URL
    let wsBase: string;
    if (/^https?:\/\//i.test(apiBase)) {
      wsBase = apiBase.replace(/^http/i, (m) => (m.toLowerCase() === 'https' ? 'wss' : 'ws'));
    } else {
      const scheme = window.location.protocol === 'https:' ? 'wss' : 'ws';
      wsBase = `${scheme}://${window.location.host}${apiBase}`;
    }
    return `${wsBase}/ws?token=${encodeURIComponent(token)}`;
  };

  const clearReconnectTimer = () => {
    if (reconnectTimer !== null) {
      clearTimeout(reconnectTimer);
      reconnectTimer = null;
    }
  };

  const clearPongWaitTimer = () => {
    if (pongWaitTimer !== null) {
      clearTimeout(pongWaitTimer);
      pongWaitTimer = null;
    }
  };

  const stopHeartbeat = () => {
    if (heartbeatTimer !== null) {
      clearInterval(heartbeatTimer);
      heartbeatTimer = null;
    }
    clearPongWaitTimer();
  };

  /** 心跳看门狗：定时发 ping，限期内收不到 pong 就主动断开，交由 onclose 走唯一重连通道 */
  const startHeartbeat = () => {
    stopHeartbeat();
    heartbeatTimer = window.setInterval(() => {
      if (!ws || ws.readyState !== WebSocket.OPEN) {
        return;
      }
      ws.send(PING_MESSAGE);
      clearPongWaitTimer();
      pongWaitTimer = window.setTimeout(() => {
        console.warn('【WebSocket消息】心跳超时未收到pong，判定连接已死，主动断开重连!');
        try {
          ws?.close();
        } catch (e) {
          // close 本身幂等，异常忽略（onclose 仍会负责清理与重连）
        }
      }, PONG_TIMEOUT) as unknown as number;
    }, HEARTBEAT_INTERVAL);
  };

  const scheduleReconnect = () => {
    clearReconnectTimer();
    const delay = Math.min(
      RECONNECT_BASE_DELAY * 2 ** reconnectAttempts,
      RECONNECT_MAX_DELAY,
    );
    reconnectAttempts += 1;
    log(`【WebSocket消息】${delay / 1000}秒后尝试第${reconnectAttempts}次重连...`);
    reconnectTimer = window.setTimeout(() => {
      reconnectTimer = null;
      createConnection();
    }, delay);
  };

  const createConnection = (onMessage?: (data: unknown) => void) => {
    // 显式发起连接 = 取消“手动关闭”语义。否则退出登录后该标记永久滞留，
    // 重新登录时布局重新挂载再调用本方法将永远不会自动重连
    isManualClose = false;
    if (onMessage) {
      messageHandler = onMessage;
    }

    // 单连接守卫：OPEN 已连接、CONNECTING 正在握手时都直接复用，
    // 杜绝重复调用 new 出多条 WebSocket（消息重复、连接泄漏、多套定时器）
    if (ws) {
      if (ws.readyState === WebSocket.OPEN) {
        log('【WebSocket消息】WebSocket已连接，跳过重复创建!');
        return;
      }
      if (ws.readyState === WebSocket.CONNECTING) {
        log('【WebSocket消息】WebSocket正在连接中，跳过重复创建!');
        return;
      }
      // CLOSING/CLOSED：旧连接交给 onclose 收尾，这里先摘除引用再建新连接
      ws = null;
    }
    clearReconnectTimer();

    const wsUrl = buildWsUrl();
    if (!wsUrl) {
      // 未登录无 token，不发起连接；登录后业务页面会再次触发
      log('【WebSocket消息】未获取到登录token，跳过WebSocket连接!');
      return;
    }

    const socket = new WebSocket(wsUrl);
    ws = socket;

    socket.onopen = () => {
      if (ws !== socket) {
        return; // 已被更新的连接取代，丢弃旧连接的迟到事件
      }
      log('【WebSocket消息】WebSocket连接成功!');
      reconnectAttempts = 0;
      startHeartbeat();
    };

    socket.onmessage = (event) => {
      if (ws !== socket) {
        return;
      }
      const raw: string = typeof event.data === 'string' ? event.data : String(event.data);
      // 心跳应答：刷新看门狗，不投递给业务处理器
      if (raw === PONG_MESSAGE) {
        clearPongWaitTimer();
        return;
      }
      // 服务端推送的是非 JSON（或心跳以外的纯文本/异常报文）时不能让 parse 异常冒泡，
      // 否则回调中断、连接被误判异常
      let payload: unknown;
      try {
        payload = JSON.parse(raw);
      } catch (e) {
        console.warn('【WebSocket消息】收到无法解析的消息，已忽略:', raw, e);
        return;
      }
      if (messageHandler) {
        try {
          messageHandler(payload);
        } catch (e) {
          console.error('【WebSocket消息】业务消息处理异常:', e, payload);
        }
      }
    };

    socket.onerror = (error) => {
      // 只记录，不在这里安排重连：按 WebSocket 规范 error 之后必然触发 close，
      // 统一由 onclose 调度，避免 onerror(长延时)+onclose(短延时)两个定时器叠加
      console.error('【WebSocket消息】WebSocket错误:', error);
    };

    socket.onclose = () => {
      // 旧连接的迟到 close：清理工作已随新连接流程完成，直接忽略，
      // 不能清空当前 ws/停掉新连接心跳或再排一个重连
      if (ws !== socket) {
        return;
      }
      log('【WebSocket消息】WebSocket连接已关闭!');
      ws = null;
      stopHeartbeat();
      if (!isManualClose) {
        scheduleReconnect();
      }
    };
  };

  const closeConnection = () => {
    isManualClose = true;
    clearReconnectTimer();
    stopHeartbeat();
    if (ws) {
      ws.close();
      ws = null;
    }
    log('【WebSocket消息】WebSocket连接已手动关闭!');
  };

  const sendMessage = (data: unknown) => {
    if (ws && ws.readyState === WebSocket.OPEN) {
      ws.send(JSON.stringify(data));
      return true;
    }
    console.warn('【WebSocket消息】连接未就绪，消息未发送');
    return false;
  };

  return {
    createConnection,
    closeConnection,
    sendMessage,
  };
});
