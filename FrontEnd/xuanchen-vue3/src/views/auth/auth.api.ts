import { getAction, postAction } from "@/utils/httpAction";
import type { Result, LoginResult } from "@/types/api";

enum AuthApiUrl {
  LOGIN = '/login',
  LOGOUT = '/logout',
  REGISTER = '/register',
  PWD_SEND_EMAIL_CODE = '/password/sendEmailCode',
  PWD_VERIFY_EMAIL_CODE = '/password/verifyEmailCode',
  PWD_RESET = '/password/reset',
  CAPTCHA_GENERATE = '/captcha/generate',
  CAPTCHA_VERIFY = '/captcha/verify',
}

/** 滑块生成响应 */
export interface CaptchaGenerateData {
  captchaId: string;
  bgImage: string;
  sliderImage: string;
  sliderTop?: number;
  imageWidth?: number;
  imageHeight?: number;
  sliderWidth?: number;
  sliderHeight?: number;
}

/** 滑块校验响应 */
export interface CaptchaVerifyData {
  captchaToken: string;
}

/** 邮箱验证码校验响应（一次性重置令牌） */
export interface VerifyEmailCodeData {
  resetToken: string;
}

export const login = (
  userName: string,
  password: string,
  captchaId: string,
  captchaToken: string,
): Promise<Result<LoginResult>> => {
  return postAction<LoginResult>(AuthApiUrl.LOGIN, { userName, password, captchaId, captchaToken });
}

export const logout = (): Promise<Result<null>> => {
  return postAction<null>(AuthApiUrl.LOGOUT, {});
}

/**
 * 自助注册（公开接口，后端做滑块校验、IP 限流、用户名/邮箱查重）
 */
export const register = (data: {
  userName: string;
  nickName?: string;
  email: string;
  password: string;
  captchaId: string;
  captchaToken: string;
}): Promise<Result<null>> => {
  return postAction<null>(AuthApiUrl.REGISTER, data);
}

/**
 * 找回密码第 1 步：发送邮箱验证码（强制滑块验证，账号是否存在返回文案一致，防枚举）
 */
export const sendPasswordEmailCode = (data: {
  userName: string;
  captchaId: string;
  captchaToken: string;
}): Promise<Result<null>> => {
  return postAction<null>(AuthApiUrl.PWD_SEND_EMAIL_CODE, data);
}

/**
 * 找回密码第 2 步：校验邮箱验证码，换取一次性重置令牌 resetToken
 */
export const verifyPasswordEmailCode = (data: {
  userName: string;
  emailCode: string;
}): Promise<Result<VerifyEmailCodeData>> => {
  return postAction<VerifyEmailCodeData>(AuthApiUrl.PWD_VERIFY_EMAIL_CODE, data);
}

/**
 * 找回密码第 3 步：凭一次性重置令牌设置新密码（成功后后端踢掉该账号全部旧会话）
 */
export const resetPasswordByEmail = (data: {
  resetToken: string;
  password: string;
}): Promise<Result<null>> => {
  return postAction<null>(AuthApiUrl.PWD_RESET, data);
}

export const captchaGenerate = (): Promise<Result<CaptchaGenerateData>> => {
  return getAction<CaptchaGenerateData>(AuthApiUrl.CAPTCHA_GENERATE, {});
}

export const captchaVerify = (
  captchaId: string,
  captchaOffset: number,
): Promise<Result<CaptchaVerifyData>> => {
  return postAction<CaptchaVerifyData>(AuthApiUrl.CAPTCHA_VERIFY, { captchaId, captchaOffset });
}
