import { getAction, postAction, deleteAction } from '@/utils/httpAction';
import type { Result } from '@/types/api';
import type { BpmDefinitionRecord } from './definition.types';

enum DefinitionApiUrl {
  INDEX_LIST = '/businessprocess/definition/list',
  INDEX_DELETE = '/businessprocess/definition/delete',
  INDEX_DELETE_BATCH = '/businessprocess/definition/deleteBatch',
  OPERATION_ADD = '/businessprocess/definition/add',
  OPERATION_EDIT = '/businessprocess/definition/edit',
  OPERATION_CHANGE_STATUS = '/businessprocess/definition/changeStatus',
  DESIGN_SAVE_DIAGRAM = '/businessprocess/definition/saveDiagram',
  DESIGN_GET_DIAGRAM = '/businessprocess/definition/getDiagram',
  DESIGN_BIND_FORM = '/businessprocess/definition/bindForm',
}

export { DefinitionApiUrl };

export const getDefinitionListApi = (params: Record<string, unknown>): Promise<Result<{ records: BpmDefinitionRecord[]; total: number }>> =>
  getAction(DefinitionApiUrl.INDEX_LIST, params);

export const saveDefinitionApi = (data: BpmDefinitionRecord): Promise<Result<null>> =>
  postAction(data.id ? DefinitionApiUrl.OPERATION_EDIT : DefinitionApiUrl.OPERATION_ADD, data);

export const deleteDefinitionApi = (id: string): Promise<Result<null>> =>
  deleteAction(DefinitionApiUrl.INDEX_DELETE, { id });

export const changeDefinitionStatusApi = (id: string, status: number): Promise<Result<null>> =>
  postAction(DefinitionApiUrl.OPERATION_CHANGE_STATUS, { id, status });

export const saveDiagramApi = (id: string, bpmnXml: string): Promise<Result<null>> =>
  postAction(DefinitionApiUrl.DESIGN_SAVE_DIAGRAM, { id, bpmnXml });

export const getDiagramApi = async (id: string): Promise<{ bpmnXml: string; name: string; onlineFormId: string }> => {
  const res = await getAction<{ bpmnXml: string; name: string; onlineFormId: string }>(DefinitionApiUrl.DESIGN_GET_DIAGRAM, { id });
  if (res.code !== 200) {
    return Promise.reject(res.msg || '加载流程图失败');
  }
  return { bpmnXml: res.data?.bpmnXml ?? '', name: res.data?.name ?? '', onlineFormId: res.data?.onlineFormId ?? '' };
};

/** 绑定/解绑在线表单（onlineFormId 传空字符串即解绑） */
export const bindDefinitionFormApi = (id: string, onlineFormId: string): Promise<Result<null>> =>
  postAction(DefinitionApiUrl.DESIGN_BIND_FORM, { id, onlineFormId });
