import { deleteAction, getAction, httpAction } from '@/utils/httpAction';
import type { Result } from '@/types/api';
import type { GenProjectProfile } from '../gentable/gentable.types';

enum GenProfileApiUrl {
  INDEX_LIST = '/tool/generator/profile/list',
  INDEX_DELETE = '/tool/generator/profile/delete',

  SELECT = '/tool/generator/profile/select',
  OPERATION_ADD = '/tool/generator/profile/add',
  OPERATION_EDIT = '/tool/generator/profile/edit',
}

export { GenProfileApiUrl };

/** 新增/修改：带 id 走编辑，否则新增 */
export const saveOrUpdate = (data: GenProjectProfile): Promise<Result<string>> => {
  const url = data.id ? GenProfileApiUrl.OPERATION_EDIT : GenProfileApiUrl.OPERATION_ADD;
  return httpAction<string>(url, data, data.id ? 'put' : 'post');
};

/** 删除（逻辑删除） */
export const deleteProfile = (id: string): Promise<Result<unknown>> => {
  return deleteAction<unknown>(GenProfileApiUrl.INDEX_DELETE, { id });
};

/** 下拉：全部预设（读取失败只告警，避免影响表配置页使用） */
export const getProfileSelect = async (): Promise<GenProjectProfile[]> => {
  try {
    const res = await getAction<GenProjectProfile[]>(GenProfileApiUrl.SELECT);
    return res.code === 200 ? res.data || [] : [];
  } catch (error) {
    console.warn('【genprofile】读取项目预设失败:', error);
    return [];
  }
};
