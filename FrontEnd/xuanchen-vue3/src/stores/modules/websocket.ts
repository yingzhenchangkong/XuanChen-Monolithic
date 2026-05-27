import { defineStore } from 'pinia';

export const useWebSocketStore = defineStore('websocket', () => {
  let ws: WebSocket | null = null;
  let reconnectTimer: number | null = null;
  let isManualClose = false;  // 标记是否为手动关闭

  const wsUrl = 'ws://localhost:8080/ws';

  const createConnection = (onMessage?: (data: any) => void) => {
    if (ws && ws.readyState === WebSocket.OPEN) {
      console.log('【WebSocket消息】WebSocket已连接!');
      return;
    }

    ws = new WebSocket(wsUrl);

    ws.onopen = () => {
      console.log('【WebSocket消息】WebSocket连接成功!');
      ws?.send(JSON.stringify("【WebSocket消息】前端和后端连接成功了!"));
    };

    ws.onmessage = (event) => {
      console.log('【WebSocket消息】收到WebSocket消息:', event.data);
      if (onMessage) {
        onMessage(JSON.parse(event.data));
      }
    };

    ws.onerror = (error) => {
      console.error('【WebSocket消息】WebSocket错误:', error);
      if (!isManualClose) {
        reconnectTimer = window.setTimeout(() => createConnection(onMessage), 600000);
      }
    };

    ws.onclose = () => {
      console.log('【WebSocket消息】WebSocket连接已关闭!');
      if (!isManualClose) {
        reconnectTimer = window.setTimeout(() => createConnection(onMessage), 5000);
      }
    };
  };

  const closeConnection = () => {
    isManualClose = true;
    if (reconnectTimer) {
      clearTimeout(reconnectTimer);
      reconnectTimer = null;
    }
    if (ws) {
      ws.close();
      ws = null;
    }
    console.log('【WebSocket消息】WebSocket连接已手动关闭!');
  };

  const sendMessage = (data: any) => {
    if (ws && ws.readyState === WebSocket.OPEN) {
      ws.send(JSON.stringify(data));
    }
  };

  return {
    createConnection,
    closeConnection,
    sendMessage,
    isManualClose,
  };
});