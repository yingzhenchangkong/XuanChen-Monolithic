import { getAction } from "@/utils/httpAction"
import type { Result } from '@/types/api';
import type { ServerInfo } from './server.types';

enum ServerApiUrl {
  REDIS = '/monitor/server/info',
}

export const getServerInfoApi = (): Promise<Result<ServerInfo>> => {
  return getAction<ServerInfo>(ServerApiUrl.REDIS, {});
}
