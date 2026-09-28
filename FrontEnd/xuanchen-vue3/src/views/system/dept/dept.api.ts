import { getAction, postAction, httpAction, deleteAction } from '@/utils/httpAction';
import type { Result } from '@/types/api';
import type { DeptSaveParams, DeptRecord, DeptSelectedRecord } from './dept.types';

enum DeptApiUrl {
  DEPT_TREE = '/system/dept/getDeptTree',
  INFO_SELECTED_DEPT = '/system/dept/getSelectedDept',
  INFO_CREATE_DEPT_CODE = '/system/dept/createDeptCode',
  INFO_ADD = '/system/dept/add',
  INFO_EDIT = '/system/dept/edit',
  TREE_DELETE = '/system/dept/delete',
  USER_LIST = '/system/userdept/listDeptUser',
  USER_LINK = '/system/userdept/link',
  USER_UNLINK = '/system/userdept/unlink',
}

export { DeptApiUrl };

/**
 * 历史接口：getDeptTree/getSelectedDept/createDeptCode 后端直接返回业务体
 * （数组 / 对象 / 字符串），没有 Result 外壳，这里按真实响应体收口类型。
 */
export const getDeptTreeApi = async (): Promise<DeptRecord[]> => {
  return await getAction(DeptApiUrl.DEPT_TREE, {}) as unknown as DeptRecord[];
};

export const getSelectedDeptApi = async (deptCode: string): Promise<DeptSelectedRecord> => {
  return await getAction(DeptApiUrl.INFO_SELECTED_DEPT, { deptCode }) as unknown as DeptSelectedRecord;
}

export const createDeptCodeApi = async (parentDeptCode: string): Promise<string> => {
  return await postAction(DeptApiUrl.INFO_CREATE_DEPT_CODE, { parentDeptCode }) as unknown as string;
}

export const saveOrUpdate = (data: DeptSaveParams): Promise<Result<null>> => {
  const httpUrl = data.id ? DeptApiUrl.INFO_EDIT : DeptApiUrl.INFO_ADD;
  const method = data.id ? 'put' : 'post';
  return httpAction<null>(httpUrl, data, method);
};

export const deleteDeptApi = (deptCode: string): Promise<Result<null>> => {
  return deleteAction<null>(DeptApiUrl.TREE_DELETE, { deptCode });
}

export const link = (listUser: string[], deptCode: string): Promise<Result<null>> => {
  return postAction<null>(DeptApiUrl.USER_LINK, { listUser, deptCode });
}

export const unlink = (id: string): Promise<Result<null>> => {
  return deleteAction<null>(DeptApiUrl.USER_UNLINK, { id });
}
