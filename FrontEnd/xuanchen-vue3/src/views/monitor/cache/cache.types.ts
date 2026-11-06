/** Redis 缓存监控信息 */
export interface CacheInfo {
  // 服务器
  version?: string;
  os?: string;
  archBits?: string | number;
  processId?: string | number;
  // 内存
  usedMemoryHuman?: string;
  usedMemoryPeakHuman?: string;
  usedMemoryLuaHuman?: string;
  // 状态
  connectedClients?: number;
  totalConnectionsReceived?: number;
  totalCommandsProcessed?: number;
  // 信息全集
  mode?: string;
  maxMemory?: string | number;
  maxClients?: number;
  dbSize?: number;
  [key: string]: unknown;
}
