import { postAction } from "@/utils/httpAction";
import type { Result } from '@/types/api';

enum OnlineUserApiUrl {
  INDEX_LIST = '/monitor/onlineUser/list',
  OPERATION_FORCELOGOUT = '/monitor/onlineUser/forceLogout',
}

export { OnlineUserApiUrl };

export const forceLogout = (token: string): Promise<Result<null>> => {
  return postAction<null>(OnlineUserApiUrl.OPERATION_FORCELOGOUT, { token });
};
