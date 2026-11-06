import { getAction, deleteAction, postFormAction, uploadAction } from '@/utils/httpAction';
import type { Result, PageResult } from '@/types/api';

/** 文件元数据（与后端 FileInfo 对应，仅列表展示所需字段） */
export interface FileInfoRecord {
  id: string;
  bucket: string;
  objectKey: string;
  originalName: string;
  ext: string;
  contentType?: string;
  fileSize: number;
  storageType: number;
  bizType: string;
  username?: string;
  createTime?: string;
  deleteTime?: string;
}

/** 上传返回的摘要 */
export interface FileInfoSummary {
  id: string;
  name: string;
  ext: string;
  size: number;
  previewUrl: string;
}

/** 配额用量行（admin 总览） */
export interface QuotaUsageRecord {
  username: string;
  /** -1 = 未单独设置（使用系统默认） */
  quotaBytes: number;
  usedBytes: number;
}

/** 共享记录（我的共享） */
export interface ShareRecord {
  id: string;
  shareNo: string;
  fileId: string;
  expireTime?: string;
  visitCount: number;
  hasCode: boolean;
  createTime?: string;
  fileName: string;
  fileExt: string;
  fileSize: number;
}

/** 公开分享信息（/share/info） */
export interface ShareInfo {
  name: string;
  ext: string;
  size: number;
  expireTime?: string;
  hasCode: boolean;
  url?: string;
}

/** 文件夹记录（平铺，前端组树） */
export interface FolderRecord {
  id: string;
  folderName: string;
  parentId: string;
  username?: string;
  createTime?: string;
}

/** 分片任务初始化返回：秒传命中 {instant,file}；否则 {uploadId,uploaded} */
export interface ChunkInitResult {
  instant: boolean;
  file?: FileInfoSummary;
  uploadId?: string;
  uploaded?: number[];
}

/** 文本预览内容（服务端已探测编码并解码；超过上限的内容会截断） */
export interface FileTextContent {
  /** 解码后的文本 */
  content: string;
  /** 实际使用的编码（自动探测结果或用户指定值） */
  charset: string;
  /** 是否因超过单次读取上限被截断 */
  truncated: boolean;
  /** 文件总字节数 */
  fileSize: number;
}

enum FileManageApiUrl {
  // ---------- 我的文件列表页（XCListPage my/MyFile.vue）----------
  INDEX_MY_LIST = '/fileinfo/my',
  INDEX_MY_DELETE = '/fileinfo/delete',
  // 回收站列表页（XCListPage recycle/RecycleFile.vue）
  RECBIN_LIST = '/fileinfo/recycle',
  OPERATION_RESTORE = '/fileinfo/restore',
  OPERATION_PURGE = '/fileinfo/purge',

  // ---------- 预览 / 下载 ----------
  VIEW_URL = '/fileinfo/url',
  VIEW_TEXT = '/fileinfo/text',

  // ---------- 上传 ----------
  UPLOAD = '/fileinfo/upload',
  UPLOAD_INSTANT = '/fileinfo/instant',
  UPLOAD_CHUNK_INIT = '/fileinfo/chunk/init',
  UPLOAD_CHUNK_PART = '/fileinfo/chunk',
  UPLOAD_CHUNK_COMPLETE = '/fileinfo/chunk/complete',

  // ---------- 配额 ----------
  QUOTA_MY = '/fileinfo/quota/my',
  QUOTA_LIST = '/fileinfo/quota/list',
  QUOTA_SAVE = '/fileinfo/quota/save',

  // ---------- 分享 ----------
  SHARE_SAVE = '/fileinfo/share/save',
  SHARE_MY = '/fileinfo/share/my',
  SHARE_DELETE = '/fileinfo/share/delete',
  SHARE_INFO = '/share/info',
  SHARE_DOWNLOAD = '/share/download',

  // ---------- 文件夹 ----------
  FOLDER_TREE = '/fileinfo/folder/tree',
  FOLDER_CREATE = '/fileinfo/folder/create',
  FOLDER_RENAME = '/fileinfo/folder/rename',
  FOLDER_DELETE = '/fileinfo/folder/delete',

