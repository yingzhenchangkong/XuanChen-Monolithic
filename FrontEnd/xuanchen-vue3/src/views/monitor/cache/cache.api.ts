import { getAction } from "@/utils/httpAction"
import type { Result } from '@/types/api';
import type { CacheInfo } from './cache.types';

enum CacheApiUrl {
  REDIS = '/monitor/cache/redis',
}

export const getCache = (): Promise<Result<CacheInfo>> => {
  return getAction<CacheInfo>(CacheApiUrl.REDIS, {});
};
