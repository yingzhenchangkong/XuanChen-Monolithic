import { getAction, postAction } from '@/utils/httpAction';

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

export const setReadApi = async (noticeStatusId: string) => {
  return await postAction(NoticeApiUrl.SET_READ, { noticeStatusId });
}

export const setReadBatchApi = async (ids: string[]) => {
  return await postAction(NoticeApiUrl.SET_READ_BATCH, { ids });
}

export const setReadAllApi = async () => {
  return await postAction(NoticeApiUrl.SET_READ_ALL, {});
}

export const issueApi = async (data: any) => {
  return await postAction(NoticeApiUrl.ISSUE, data);
}

export const cancelApi = async (id: string) => {
  return await postAction(NoticeApiUrl.CANCEL, { id });
}

export const recoverApi = async (id: string) => {
  return await postAction(NoticeApiUrl.RECOVER, { id });
}