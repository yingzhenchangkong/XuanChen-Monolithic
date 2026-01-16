import { getAction, httpAction } from '@/utils/httpAction';
import type { PostModel } from './post.types';

enum PostApiUrl {
  INDEX_LIST = '/system/post/list',
  INDEX_DELETE = '/system/post/delete',
  INDEX_DELETE_BATCH = '/system/post/deleteBatch',
  INDEX_EXPORT_EXCEL = '/system/post/exportExcel',
  INDEX_IMPORT_EXCEL = '/system/post/importExcel',
  OPERATION_ADD = '/system/post/add',
  OPERATION_EDIT = '/system/post/edit',
  OPERATION_VALIDATE_POST_CODE = '/system/post/validatePostCode',
  OPERATION_VALIDATE_POST_NAME = '/system/post/validatePostName',
  RECBIN_LIST = '/system/post/listRecycleBin',
  RECBIN_DELETE = '/system/post/deleteRecycleBin',
  RECBIN_DELETE_BATCH = '/system/post/deleteRecycleBinBatch',
  RECBIN_REVERT = '/system/post/revertRecycleBin',
  RECBIN_REVERT_BATCH = '/system/post/revertRecycleBinBatch',
}

export { PostApiUrl };

export const validatePostCodeApi = async (id: string, postCode: string) => {
  const res: any = await getAction(PostApiUrl.OPERATION_VALIDATE_POST_CODE, { id, postCode });
  if (res.code === 500) {
    return Promise.reject(res.msg);
  } else {
    return Promise.resolve();
  }
}

export const validatePostNameApi = async (id: string, postName: string) => {
  const res: any = await getAction(PostApiUrl.OPERATION_VALIDATE_POST_NAME, { id, postName });
  if (res.code === 500) {
    return Promise.reject(res.msg);
  } else {
    return Promise.resolve();
  }
}

export const saveOrUpdate = async (data: PostModel) => {
  const httpUrl = data.id ? PostApiUrl.OPERATION_EDIT : PostApiUrl.OPERATION_ADD;
  const method = data.id ? 'put' : 'post';
  return await httpAction(httpUrl, data, method);
};