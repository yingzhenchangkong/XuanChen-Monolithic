import { getAction, postAction, httpAction } from '@/utils/httpAction';
import type { Result, SelectOption } from '@/types/api';
import type { GenDatabase } from './gendatabase.types';

enum GenDatabaseApiUrl {
  INDEX_LIST = '/tool/generator/database/list',
  INDEX_DELETE = '/tool/generator/database/delete',
  INDEX_DELETE_BATCH = '/tool/generator/database/deleteBatch',
  INDEX_EXPORT_EXCEL = '/tool/generator/database/exportExcel',
  INDEX_IMPORT_EXCEL = '/tool/generator/database/importExcel',
  INDEX_CHANGE_STATUS = '/tool/generator/database/changeStatus',

  OPERATION_ADD = '/tool/generator/database/add',
  OPERATION_EDIT = '/tool/generator/database/edit',

  RECBIN_LIST = '/tool/generator/database/listRecycleBin',
  RECBIN_DELETE = '/tool/generator/database/deleteRecycleBin',
  RECBIN_DELETE_BATCH = '/tool/generator/database/deleteRecycleBinBatch',
  RECBIN_REVERT = '/tool/generator/database/revertRecycleBin',
  RECBIN_REVERT_BATCH = '/tool/generator/database/revertRecycleBinBatch',

  SELECT = '/tool/generator/database/select',
  GET_ONE_BY_ID = '/tool/generator/database/getOneById',
}

export { GenDatabaseApiUrl };

export const changeStatusApi = (id: string, status: number): Promise<Result<null>> => {
  return postAction<null>(GenDatabaseApiUrl.INDEX_CHANGE_STATUS, { id, status });
};

export const saveOrUpdate = (data: GenDatabase): Promise<Result<unknown>> => {
  const httpUrl = data.id ? GenDatabaseApiUrl.OPERATION_EDIT : GenDatabaseApiUrl.OPERATION_ADD;
  const method = data.id ? 'put' : 'post';
  return httpAction(httpUrl, data, method);
};

/** 数据源下拉选项（value 取 id，label 取 connName） */
export const getGenDatabaseSelect = (): Promise<Result<SelectOption[]>> => {
  return getAction<SelectOption[]>(GenDatabaseApiUrl.SELECT, {});
};

export const getOneById = (id: string): Promise<Result<GenDatabase>> => {
  return getAction<GenDatabase>(GenDatabaseApiUrl.GET_ONE_BY_ID, { id });
};
