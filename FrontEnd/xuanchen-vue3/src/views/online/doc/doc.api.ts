import { getAction, deleteAction, postFormAction } from '@/utils/httpAction';
import type { Result } from '@/types/api';
import type { CollaboratorRecord, OnlineFileInfo } from './doc.types';

enum DocApiUrl {
  INDEX_LIST = '/online/doc/list',
  INDEX_CREATE = '/online/doc/create',
  INDEX_RENAME = '/online/doc/rename',
  INDEX_REMOVE = '/online/doc/remove',

  COLLAB_LIST = '/online/doc/collaborator/list',
  COLLAB_ADD = '/online/doc/collaborator/add',
  COLLAB_DELETE = '/online/doc/collaborator/delete',
}

export { DocApiUrl };

/** 新建空白文档（FormData：docType=word/cell/slide、docName），返回文件元数据 */
export const createDocApi = (docType: string, docName: string): Promise<Result<OnlineFileInfo>> => {
  const formData = new FormData();
  formData.append('docType', docType);
  formData.append('docName', docName);
  return postFormAction<OnlineFileInfo>(DocApiUrl.INDEX_CREATE, formData);
};

/** 重命名（仅属主；不改扩展名） */
export const renameDocApi = (fileId: string, docName: string): Promise<Result<null>> => {
  const formData = new FormData();
  formData.append('fileId', fileId);
  formData.append('docName', docName);
  return postFormAction<null>(DocApiUrl.INDEX_RENAME, formData);
};

/** 删除（进文件中心回收站，仅属主） */
export const removeDocApi = (fileId: string): Promise<Result<null>> =>
  deleteAction<null>(DocApiUrl.INDEX_REMOVE, { fileId });

/** 协作者列表（仅属主） */
export const listCollaboratorsApi = (fileId: string): Promise<Result<CollaboratorRecord[]>> =>
  getAction<CollaboratorRecord[]>(DocApiUrl.COLLAB_LIST, { fileId });

/** 添加协作者（FormData：fileId、username、permission） */
export const addCollaboratorApi = (
  fileId: string,
  username: string,
  permission: number,
): Promise<Result<null>> => {
  const formData = new FormData();
  formData.append('fileId', fileId);
  formData.append('username', username);
  formData.append('permission', String(permission));
  return postFormAction<null>(DocApiUrl.COLLAB_ADD, formData);
};

/** 移除协作者 */
export const removeCollaboratorApi = (fileId: string, username: string): Promise<Result<null>> =>
  deleteAction<null>(DocApiUrl.COLLAB_DELETE, { fileId, username });
