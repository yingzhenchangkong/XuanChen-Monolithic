import { getAction, postAction } from '@/utils/httpAction';

enum NoticeApiUrl {
  SET_READ = '/system/notice/setRead',
  LIST_USER = '/system/notice/listUser',
  SET_READ_BATCH = '/system/notice/setReadBatch',
  SET_READ_ALL = '/system/notice/setReadAll',
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