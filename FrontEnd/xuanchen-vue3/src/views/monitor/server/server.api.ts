import { getAction } from "@/utils/httpAction"
import type { Result } from '@/types/api';

/** 服务器监控-磁盘信息项 */
export interface ServerDiskInfo {
  name?: string;
  total?: string | number;
  free?: string | number;
  used?: string | number;
  usage?: string | number;
  [key: string]: unknown;
}

/** 服务器监控信息 */
export interface ServerInfo {
  // CPU
  cpuNum?: number;
  cpuUsed?: string | number;
  cpuSys?: string | number;
  cpuFree?: string | number;
  // 内存
  totalMem?: string | number;
  usedMem?: string | number;
  freeMem?: string | number;
  memUsage?: string | number;
  // 磁盘
  diskInfos?: ServerDiskInfo[];
  // JVM
  jvmName?: string;
  jvmVersion?: string;
  jvmTotal?: string | number;
  jvmMax?: string | number;
  jvmFree?: string | number;
  jvmUsed?: string | number;
  jvmUsage?: string | number;
  [key: string]: unknown;
}

enum ServerApiUrl {
  redis = '/monitor/server/info',
}

export const getServerInfoApi = (): Promise<Result<ServerInfo>> => {
  return getAction<ServerInfo>(ServerApiUrl.redis, {});
}
