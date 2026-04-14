import { getAction, postAction, httpAction } from '@/utils/httpAction';

enum UserApiUrl {
  INDEX_LIST = '/system/user/list',
  INDEX_DELETE = '/system/user/delete',
  INDEX_DELETE_BATCH = '/system/user/deleteBatch',
  INDEX_EXPORT_EXCEL = '/system/user/exportExcel',
  INDEX_IMPORT_EXCEL = '/system/user/importExcel',
  INDEX_CHANGE_STATUS = '/system/user/changeStatus',

  OPERATION_ADD = '/system/user/add',
  OPERATION_EDIT = '/system/user/edit',
  OPERATION_UPLOAD = import.meta.env.APP_FILE_UPLOAD_PATH,
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

export const getUserSelect = async () => {
  try {
    const res: any = await getAction(UserApiUrl.SELECT, {});
    if (res.code === 200) {
      return res.data;
    }
    return [];
  } catch (error) {
    console.error('获取用户数据失败:', error);
    return [];
  }
}

export const validateUserNameApi = async (id: string, userName: string) => {
  const res: any = await getAction(UserApiUrl.OPERATION_VALIDATE, { id, userName });
  if (res.code === 500) {
    return Promise.reject("用户名已存在!");
  } else {
    return Promise.resolve();
  }
}
export const validateMobileApi = async (id: string, mobile: string) => {
  const res: any = await getAction(UserApiUrl.OPERATION_VALIDATE, { id, mobile });
  if (res.code === 500) {
    return Promise.reject("手机号已存在!");
  } else {
    return Promise.resolve();
  }
}
export const validateEmailApi = async (id: string, email: string) => {
  const res: any = await getAction(UserApiUrl.OPERATION_VALIDATE, { id, email });
  if (res.code === 500) {
    return Promise.reject("邮箱已存在!");
  } else {
    return Promise.resolve();
  }
}

export const changeStatusApi = async (id: string, status: number) => {
  return await postAction(UserApiUrl.INDEX_CHANGE_STATUS, { id, status });
}

export const saveOrUpdate = async (data: any) => {
  const httpUrl = data.id ? UserApiUrl.OPERATION_EDIT : UserApiUrl.OPERATION_ADD;
  const method = data.id ? 'put' : 'post';
  return await httpAction(httpUrl, data, method);
};

export const resetPassword = async (id: string, password: string) => {
  return await postAction(UserApiUrl.RESET_PASSWORD, { id, password });
};

export const userCenterUpdateAvatar = async (userName: string, avatar: string) => {
  return await httpAction(UserApiUrl.USER_CENTER_UPDATE_AVATAR, { userName, avatar }, 'put');
};

export const userCenterEdit = async (data: any) => {
  return await postAction(UserApiUrl.USER_CENTER_EDIT, data);
};

export const changePassword = async (token: string | null, password: string) => {
  return await postAction(UserApiUrl.CHANGE_PASSWORD, { token, password });
};