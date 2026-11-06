import { getAction, postAction, httpAction } from '@/utils/httpAction';
import type { Result, SelectOption, LoginResult } from '@/types/api';
import type { AccountSaveParams } from './account.types';

enum AccountApiUrl {
  INDEX_LIST = '/system/account/list',
  INDEX_DELETE = '/system/account/delete',
  INDEX_DELETE_BATCH = '/system/account/deleteBatch',
  INDEX_EXPORT_EXCEL = '/system/account/exportExcel',
  INDEX_IMPORT_EXCEL = '/system/account/importExcel',
  INDEX_CHANGE_STATUS = '/system/account/changeStatus',

  OPERATION_ADD = '/system/account/add',
  OPERATION_EDIT = '/system/account/edit',
  OPERATION_VALIDATE = '/system/account/validate',

  REC_BIN_LIST = '/system/account/listRecycleBin',
  REC_BIN_DELETE = '/system/account/deleteRecycleBin',
  REC_BIN_DELETE_BATCH = '/system/account/deleteRecycleBinBatch',
  REC_BIN_REVERT = '/system/account/revertRecycleBin',
  REC_BIN_REVERT_BATCH = '/system/account/revertRecycleBinBatch',

  SELECT = '/system/account/select',
  RESET_PASSWORD = '/system/account/resetPassword',

  /** 用户自助改密（账号维度，保留在账号控制器下） */
  CHANGE_PASSWORD = '/system/account/changePassword',

  /** 用户自助：本人资料/头像（维护 sys_account） */
  USER_CENTER_EDIT = '/system/account/userCenterEdit',
  USER_CENTER_UPDATE_AVATAR = '/system/account/userCenterUpdateAvatar',
}

export { AccountApiUrl };

/** 账号下拉（登录日志/操作日志等模块使用） */
export const getUserSelect = async (): Promise<SelectOption[]> => {
  try {
    const res = await getAction<SelectOption[]>(AccountApiUrl.SELECT);
    if (res.code === 200) {
      return res.data ?? [];
    }
    return [];
  } catch (error) {
    console.error('获取账号数据失败:', error);
    return [];
  }
}

/** 用户名唯一性校验（账号侧唯一会查重的字段） */
export const validateUserNameApi = async (id: string, userName: string): Promise<void> => {
  const res = await getAction(AccountApiUrl.OPERATION_VALIDATE, { id, userName });
  if (res.code !== 200) {
    return Promise.reject("用户名已存在!");
  } else {
    return Promise.resolve();
  }
}

/** 手机号唯一性校验（账号维度：登录凭证，全系统唯一） */
export const validateMobileApi = async (id: string, mobile: string): Promise<void> => {
  const res = await getAction(AccountApiUrl.OPERATION_VALIDATE, { id, mobile });
  if (res.code !== 200) {
    return Promise.reject("该手机号已被其他账号使用!");
  } else {
    return Promise.resolve();
  }
}

/** 邮箱唯一性校验（账号维度：找回密码凭证，全系统唯一） */
export const validateEmailApi = async (id: string, email: string): Promise<void> => {
  const res = await getAction(AccountApiUrl.OPERATION_VALIDATE, { id, email });
  if (res.code !== 200) {
    return Promise.reject("该邮箱已被其他账号绑定!");
  } else {
    return Promise.resolve();
  }
}

export const changeStatusApi = (id: string, status: number): Promise<Result<null>> => {
  return postAction<null>(AccountApiUrl.INDEX_CHANGE_STATUS, { id, status });
}

export const saveOrUpdate = (data: AccountSaveParams) => {
  const httpUrl = data.id ? AccountApiUrl.OPERATION_EDIT : AccountApiUrl.OPERATION_ADD;
  const method = data.id ? 'put' : 'post';
  return httpAction(httpUrl, data, method);
};

export const resetPassword = (id: string, password: string): Promise<Result<null>> => {
  return postAction<null>(AccountApiUrl.RESET_PASSWORD, { id, password });
};

/** 用户自助改密 */
export const changePassword = (oldPassword: string, password: string): Promise<Result<LoginResult | null>> => {
  return postAction<LoginResult | null>(AccountApiUrl.CHANGE_PASSWORD, { oldPassword, password });
};

/** 用户中心：保存本人资料（服务端按登录账号定位，维护 sys_account 的昵称/手机号/邮箱） */
export const userCenterEdit = (data: Record<string, unknown>) => {
  return postAction(AccountApiUrl.USER_CENTER_EDIT, data);
};

/** 用户中心：更新本人头像（sys_account.avatar） */
export const userCenterUpdateAvatar = (userName: string, avatar: string) => {
  return httpAction(AccountApiUrl.USER_CENTER_UPDATE_AVATAR, { userName, avatar }, 'put');
};
