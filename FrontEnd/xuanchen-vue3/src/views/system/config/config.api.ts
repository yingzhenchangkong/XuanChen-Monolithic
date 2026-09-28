import { getAction, postAction, httpAction } from '@/utils/httpAction';
import type { Result } from '@/types/api';
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

export const validateConfigNameApi = async (id: string, configName: string): Promise<void> => {
  const res = await getAction(ConfigApiUrl.OPERATION_VALIDATE, { id, configName });
  if (res.code !== 200) {
    return Promise.reject("参数名称已存在!");
  } else {
    return Promise.resolve();
  }
}

export const validateConfigKeyApi = async (id: string, configKey: string): Promise<void> => {
  const res = await getAction(ConfigApiUrl.OPERATION_VALIDATE, { id, configKey });
  if (res.code !== 200) {
    return Promise.reject("参数键名已存在!");
  } else {
    return Promise.resolve();
  }
}

export const changeStatusApi = (id: string, status: number): Promise<Result<null>> => {
  return postAction<null>(ConfigApiUrl.INDEX_CHANGE_STATUS, { id, status });
}

export const saveOrUpdate = (data: ConfigModel) => {
  const httpUrl = data.id ? ConfigApiUrl.OPERATION_EDIT : ConfigApiUrl.OPERATION_ADD;
  const method = data.id ? 'put' : 'post';
  return httpAction(httpUrl, data, method);
};

export const getConfigKeyValueApi = (configKey: string): Promise<Result<{ configValue: string }>> => {
  return getAction<{ configValue: string }>(ConfigApiUrl.GET_CONFIG_KEY_VALUE, { configKey });
}

export const setConfigKeyValueApi = (configKey: string, configValue: string) => {
  return postAction(ConfigApiUrl.SET_CONFIG_KEY_VALUE, { configKey, configValue });
}