  // ---------- ONLYOFFICE ----------
  OFFICE_CONFIG = '/office/config',
}

export { FileManageApiUrl };

/** ONLYOFFICE 协同编辑配置（后端整签，前端原样喂 DocsAPI.DocEditor） */
export interface OfficeEditorConfig {
  dsUrl: string;
  config: Record<string, unknown>;
}

/** 通用文件上传（FormData：file + bizType） */
export const uploadFileInfoApi = (formData: FormData): Promise<Result<FileInfoSummary>> =>
  uploadAction<FileInfoSummary>(FileManageApiUrl.UPLOAD, formData);

/** 我的文件分页 */
export const getMyFilePageApi = (
  params: Record<string, unknown>,
): Promise<Result<PageResult<FileInfoRecord>>> =>
  getAction<PageResult<FileInfoRecord>>(FileManageApiUrl.INDEX_MY_LIST, params);

/** 获取预览/下载地址（预签名 URL 有有效期，必须即用即取） */
export const getFileUrlApi = (id: string, inline: boolean): Promise<Result<string>> =>
  getAction<string>(FileManageApiUrl.VIEW_URL, { id, inline });

/** 文本内容预览：服务端按 BOM/UTF-8/GB18030 探测编码后解码，charset 传值可手动指定编码 */
export const getFileTextApi = (id: string, charset?: string): Promise<Result<FileTextContent>> =>
  getAction<FileTextContent>(FileManageApiUrl.VIEW_TEXT, { id, charset: charset || undefined });

/** 删除我的文件（进回收站） */
export const deleteFileInfoApi = (id: string): Promise<Result<null>> =>
  deleteAction<null>(FileManageApiUrl.INDEX_MY_DELETE, { id });

/** 回收站分页 */
export const getRecyclePageApi = (
  params: Record<string, unknown>,
): Promise<Result<PageResult<FileInfoRecord>>> =>
  getAction<PageResult<FileInfoRecord>>(FileManageApiUrl.RECBIN_LIST, params);

/** 从回收站恢复（后端 @RequestParam，须用表单 POST） */
export const restoreFileApi = (id: string): Promise<Result<null>> => {
  const form = new FormData();
  form.append('id', id);
  return postFormAction<null>(FileManageApiUrl.OPERATION_RESTORE, form);
};

/** 彻底删除（物理删） */
export const purgeFileApi = (id: string): Promise<Result<null>> =>
  deleteAction<null>(FileManageApiUrl.OPERATION_PURGE, { id });

/** 秒传探测：data=null 表示未命中（转常规上传） */
export const instantUploadApi = (params: {
  name: string;
  md5: string;
  bizType: string;
}): Promise<Result<FileInfoSummary | null>> => getAction<FileInfoSummary | null>(FileManageApiUrl.UPLOAD_INSTANT, params);

/** 我的配额用量 */
export const getQuotaMyApi = (): Promise<Result<{ usedBytes: number; quotaBytes: number }>> =>
  getAction<{ usedBytes: number; quotaBytes: number }>(FileManageApiUrl.QUOTA_MY);

/** 配额用量总览（admin） */
export const getQuotaListApi = (): Promise<Result<QuotaUsageRecord[]>> =>
  getAction<QuotaUsageRecord[]>(FileManageApiUrl.QUOTA_LIST);

/** 保存用户配额（admin）：quotaBytes=-1 恢复默认（后端 @RequestParam，须用表单 POST） */
export const saveQuotaApi = (username: string, quotaBytes: number): Promise<Result<null>> => {
  const form = new FormData();
  form.append('username', username);
  form.append('quotaBytes', String(quotaBytes));
  return postFormAction<null>(FileManageApiUrl.QUOTA_SAVE, form);
};

