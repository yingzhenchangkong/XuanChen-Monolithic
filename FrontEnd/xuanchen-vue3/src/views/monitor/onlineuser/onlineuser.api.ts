import { postAction } from "@/utils/httpAction";
import type { Result } from '@/types/api';

enum OnlineUserApiUrl {
  LIST = '/monitor/onlineUser/list',
  FORCELOGOUT = '/monitor/onlineUser/forceLogout',
}

export { OnlineUserApiUrl };

export const forceLogout = (token: string): Promise<Result<null>> => {
  return postAction<null>(OnlineUserApiUrl.FORCELOGOUT, { token });
};
