import { getAction, postAction } from "@/utils/httpAction";

enum AuthApiUrl {
  LOGIN = '/login',
  LOGOUT = '/logout',
  CAPTCHA_GENERATE = '/captcha/generate',
  CAPTCHA_VERIFY = '/captcha/verify',
}

export const login = async (userName: string, password: string, captchaId: string, captchaToken: string, rememberMe: boolean) => {
  return await postAction(AuthApiUrl.LOGIN, { userName, password, captchaId, captchaToken, rememberMe });
}

export const logout = async () => {
  return await postAction(AuthApiUrl.LOGOUT, {});
}

export const captchaGenerate = async () => {
  return await getAction(AuthApiUrl.CAPTCHA_GENERATE, {});
}

export const captchaVerify = async (captchaId: string, captchaOffset: number) => {
  return await postAction(AuthApiUrl.CAPTCHA_VERIFY, { captchaId, captchaOffset });
}