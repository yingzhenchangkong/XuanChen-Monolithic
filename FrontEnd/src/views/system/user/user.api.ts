import { getAction, httpAction } from '@/utils/httpAction';

enum UserApiUrl {
  SELECT = '/system/user/select',
  ADD = '/system/user/add',
  EDIT = '/system/user/edit',
  UPLOAD = import.meta.env.APP_FILE_UPLOAD_PATH,
  VALIDATE = '/system/user/validate',
  CHANGE_STATUS = '/system/user/changeStatus',

  REC_BIN_LIST = '/system/user/listRecycleBin',
  REC_BIN_DELETE = '/system/user/deleteRecycleBin',
  REC_BIN_DELETE_BATCH = '/system/user/deleteRecycleBinBatch',
  REC_BIN_REVERT = '/system/user/revertRecycleBin',
  REC_BIN_REVERT_BATCH = '/system/user/revertRecycleBinBatch',

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
  const res: any = await getAction(UserApiUrl.VALIDATE, { id, userName });
  if (res.code === 500) {
    return Promise.reject("用户名已存在!");
  } else {
    return Promise.resolve();
  }
}
export const validateMobileApi = async (id: string, mobile: string) => {
  const res: any = await getAction(UserApiUrl.VALIDATE, { id, mobile });
  if (res.code === 500) {
    return Promise.reject("手机号已存在!");
  } else {
    return Promise.resolve();
  }
}
export const validateEmailApi = async (id: string, email: string) => {
  const res: any = await getAction(UserApiUrl.VALIDATE, { id, email });
  if (res.code === 500) {
    return Promise.reject("邮箱已存在!");
  } else {
    return Promise.resolve();
  }
}

export const changeStatusApi = async (id: string, status: number) => {
  
}

export const saveOrUpdate = async (data: any) => {
  const httpUrl = data.id ? UserApiUrl.EDIT : UserApiUrl.ADD;
  const method = data.id ? 'put' : 'post';
  return await httpAction(httpUrl, data, method);
};