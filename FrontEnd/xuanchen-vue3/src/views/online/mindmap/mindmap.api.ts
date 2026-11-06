import { getAction, deleteAction, postFormAction } from '@/utils/httpAction';
import type { Result } from '@/types/api';
import type { OnlineDocRecord } from './mindmap.types';

enum MindmapApiUrl {
  INDEX_LIST = '/online/mindmap/list',
  INDEX_DELETE = '/online/mindmap/delete',

  EDITOR_DETAIL = '/online/mindmap/detail',
  EDITOR_SAVE = '/online/mindmap/save',
}

export { MindmapApiUrl };

/** 思维导图详情（含快照） */
export const getMindmapDetailApi = (id: string): Promise<Result<OnlineDocRecord>> =>
  getAction<OnlineDocRecord>(MindmapApiUrl.EDITOR_DETAIL, { id });

/** 保存思维导图（FormData：id 可空、docName、snapshot） */
export const saveMindmapApi = (
  id: string | undefined,
  docName: string,
  snapshot: string,
): Promise<Result<OnlineDocRecord>> => {
  const formData = new FormData();
  if (id) formData.append('id', id);
  formData.append('docName', docName);
  formData.append('snapshot', snapshot);
  return postFormAction<OnlineDocRecord>(MindmapApiUrl.EDITOR_SAVE, formData);
};

/** 删除思维导图（逻辑删） */
export const deleteMindmapApi = (id: string): Promise<Result<null>> =>
  deleteAction<null>(MindmapApiUrl.INDEX_DELETE, { id });
