import { getAction, postAction, deleteAction } from '@/utils/httpAction';
import type { Result } from '@/types/api';
import type { TemplateRecord } from './template.types';

enum TemplateApiUrl {
  INDEX_LIST = '/report/template/list',
  INDEX_DELETE = '/report/template/delete',
  INDEX_DELETE_BATCH = '/report/template/deleteBatch',
  OPERATION_ADD = '/report/template/add',
  OPERATION_EDIT = '/report/template/edit',
  OPERATION_CHANGE_STATUS = '/report/template/changeStatus',
  COPY = '/report/template/copy',
  ALL = '/report/template/all',
  CONFIG = '/report/template/config',
  SAVE_CONFIG = '/report/template/saveConfig',
}

export { TemplateApiUrl };

export const saveOrUpdateTemplate = (data: TemplateRecord): Promise<Result<null>> => {
  const url = data.id ? TemplateApiUrl.OPERATION_EDIT : TemplateApiUrl.OPERATION_ADD;
  return postAction<null>(url, data);
};

export const copyTemplateApi = (id: string, templateCode: string, templateName: string): Promise<Result<null>> => {
  return postAction<null>(TemplateApiUrl.COPY, { id, templateCode, templateName });
};

export const changeTemplateStatusApi = (id: string, status: number): Promise<Result<null>> => {
  return postAction<null>(TemplateApiUrl.OPERATION_CHANGE_STATUS, { id, status });
};

export const getAllTemplatesApi = async (): Promise<TemplateRecord[]> => {
  const res = await getAction<TemplateRecord[]>(TemplateApiUrl.ALL);
  if (res.code !== 200) {
    return Promise.reject(res.msg || '加载模板列表失败');
  }
  return res.data ?? [];
};

export const getTemplateConfigApi = async (id: string) => {
  const res = await getAction<{ template: TemplateRecord }>(TemplateApiUrl.CONFIG, { id });
  if (res.code !== 200) {
    return Promise.reject(res.msg || '加载模板失败');
  }
  return res.data;
};

export const saveTemplateConfigApi = (id: string, configJson: string): Promise<Result<null>> =>
  postAction<null>(TemplateApiUrl.SAVE_CONFIG, { id, configJson });
