import { postAction } from '@/utils/httpAction';
import type { Result } from '@/types/api';
import type { NoticeSaveParams } from './notice.types';

enum NoticeApiUrl {
  // 我的通知（用户侧列表页 NoticeList）
  INDEX_USER_LIST = '/system/notice/listUser',

  // 管理员通知列表（ManageList）
  INDEX_MANAGE_LIST = '/system/notice/listManage',
  // 管理员查看已读状态明细（ManageList 下的子表 ManageStatus）
  INDEX_MANAGE_STATUS = '/system/notice/listManageStatus',

  // 用户侧列表页操作
  OPERATION_SET_READ = '/system/notice/setRead',
  OPERATION_SET_READ_BATCH = '/system/notice/setReadBatch',
  OPERATION_SET_READ_ALL = '/system/notice/setReadAll',

  // 管理员发布页操作
  OPERATION_ISSUE = '/system/notice/issue',
  OPERATION_CANCEL = '/system/notice/cancel',
  OPERATION_RECOVER = '/system/notice/recover',
}

export { NoticeApiUrl };

export const setReadApi = (noticeStatusId: string): Promise<Result<null>> => {
  return postAction<null>(NoticeApiUrl.OPERATION_SET_READ, { noticeStatusId });
}

export const setReadBatchApi = (ids: (string | number)[]): Promise<Result<null>> => {
  return postAction<null>(NoticeApiUrl.OPERATION_SET_READ_BATCH, { ids });
}

export const setReadAllApi = (): Promise<Result<null>> => {
  return postAction<null>(NoticeApiUrl.OPERATION_SET_READ_ALL, {});
}

export const issueApi = (data: NoticeSaveParams): Promise<Result<null>> => {
  return postAction<null>(NoticeApiUrl.OPERATION_ISSUE, data);
}

export const cancelApi = (id: string): Promise<Result<null>> => {
  return postAction<null>(NoticeApiUrl.OPERATION_CANCEL, { id });
}

export const recoverApi = (id: string): Promise<Result<null>> => {
  return postAction<null>(NoticeApiUrl.OPERATION_RECOVER, { id });
}
