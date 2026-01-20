import { getAction, postAction, httpAction, deleteAction } from '@/utils/httpAction';
import type { DeptModel } from './dept.types';
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

export const getDeptTreeApi = async () => {
  return await getAction(DeptApiUrl.DEPT_TREE, {});
};

export const getSelectedDeptApi = async (deptCode: string) => {
  return await getAction(DeptApiUrl.INFO_SELECTED_DEPT, { deptCode });
}

export const createDeptCodeApi = async (parentDeptCode: string) => {
  return await postAction(DeptApiUrl.INFO_CREATE_DEPT_CODE, { parentDeptCode });
}

export const saveOrUpdate = async (data: DeptModel) => {
  const httpUrl = data.id ? DeptApiUrl.INFO_EDIT : DeptApiUrl.INFO_ADD;
  const method = data.id ? 'put' : 'post';
  return await httpAction(httpUrl, data, method);
};

export const deleteDeptApi = async (deptCode: string) => {
  return await deleteAction(DeptApiUrl.TREE_DELETE, { deptCode });
}

export const link = async (listUser: string[], deptCode: string) => {
  return await postAction(DeptApiUrl.USER_LINK, { listUser, deptCode });
}

export const unlink = async (id: string) => {
  return await deleteAction(DeptApiUrl.USER_UNLINK, { id });
}