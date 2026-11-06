import { getAction, postAction, postFormAction, deleteAction } from '@/utils/httpAction';
import type { Result, PageResult } from '@/types/api';
import type { AiDocRecord, AiSearchHit, FileCenterRecord } from './knowledge.types';

enum AiKbApiUrl {
  // 知识库列表页
  INDEX_LIST = '/ai/kb/list',
  INDEX_CHANGE_STATUS = '/ai/kb/changeStatus',

  // 知识库新增/编辑/删除
  OPERATION_ADD = '/ai/kb/add',
  OPERATION_EDIT = '/ai/kb/edit',
  OPERATION_DELETE = '/ai/kb/delete',
  OPERATION_DELETE_BATCH = '/ai/kb/deleteBatch',

  // 回收站
  RECBIN_LIST = '/ai/kb/listRecycleBin',
  RECBIN_DELETE = '/ai/kb/deleteRecycleBin',
  RECBIN_DELETE_BATCH = '/ai/kb/deleteRecycleBinBatch',
  RECBIN_REVERT = '/ai/kb/revertRecycleBin',
  RECBIN_REVERT_BATCH = '/ai/kb/revertRecycleBinBatch',

  // 文档
  DOC_LIST = '/ai/kb/doc/list',
  DOC_PARSE = '/ai/kb/doc/parse',
  DOC_DELETE = '/ai/kb/doc/delete',
  DOC_TEXT = '/ai/kb/doc/text',
  DOC_IMPORT_FROM_CENTER = '/ai/kb/doc/importFromCenter',

  // 文件中心（导入选择器用）
  FILE_MY = '/fileinfo/my',

  // 检索测试
  SEARCH = '/ai/kb/search',
}

export { AiKbApiUrl };

/** 兼容旧别名 */
export const AiKbUrl = AiKbApiUrl;

export const changeStatusApi = (id: string, status: number): Promise<Result<null>> =>
  postAction<null>(AiKbApiUrl.INDEX_CHANGE_STATUS, { id, status });

export const saveKbApi = (data: { id?: string; kbName: string; kbDesc?: string; chunkSize?: number; chunkOverlap?: number }): Promise<Result<null>> =>
  postAction<null>(data.id ? AiKbApiUrl.OPERATION_EDIT : AiKbApiUrl.OPERATION_ADD, data);

export const listDocsApi = (kbId: string, pageNo = 1, pageSize = 50): Promise<Result<PageResult<AiDocRecord>>> =>
  getAction<PageResult<AiDocRecord>>(AiKbApiUrl.DOC_LIST, { kbId, pageNo, pageSize });

/** 触发解析（后端 @RequestParam，走表单编码） */
export const parseDocApi = (id: string): Promise<Result<null>> => {
  const form = new FormData();
  form.append('id', id);
  return postFormAction<null>(AiKbApiUrl.DOC_PARSE, form);
};

export const deleteDocApi = (id: string): Promise<Result<null>> =>
  deleteAction<null>(AiKbApiUrl.DOC_DELETE, { id });

/** 手动录入知识：正文包装为 txt 入库并自动解析 */
export const addDocTextApi = (kbId: string, title: string, content: string): Promise<Result<null>> =>
  postAction<null>(AiKbApiUrl.DOC_TEXT, { kbId, title, content });

/** 我的文件分页（文件中心） */
export const listMyFilesApi = (params: { pageNo: number; pageSize: number; keyword?: string }): Promise<Result<PageResult<FileCenterRecord>>> =>
  getAction<PageResult<FileCenterRecord>>(AiKbApiUrl.FILE_MY, params);

/** 从文件中心导入已有文件（不重复上传，直接按 fileId 建关联并解析） */
export const importFromCenterApi = (kbId: string, fileIds: string[]): Promise<Result<string>> =>
  postAction<string>(AiKbApiUrl.DOC_IMPORT_FROM_CENTER, { kbId, fileIds });

export const searchKbApi = (query: string, kbIds: string[]): Promise<Result<AiSearchHit[]>> =>
  postAction<AiSearchHit[]>(AiKbApiUrl.SEARCH, { query, kbIds });
