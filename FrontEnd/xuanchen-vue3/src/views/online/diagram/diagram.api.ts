import { getAction, deleteAction, postFormAction } from '@/utils/httpAction';
import type { Result } from '@/types/api';
import type { OnlineDocRecord } from './diagram.types';

enum DiagramApiUrl {
  INDEX_LIST = '/online/diagram/list',
  INDEX_DELETE = '/online/diagram/delete',

  EDITOR_DETAIL = '/online/diagram/detail',
  EDITOR_SAVE = '/online/diagram/save',
}

export { DiagramApiUrl };

/** 流程图详情（含快照） */
export const getDiagramDetailApi = (id: string): Promise<Result<OnlineDocRecord>> =>
  getAction<OnlineDocRecord>(DiagramApiUrl.EDITOR_DETAIL, { id });

/** 保存流程图（FormData：id 可空、docName、snapshot） */
export const saveDiagramApi = (
  id: string | undefined,
  docName: string,
  snapshot: string,
): Promise<Result<OnlineDocRecord>> => {
  const formData = new FormData();
  if (id) formData.append('id', id);
  formData.append('docName', docName);
  formData.append('snapshot', snapshot);
  return postFormAction<OnlineDocRecord>(DiagramApiUrl.EDITOR_SAVE, formData);
};

/** 删除流程图（逻辑删） */
export const deleteDiagramApi = (id: string): Promise<Result<null>> =>
  deleteAction<null>(DiagramApiUrl.INDEX_DELETE, { id });
