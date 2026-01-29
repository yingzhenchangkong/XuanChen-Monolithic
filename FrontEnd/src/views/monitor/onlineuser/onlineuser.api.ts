import { postAction } from "@/utils/httpAction";

enum OnlineUserApiUrl {
  LIST = '/monitor/onlineUser/list',
  FORCELOGOUT = '/monitor/onlineUser/forceLogout',
}

export { OnlineUserApiUrl };

export const forceLogout = async (token: string) => {
  return await postAction(OnlineUserApiUrl.FORCELOGOUT, { token });
}