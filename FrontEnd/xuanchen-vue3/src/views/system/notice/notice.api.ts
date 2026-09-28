import { postAction } from '@/utils/httpAction';
import type { Result } from '@/types/api';
import type { NoticeSaveParams } from './notice.types';

enum NoticeApiUrl {
  SET_READ = '/system/notice/setRead',
  LIST_USER = '/system/notice/listUser',
  SET_READ_BATCH = '/system/notice/setReadBatch',
  SET_READ_ALL = '/system/notice/setReadAll',
  ISSUE = '/system/notice/issue',
  LIST_MANAGE = '/system/notice/listManage',
  CANCEL = '/system/notice/cancel',
  RECOVER = '/system/notice/recover',
  LIST_MANAGE_STATUS = '/system/notice/listManageStatus',
}

export { NoticeApiUrl };

export const setReadApi = (noticeStatusId: string): Promise<Result<null>> => {
  return postAction<null>(NoticeApiUrl.SET_READ, { noticeStatusId });
}

export const setReadBatchApi = (ids: (string | number)[]): Promise<Result<null>> => {
  return postAction<null>(NoticeApiUrl.SET_READ_BATCH, { ids });
}

export const setReadAllApi = (): Promise<Result<null>> => {
  return postAction<null>(NoticeApiUrl.SET_READ_ALL, {});
}

export const issueApi = (data: NoticeSaveParams): Promise<Result<null>> => {
  return postAction<null>(NoticeApiUrl.ISSUE, data);
}

export const cancelApi = (id: string): Promise<Result<null>> => {
  return postAction<null>(NoticeApiUrl.CANCEL, { id });
}

export const recoverApi = (id: string): Promise<Result<null>> => {
  return postAction<null>(NoticeApiUrl.RECOVER, { id });
}
