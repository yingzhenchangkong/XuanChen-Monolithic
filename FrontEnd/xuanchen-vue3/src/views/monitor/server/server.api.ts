import { getAction } from "@/utils/httpAction"

enum ServerApiUrl {
  redis = '/monitor/server/info',
}

export const getServerInfoApi = async () => {
  return await getAction(ServerApiUrl.redis, {});
}