import { getAction, httpAction } from '@/utils/httpAction';
import type { SelectOption } from '@/types/api';
import type { PersonSaveParams } from './person.types';

enum PersonApiUrl {
  INDEX_LIST = '/system/person/list',
  INDEX_DELETE = '/system/person/delete',
  INDEX_DELETE_BATCH = '/system/person/deleteBatch',
  INDEX_EXPORT_EXCEL = '/system/person/exportExcel',
  INDEX_IMPORT_EXCEL = '/system/person/importExcel',

  REC_BIN_LIST = '/system/person/listRecycleBin',
  REC_BIN_DELETE = '/system/person/deleteRecycleBin',
  REC_BIN_DELETE_BATCH = '/system/person/deleteRecycleBinBatch',
  REC_BIN_REVERT = '/system/person/revertRecycleBin',
  REC_BIN_REVERT_BATCH = '/system/person/revertRecycleBinBatch',

  OPERATION_ADD = '/system/person/add',
  OPERATION_EDIT = '/system/person/edit',
  OPERATION_VALIDATE = '/system/person/validate',

  SELECT = '/system/person/select',
}

export { PersonApiUrl };

/** 人员下拉（账号绑定等场景使用） */
export const getPersonSelect = async (): Promise<SelectOption[]> => {
  try {
    const res = await getAction<SelectOption[]>(PersonApiUrl.SELECT);
    if (res.code === 200) {
      return res.data ?? [];
    }
    return [];
  } catch (error) {
    console.error('获取人员数据失败:', error);
    return [];
  }
}

export const validateIdCardApi = async (id: string, idCard: string): Promise<void> => {
  const res = await getAction(PersonApiUrl.OPERATION_VALIDATE, { id, idCard });
  if (res.code !== 200) {
    return Promise.reject("身份证号已存在!");
  } else {
    return Promise.resolve();
  }
}

export const validateMobileApi = async (id: string, mobile: string): Promise<void> => {
  const res = await getAction(PersonApiUrl.OPERATION_VALIDATE, { id, mobile });
  if (res.code !== 200) {
    return Promise.reject("手机号已存在!");
  } else {
    return Promise.resolve();
  }
}

export const validateEmailApi = async (id: string, email: string): Promise<void> => {
  const res = await getAction(PersonApiUrl.OPERATION_VALIDATE, { id, email });
  if (res.code !== 200) {
    return Promise.reject("邮箱已存在!");
  } else {
    return Promise.resolve();
  }
}

export const saveOrUpdate = (data: PersonSaveParams) => {
  const httpUrl = data.id ? PersonApiUrl.OPERATION_EDIT : PersonApiUrl.OPERATION_ADD;
  return httpAction(httpUrl, data, 'post');
};
