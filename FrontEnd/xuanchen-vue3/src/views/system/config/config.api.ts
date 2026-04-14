import { getAction, postAction, httpAction } from '@/utils/httpAction';
import type { ConfigModel } from './config.types';

enum ConfigApiUrl {
  INDEX_LIST = '/system/config/list',
  INDEX_DELETE = '/system/config/delete',
  INDEX_DELETE_BATCH = '/system/config/deleteBatch',
  INDEX_EXPORT_EXCEL = '/system/config/exportExcel',
  INDEX_IMPORT_EXCEL = '/system/config/importExcel',
  INDEX_CHANGE_STATUS = '/system/config/changeStatus',

  OPERATION_ADD = '/system/config/add',
  OPERATION_EDIT = '/system/config/edit',
  OPERATION_VALIDATE = '/system/config/validate',

  RECBIN_LIST = '/system/config/listRecycleBin',
  RECBIN_DELETE = '/system/config/deleteRecycleBin',
  RECBIN_DELETE_BATCH = '/system/config/deleteRecycleBinBatch',
  RECBIN_REVERT = '/system/config/revertRecycleBin',
  RECBIN_REVERT_BATCH = '/system/config/revertRecycleBinBatch',

  GET_CONFIG_KEY_VALUE = '/system/config/getConfigKeyValue',
  SET_CONFIG_KEY_VALUE = '/system/config/setConfigKeyValue'
}

export { ConfigApiUrl };

export const validateConfigNameApi = async (id: string, configName: string) => {
  const res: any = await getAction(ConfigApiUrl.OPERATION_VALIDATE, { id, configName });
  if (res.code === 500) {
    return Promise.reject("参数名称已存在!");
  } else {
    return Promise.resolve();
  }
}

export const validateConfigKeyApi = async (id: string, configKey: string) => {
  const res: any = await getAction(ConfigApiUrl.OPERATION_VALIDATE, { id, configKey });
  if (res.code === 500) {
    return Promise.reject("参数键名已存在!");
  } else {
    return Promise.resolve();
  }
}

export const changeStatusApi = async (id: string, status: number) => {
  return await postAction(ConfigApiUrl.INDEX_CHANGE_STATUS, { id, status });
}

export const saveOrUpdate = async (data: ConfigModel) => {
  const httpUrl = data.id ? ConfigApiUrl.OPERATION_EDIT : ConfigApiUrl.OPERATION_ADD;
  const method = data.id ? 'put' : 'post';
  return await httpAction(httpUrl, data, method);
};

export const getConfigKeyValueApi = async (configKey: string) => {
  return await getAction(ConfigApiUrl.GET_CONFIG_KEY_VALUE, { configKey });
}

export const setConfigKeyValueApi = async (configKey: string, configValue: string) => {
  return await postAction(ConfigApiUrl.SET_CONFIG_KEY_VALUE, { configKey, configValue });
}