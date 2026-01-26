import { getAction, postAction } from "@/utils/httpAction";

enum AuthApiUrl {
  LOGIN = '/login',
  LOGOUT = '/logout',
}

export const logout = async () => {
  return await getAction(AuthApiUrl.LOGOUT, {});
}

export const login = async (userName: string, password: string, captcha: string, rememberMe: boolean) => {
  return await postAction(AuthApiUrl.LOGIN, { userName, password, captcha, rememberMe });
}