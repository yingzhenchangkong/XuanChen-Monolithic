import { getAction, postAction, httpAction } from '@/utils/httpAction';
import type { Result, SelectOption, PageResult } from '@/types/api';
import type { PostModel, UnAssignUserOption } from './post.types';

enum PostApiUrl {
  INDEX_LIST = '/system/post/list',
  INDEX_DELETE = '/system/post/delete',
  INDEX_DELETE_BATCH = '/system/post/deleteBatch',
  INDEX_EXPORT_EXCEL = '/system/post/exportExcel',
  INDEX_IMPORT_EXCEL = '/system/post/importExcel',
  INDEX_CHANGE_STATUS = '/system/post/changeStatus',

  OPERATION_ADD = '/system/post/add',
  OPERATION_EDIT = '/system/post/edit',
  OPERATION_VALIDATE = '/system/post/validate',

  RECBIN_LIST = '/system/post/listRecycleBin',
  RECBIN_DELETE = '/system/post/deleteRecycleBin',
  RECBIN_DELETE_BATCH = '/system/post/deleteRecycleBinBatch',
  RECBIN_REVERT = '/system/post/revertRecycleBin',
  RECBIN_REVERT_BATCH = '/system/post/revertRecycleBinBatch',

  SELECT = '/system/post/select',

  ASSIGN_USER_LIST_ASSIGN_USER = '/system/userpost/listAssignUser',
  ASSIGN_USER_LIST_UN_ASSIGN_USER = '/system/userpost/listUnAssignUser',
  ASSIGN_USER_ASSIGN = '/system/userpost/assign',
  ASSIGN_USER_ASSIGN_BATCH = '/system/userpost/assignBatch',
  ASSIGN_USER_CANCEL_ASSIGN = '/system/userpost/cancelAssign',
  ASSIGN_USER_CANCEL_ASSIGN_BATCH = '/system/userpost/cancelAssignBatch',
}

export { PostApiUrl };

export const getPostSelect = async (): Promise<SelectOption[]> => {
  const res = await getAction<SelectOption[]>(PostApiUrl.SELECT, {});
  return res.data;
}

export const validatePostCodeApi = async (id: string, postCode: string): Promise<void> => {
  const res = await getAction(PostApiUrl.OPERATION_VALIDATE, { id, postCode });
  if (res.code !== 200) {
    return Promise.reject("岗位编码已存在!");
  } else {
    return Promise.resolve();
  }
}

export const validatePostNameApi = async (id: string, postName: string): Promise<void> => {
  const res = await getAction(PostApiUrl.OPERATION_VALIDATE, { id, postName });
  if (res.code !== 200) {
    return Promise.reject("岗位名称已存在!");
  } else {
    return Promise.resolve();
  }
}

export const changeStatusApi = (id: string, status: number): Promise<Result<null>> => {
  return postAction<null>(PostApiUrl.INDEX_CHANGE_STATUS, { id, status });
}

export const saveOrUpdate = (data: PostModel) => {
  const httpUrl = data.id ? PostApiUrl.OPERATION_EDIT : PostApiUrl.OPERATION_ADD;
  const method = data.id ? 'put' : 'post';
  return httpAction(httpUrl, data, method);
};

export const cancelAssignUser = (userId: string, postId: string): Promise<Result<null>> => {
  return postAction<null>(PostApiUrl.ASSIGN_USER_CANCEL_ASSIGN, { userId, postId });
};

export const cancleAssignUserBatch = (userIds: string[], postId: string): Promise<Result<null>> => {
  return postAction<null>(PostApiUrl.ASSIGN_USER_CANCEL_ASSIGN_BATCH, { userIds, postId });
}

export const assignUser = (userId: string, postId: string): Promise<Result<null>> => {
  return postAction<null>(PostApiUrl.ASSIGN_USER_ASSIGN, { userId, postId });
};

export const assignUserBatch = (userIds: string[], postId: string): Promise<Result<null>> => {
  return postAction<null>(PostApiUrl.ASSIGN_USER_ASSIGN_BATCH, { userIds, postId });
}

export const getListUnAssignUser = (postId: string) => {
  return getAction<PageResult<UnAssignUserOption>>(PostApiUrl.ASSIGN_USER_LIST_UN_ASSIGN_USER, { postId });
}
