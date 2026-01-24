import { getAction, postAction, httpAction } from '@/utils/httpAction';
import type { MenuModel } from './menu.types';

enum MenuApiUrl {
  INDEX_CHANGE_STATUS = '/system/menu/changeStatus',
  
  LIST = '/system/menu/list',
  ADD = '/system/menu/add',
  EDIT = '/system/menu/edit',
  DELETE = '/system/menu/delete',
  VALIDATE = '/system/menu/validate',
}

export { MenuApiUrl };

export const getMenuListApi = async () => {
  return await getAction(MenuApiUrl.LIST, {});
}

export const validateNameApi = async (id: string, name: string) => {
  const res: any = await getAction(MenuApiUrl.VALIDATE, { id, name });
  if (res.code === 500) {
    return Promise.reject("路由名称已存在!");
  } else {
    return Promise.resolve();
  }
}

export const validatePathApi = async (id: string, path: string) => {
  const res: any = await getAction(MenuApiUrl.VALIDATE, { id, path });
  if (res.code === 500) {
    return Promise.reject("路由地址已存在!");
  } else {
    return Promise.resolve();
  }
}

export const changeStatusApi = async (id: string, status: number) => {
  return await postAction(MenuApiUrl.INDEX_CHANGE_STATUS, { id, status });
}

export const saveOrUpdate = async (data: MenuModel) => {
  const httpUrl = data.id ? MenuApiUrl.EDIT : MenuApiUrl.ADD;
  const method = data.id ? 'put' : 'post';
  return await httpAction(httpUrl, data, method);
};