import { postAction, httpAction } from '@/utils/httpAction';
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
}

export { GenDatabaseApiUrl };

export const changeStatusApi = async (id: string, status: number) => {
  return await postAction(GenDatabaseApiUrl.INDEX_CHANGE_STATUS, { id, status });
}

export const saveOrUpdate = async (data: GenDatabase) => {
  const httpUrl = data.id ? GenDatabaseApiUrl.OPERATION_EDIT : GenDatabaseApiUrl.OPERATION_ADD;
  const method = data.id ? 'put' : 'post';
  return await httpAction(httpUrl, data, method);
};