import { getAction } from "@/utils/httpAction"

enum CacheApiUrl {
  redis = '/monitor/cache/redis',
}

export const getCache = async () => {
  return await getAction(CacheApiUrl.redis, {});
}