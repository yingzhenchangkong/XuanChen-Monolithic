import { getAction, httpAction } from '@/utils/httpAction';
import type { Result, PageResult, MenuRecord } from '@/types/api';
import type { MenuSaveParams } from './menu.types';

enum MenuApiUrl {
  INDEX_LIST = '/system/menu/list',
  INDEX_DELETE = '/system/menu/delete',

  OPERATION_ADD = '/system/menu/add',
  OPERATION_EDIT = '/system/menu/edit',
  OPERATION_VALIDATE = '/system/menu/validate',
}

export { MenuApiUrl };

/**
 * 菜单树（无分页）：后端 data 为 { records: 菜单节点树 }，
 * 与 useList 中 PageResult<T> 的 records 约定保持一致。
 */
export const getMenuListApi = (): Promise<Result<PageResult<MenuRecord>>> => {
  return getAction<PageResult<MenuRecord>>(MenuApiUrl.INDEX_LIST, {});
}

export const validateNameApi = async (id: string, name: string): Promise<void> => {
  const res = await getAction(MenuApiUrl.OPERATION_VALIDATE, { id, name });
  if (res.code !== 200) {
    return Promise.reject("路由名称已存在!");
  } else {
    return Promise.resolve();
  }
}

export const validatePathApi = async (id: string, path: string): Promise<void> => {
  const res = await getAction(MenuApiUrl.OPERATION_VALIDATE, { id, path });
  if (res.code !== 200) {
    return Promise.reject("路由地址已存在!");
  } else {
    return Promise.resolve();
  }
}

export const saveOrUpdate = (data: MenuSaveParams): Promise<Result<null>> => {
  const httpUrl = data.id ? MenuApiUrl.OPERATION_EDIT : MenuApiUrl.OPERATION_ADD;
  const method = data.id ? 'put' : 'post';
  return httpAction<null>(httpUrl, data, method);
};
