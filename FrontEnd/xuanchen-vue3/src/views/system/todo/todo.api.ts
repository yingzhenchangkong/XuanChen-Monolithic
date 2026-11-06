import { postAction } from '@/utils/httpAction';
import type { Result } from '@/types/api';

/** 待办接口地址 */
export enum TodoApiUrl {
  /** 我的待办分页 */
  INDEX_LIST = '/system/todo/list',
  /** 删除（DELETE，供 useList 的 deleteAction 使用） */
  INDEX_DELETE = '/system/todo/delete',
  /** 批量删除（DELETE） */
  INDEX_DELETE_BATCH = '/system/todo/deleteBatch',

  /** 新增 */
  OPERATION_ADD = '/system/todo/add',
  /** 编辑 */
  OPERATION_EDIT = '/system/todo/edit',
  /** 完成待办 */
  OPERATION_FINISH = '/system/todo/finish',
}

/** 新增/编辑待办 */
export const saveOrUpdateTodoApi = (data: { id?: string; title: string; content?: string | null; priority: number; dueDate?: string | null }): Promise<Result<null>> => {
  return postAction<null>(data.id ? TodoApiUrl.OPERATION_EDIT : TodoApiUrl.OPERATION_ADD, data);
}

/** 完成待办 */
export const finishTodoApi = (id: string): Promise<Result<null>> => {
  return postAction<null>(TodoApiUrl.OPERATION_FINISH, { id });
}
