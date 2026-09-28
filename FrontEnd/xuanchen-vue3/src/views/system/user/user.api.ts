import { getAction, postAction, httpAction } from '@/utils/httpAction';
import type { Result, SelectOption, LoginResult } from '@/types/api';
import type { UserSaveParams } from './user.types';

enum UserApiUrl {
  INDEX_LIST = '/system/user/list',
  INDEX_DELETE = '/system/user/delete',
  INDEX_DELETE_BATCH = '/system/user/deleteBatch',
  INDEX_EXPORT_EXCEL = '/system/user/exportExcel',
  INDEX_IMPORT_EXCEL = '/system/user/importExcel',
  INDEX_CHANGE_STATUS = '/system/user/changeStatus',

  OPERATION_ADD = '/system/user/add',
  OPERATION_EDIT = '/system/user/edit',
  OPERATION_VALIDATE = '/system/user/validate',

  REC_BIN_LIST = '/system/user/listRecycleBin',
  REC_BIN_DELETE = '/system/user/deleteRecycleBin',
  REC_BIN_DELETE_BATCH = '/system/user/deleteRecycleBinBatch',
  REC_BIN_REVERT = '/system/user/revertRecycleBin',
  REC_BIN_REVERT_BATCH = '/system/user/revertRecycleBinBatch',

  USER_CENTER_UPDATE_AVATAR = '/system/user/userCenterUpdateAvatar',
  USER_CENTER_EDIT = '/system/user/userCenterEdit',

  SELECT = '/system/user/select',
  RESET_PASSWORD = '/system/user/resetPassword',
  CHANGE_PASSWORD = '/system/user/changePassword',
}

export { UserApiUrl };

export const getUserSelect = async (): Promise<SelectOption[]> => {
  try {
    const res = await getAction<SelectOption[]>(UserApiUrl.SELECT);
    if (res.code === 200) {
      return res.data ?? [];
    }
    return [];
  } catch (error) {
    console.error('获取用户数据失败:', error);
    return [];
  }
}

export const validateUserNameApi = async (id: string, userName: string): Promise<void> => {
  const res = await getAction(UserApiUrl.OPERATION_VALIDATE, { id, userName });
  if (res.code !== 200) {
    return Promise.reject("用户名已存在!");
  } else {
    return Promise.resolve();
  }
}
export const validateMobileApi = async (id: string, mobile: string): Promise<void> => {
  const res = await getAction(UserApiUrl.OPERATION_VALIDATE, { id, mobile });
  if (res.code !== 200) {
    return Promise.reject("手机号已存在!");
  } else {
    return Promise.resolve();
  }
}
export const validateEmailApi = async (id: string, email: string): Promise<void> => {
  const res = await getAction(UserApiUrl.OPERATION_VALIDATE, { id, email });
  if (res.code !== 200) {
    return Promise.reject("邮箱已存在!");
  } else {
    return Promise.resolve();
  }
}

export const changeStatusApi = (id: string, status: number): Promise<Result<null>> => {
  return postAction<null>(UserApiUrl.INDEX_CHANGE_STATUS, { id, status });
}

export const saveOrUpdate = (data: UserSaveParams) => {
  const httpUrl = data.id ? UserApiUrl.OPERATION_EDIT : UserApiUrl.OPERATION_ADD;
  const method = data.id ? 'put' : 'post';
  return httpAction(httpUrl, data, method);
};

export const resetPassword = (id: string, password: string): Promise<Result<null>> => {
  return postAction<null>(UserApiUrl.RESET_PASSWORD, { id, password });
};

export const userCenterUpdateAvatar = (userName: string, avatar: string) => {
  return httpAction(UserApiUrl.USER_CENTER_UPDATE_AVATAR, { userName, avatar }, 'put');
};

export const userCenterEdit = (data: UserSaveParams) => {
  return postAction(UserApiUrl.USER_CENTER_EDIT, data);
};

export const changePassword = (oldPassword: string, password: string): Promise<Result<LoginResult | null>> => {
  return postAction<LoginResult | null>(UserApiUrl.CHANGE_PASSWORD, { oldPassword, password });
};
