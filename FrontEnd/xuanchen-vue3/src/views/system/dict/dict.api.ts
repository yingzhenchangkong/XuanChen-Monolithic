import { getAction, postAction, deleteAction, httpAction } from '@/utils/httpAction';
import type { SelectOption } from '@/types/api';
import type { DictSaveParams, DictItemSaveParams } from './dict.types';

enum DictApiUrl {
  // 字典主表列表页
  INDEX_LIST = '/system/dict/list',
  INDEX_DELETE = '/system/dict/delete',

  // 字典主表增删改
  OPERATION_ADD = '/system/dict/add',
  OPERATION_EDIT = '/system/dict/edit',
  OPERATION_CHANGE_STATUS = '/system/dict/changeStatus',
  OPERATION_VALIDATE = '/system/dict/validate',

  // 字典项列表（主页面下面的子表格）
  INDEX_ITEM_LIST = '/system/dict/listItem',
  INDEX_ITEM_DELETE = '/system/dict/deleteItem',

  // 字典项增删改
  OPERATION_ITEM_ADD = '/system/dict/addItem',
  OPERATION_ITEM_EDIT = '/system/dict/editItem',
  OPERATION_ITEM_CHANGE_STATUS = '/system/dict/changeStatusItem',
  OPERATION_VALIDATE_ITEM = '/system/dict/validateItem',

  // 对外字典查询接口（不参与 CRUD 分组）
  SELECT = '/system/dict/select',
}

export { DictApiUrl };

export const validateDictCodeApi = async (id: string, dictCode: string): Promise<void> => {
  const res = await getAction(DictApiUrl.OPERATION_VALIDATE, { id, dictCode });
  if (res.code !== 200) {
    return Promise.reject("字典编码已存在!");
  } else {
    return Promise.resolve();
  }
}

export const validateDictNameApi = async (id: string, dictName: string): Promise<void> => {
  const res = await getAction(DictApiUrl.OPERATION_VALIDATE, { id, dictName });
  if (res.code !== 200) {
    return Promise.reject("字典名称已存在!");
  } else {
    return Promise.resolve();
  }
}

export const getDictSelect = async (dictCode: string): Promise<SelectOption[]> => {
  try {
    const res = await getAction<SelectOption[]>(DictApiUrl.SELECT, { dictCode });
    if (res.code === 200) {
      return res.data ?? [];
    }
    return [];
  } catch (error) {
    console.error('获取字典数据失败:', error);
    return [];
  }
}

export const deleteDictApi = async (id: string) => {
  return await deleteAction(DictApiUrl.INDEX_DELETE, { id });
}

export const changeStatusApi = async (id: string, status: number) => {
  return await postAction(DictApiUrl.OPERATION_CHANGE_STATUS, { id, status });
}

export const saveOrUpdateDict = async (data: DictSaveParams) => {
  const httpUrl = data.id ? DictApiUrl.OPERATION_EDIT : DictApiUrl.OPERATION_ADD;
  const method = data.id ? 'put' : 'post';
  return await httpAction(httpUrl, data, method);
};

export const validateDictItemTextApi = async (id: string, dictCode: string, dictItemText: string): Promise<void> => {
  const res = await getAction(DictApiUrl.OPERATION_VALIDATE_ITEM, { id, dictCode, dictItemText });
  if (res.code !== 200) {
    return Promise.reject("名称已存在!");
  } else {
    return Promise.resolve();
  }
}

export const validateDictItemValueApi = async (id: string, dictCode: string, dictItemValue: string): Promise<void> => {
  const res = await getAction(DictApiUrl.OPERATION_VALIDATE_ITEM, { id, dictCode, dictItemValue });
  if (res.code !== 200) {
    return Promise.reject("数据值已存在!");
  } else {
    return Promise.resolve();
  }
}

export const changeStatusItemApi = async (id: string, status: number) => {
  return await postAction(DictApiUrl.OPERATION_ITEM_CHANGE_STATUS, { id, status });
}

export const saveOrUpdateDictItem = async (data: DictItemSaveParams) => {
  const httpUrl = data.id ? DictApiUrl.OPERATION_ITEM_EDIT : DictApiUrl.OPERATION_ITEM_ADD;
  const method = data.id ? 'put' : 'post';
  return await httpAction(httpUrl, data, method);
};
