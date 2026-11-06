import { getAction, postAction, deleteAction, postFormAction } from '@/utils/httpAction';
import type { Result } from '@/types/api';
import type { OnlineFormRecord } from './form.types';

enum FormApiUrl {
  INDEX_LIST = '/online/form/list',
  INDEX_DELETE = '/online/form/delete',
  OPERATION_OPTIONS = '/online/form/options',
  OPERATION_DETAIL = '/online/form/detail',
  OPERATION_SAVE = '/online/form/save',
}

export { FormApiUrl };

export const getFormListApi = (params: Record<string, unknown>): Promise<Result<{ records: OnlineFormRecord[]; total: number }>> =>
  getAction(FormApiUrl.INDEX_LIST, params);

/** 表单下拉选项（流程设计绑定用，不含 schema） */
export const getFormOptionsApi = (): Promise<Result<OnlineFormRecord[]>> =>
  getAction(FormApiUrl.OPERATION_OPTIONS);

export const getFormDetailApi = (id: string): Promise<Result<OnlineFormRecord>> =>
  getAction(FormApiUrl.OPERATION_DETAIL, { id });

/** 保存表单（FormData：id 可空、formName、remark、schemaJson） */
export const saveFormApi = (
  id: string | undefined,
  formName: string,
  remark: string,
  schemaJson: string,
): Promise<Result<OnlineFormRecord>> => {
  const formData = new FormData();
  if (id) formData.append('id', id);
  formData.append('formName', formName);
  formData.append('remark', remark);
  formData.append('schemaJson', schemaJson);
  return postFormAction<OnlineFormRecord>(FormApiUrl.OPERATION_SAVE, formData);
};

export const deleteFormApi = (id: string): Promise<Result<null>> =>
  deleteAction(FormApiUrl.INDEX_DELETE, { id });
