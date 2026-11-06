import { getAction, postAction } from '@/utils/httpAction';
import type { DashboardSummary, TodoItem, TodoSaveParams } from './index.types';

/** 接口地址（工作台 / 待办） */
export enum IndexApiUrl {
  /** 工作台汇总（未读公告数、待办数、今日登录数、未读公告 top5、待办 top5） */
  DASHBOARD_SUMMARY = '/dashboard/summary',
  /** 我的待办分页 */
  TODO_LIST = '/system/todo/list',
  /** 新增待办 */
  TODO_ADD = '/system/todo/add',
  /** 编辑待办 */
  TODO_EDIT = '/system/todo/edit',
  /** 完成待办 */
  TODO_FINISH = '/system/todo/finish',
  /** 删除待办 */
  TODO_DELETE = '/system/todo/delete',
  /** 标记通知已读（复用通知模块接口） */
  NOTICE_SET_READ = '/system/notice/setRead',
}

/** 工作台汇总 */
export const getDashboardSummaryApi = async (): Promise<DashboardSummary | null> => {
  const res = await getAction<DashboardSummary>(IndexApiUrl.DASHBOARD_SUMMARY);
  return res.code === 200 ? res.data : null;
};

/** 新增待办 */
export const addTodoApi = async (params: TodoSaveParams): Promise<boolean> => {
  const res = await postAction<string>(IndexApiUrl.TODO_ADD, params);
  return res.code === 200;
};

/** 编辑待办 */
export const editTodoApi = async (params: TodoSaveParams): Promise<boolean> => {
  const res = await postAction<string>(IndexApiUrl.TODO_EDIT, params);
  return res.code === 200;
};

/** 完成待办 */
export const finishTodoApi = async (id: string): Promise<boolean> => {
  const res = await postAction<string>(IndexApiUrl.TODO_FINISH, { id });
  return res.code === 200;
};

/** 删除待办 */
export const deleteTodoApi = async (id: string): Promise<boolean> => {
  const res = await postAction<string>(IndexApiUrl.TODO_DELETE, { id });
  return res.code === 200;
};

/** 标记通知已读（noticeStatusId 为 sys_notice_status 行 id） */
export const setNoticeReadApi = async (noticeStatusId: string): Promise<boolean> => {
  const res = await postAction<string>(IndexApiUrl.NOTICE_SET_READ, { noticeStatusId });
  return res.code === 200;
};

export type { TodoItem };
