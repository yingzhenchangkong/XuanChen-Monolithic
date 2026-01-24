import { getAction, postAction, deleteAction, httpAction } from '@/utils/httpAction';
import type { DictModel, DictItemModel } from './dict.types';

enum DictApiUrl {
  DICT_LIST = '/system/dict/list',
  DICT_DELETE = '/system/dict/delete',
  DICT_ADD = '/system/dict/add',
  DICT_EDIT = '/system/dict/edit',
  DICT_CHANGE_STATUS = '/system/dict/changeStatus',
  VALIDATE = '/system/dict/validate',
  SELECT = '/system/dict/select',

  DICT_ITEM_LIST = '/system/dict/listItem',
  DICT_ITEM_DELETE = '/system/dict/deleteItem',
  DICT_ITEM_ADD = '/system/dict/addItem',
  DICT_ITEM_EDIT = '/system/dict/editItem',
  DICT_ITEM_CHANGE_STATUS = '/system/dict/changeStatusItem',
  VALIDATE_ITEM = '/system/dict/validateItem',
}

export { DictApiUrl };

export const validateDictCodeApi = async (id: string, dictCode: string) => {
  const res: any = await getAction(DictApiUrl.VALIDATE, { id, dictCode });
  if (res.code === 500) {
    return Promise.reject("字典编码已存在!");
  } else {
    return Promise.resolve();
  }
}

export const validateDictNameApi = async (id: string, dictName: string) => {
  const res: any = await getAction(DictApiUrl.VALIDATE, { id, dictName });
  if (res.code === 500) {
    return Promise.reject("字典名称已存在!");
  } else {
    return Promise.resolve();
  }
}

export const getDictSelect = async (dictCode: string) => {
  try {
    const res: any = await getAction(DictApiUrl.SELECT, { dictCode });
    if (res.code === 200) {
      return res.data;
    }
    return [];
  } catch (error) {
    console.error('获取字典数据失败:', error);
    return [];
  }
}

export const deleteDictApi = async (id: string) => {
  return await deleteAction(DictApiUrl.DICT_DELETE, { id });
}

export const changeStatusApi = async (id: string, status: number) => {
  return await postAction(DictApiUrl.DICT_CHANGE_STATUS, { id, status });
}

export const saveOrUpdateDict = async (data: DictModel) => {
  const httpUrl = data.id ? DictApiUrl.DICT_EDIT : DictApiUrl.DICT_ADD;
  const method = data.id ? 'put' : 'post';
  return await httpAction(httpUrl, data, method);
};

export const validateDictItemTextApi = async (id: string, dictCode: string, dictItemText: string) => {
  const res: any = await getAction(DictApiUrl.VALIDATE_ITEM, { id, dictCode, dictItemText });
  if (res.code === 500) {
    return Promise.reject("名称已存在!");
  } else {
    return Promise.resolve();
  }
}

export const validateDictItemValueApi = async (id: string, dictCode: string, dictItemValue: string) => {
  const res: any = await getAction(DictApiUrl.VALIDATE_ITEM, { id, dictCode, dictItemValue });
  if (res.code === 500) {
    return Promise.reject("数据值已存在!");
  } else {
    return Promise.resolve();
  }
}

export const changeStatusItemApi = async (id: string, status: number) => {
  return await postAction(DictApiUrl.DICT_ITEM_CHANGE_STATUS, { id, status });
}

export const saveOrUpdateDictItem = async (data: DictItemModel) => {
  const httpUrl = data.id ? DictApiUrl.DICT_ITEM_EDIT : DictApiUrl.DICT_ITEM_ADD;
  const method = data.id ? 'put' : 'post';
  return await httpAction(httpUrl, data, method);
};