/** 创建共享（后端 @RequestParam，须用表单 POST） */
export const createShareApi = (
  fileId: string,
  expireHours: number,
  shareCode?: string,
): Promise<Result<{ id: string; shareNo: string; expireTime?: string }>> => {
  const form = new FormData();
  form.append('fileId', fileId);
  form.append('expireHours', String(expireHours));
  if (shareCode) {
    form.append('shareCode', shareCode);
  }
  return postFormAction<{ id: string; shareNo: string; expireTime?: string }>(FileManageApiUrl.SHARE_SAVE, form);
};

/** 我的共享列表 */
export const getMySharesApi = (): Promise<Result<ShareRecord[]>> =>
  getAction<ShareRecord[]>(FileManageApiUrl.SHARE_MY);

/** 撤销共享 */
export const revokeShareApi = (id: string): Promise<Result<null>> =>
  deleteAction<null>(FileManageApiUrl.SHARE_DELETE, { id });

/** 公开：分享信息（免登录） */
export const getShareInfoApi = (no: string): Promise<Result<ShareInfo>> =>
  getAction<ShareInfo>(FileManageApiUrl.SHARE_INFO, { no });

/** 公开：分享下载（免登录，返回现签下载地址） */
export const getShareDownloadApi = (no: string, code?: string): Promise<Result<ShareInfo>> =>
  getAction<ShareInfo>(FileManageApiUrl.SHARE_DOWNLOAD, { no, code: code || undefined });

// ==================== 三期：文件夹 / 分片上传 / 在线预览 ====================

/** 我的文件夹列表（平铺） */
export const getFolderTreeApi = (): Promise<Result<FolderRecord[]>> =>
  getAction<FolderRecord[]>(FileManageApiUrl.FOLDER_TREE);

/** 创建文件夹（parentId 空/0 = 根目录） */
export const createFolderApi = (parentId: string, folderName: string): Promise<Result<FolderRecord>> => {
  const form = new FormData();
  form.append('parentId', parentId || '0');
  form.append('folderName', folderName);
  return postFormAction<FolderRecord>(FileManageApiUrl.FOLDER_CREATE, form);
};

/** 重命名文件夹 */
export const renameFolderApi = (id: string, folderName: string): Promise<Result<null>> => {
  const form = new FormData();
  form.append('id', id);
  form.append('folderName', folderName);
  return postFormAction<null>(FileManageApiUrl.FOLDER_RENAME, form);
};

/** 删除文件夹（递归子树，文件移入回收站） */
export const deleteFolderApi = (id: string): Promise<Result<null>> => {
  const form = new FormData();
  form.append('id', id);
  return postFormAction<null>(FileManageApiUrl.FOLDER_DELETE, form);
};

/** 分片任务初始化（含秒传探测与断点续传命中） */
export const chunkInitApi = (fields: Record<string, string | number>): Promise<Result<ChunkInitResult>> => {
  const form = new FormData();
  Object.entries(fields).forEach(([k, v]) => form.append(k, String(v)));
  return postFormAction<ChunkInitResult>(FileManageApiUrl.UPLOAD_CHUNK_INIT, form);
};

/** 上传单片 */
export const uploadChunkApi = (form: FormData): Promise<Result<null>> =>
  uploadAction<null>(FileManageApiUrl.UPLOAD_CHUNK_PART, form);

/** 完成合并 */
export const chunkCompleteApi = (uploadId: string): Promise<Result<{ instant: boolean; file: FileInfoSummary }>> => {
  const form = new FormData();
  form.append('uploadId', uploadId);
  return postFormAction<{ instant: boolean; file: FileInfoSummary }>(FileManageApiUrl.UPLOAD_CHUNK_COMPLETE, form);
};

/** ONLYOFFICE 编辑器配置（mode：edit=协同编辑，view=只读原格式预览；config 为后端整签，前端原样喂 DocsAPI.DocEditor） */
export const getOfficeConfigApi = (
  fileId: string,
  mode: 'edit' | 'view' = 'edit',
): Promise<Result<OfficeEditorConfig>> =>
  getAction<OfficeEditorConfig>(FileManageApiUrl.OFFICE_CONFIG, { fileId, mode });
