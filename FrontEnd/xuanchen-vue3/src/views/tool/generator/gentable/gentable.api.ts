import { getAction, httpAction } from '@/utils/httpAction';
import type { GenTable, GenDatabase } from './gentable.types';

enum GenTableApiUrl {
  INDEX_LIST = '/tool/generator/table/list',
  INDEX_DELETE = '/tool/generator/table/delete',
  INDEX_DELETE_BATCH = '/tool/generator/table/deleteBatch',

  OPERATION_ADD = '/tool/generator/table/add',
  OPERATION_EDIT = '/tool/generator/table/edit',

  GENERATOR = '/codeGenerator',

  LIST_DB_TABLE = '/tool/generator/table/listDBTable',
  LIST_DB_TABLE_COLUMN = '/tool/generator/table/listDBTableColumn',
  LIST_TABLE_COLUMN = '/tool/generator/table/listTableColumn',
}

export { GenTableApiUrl };

export const saveOrUpdate = async (data: GenTable) => {
  const httpUrl = data.id ? GenTableApiUrl.OPERATION_EDIT : GenTableApiUrl.OPERATION_ADD;
  const method = data.id ? 'put' : 'post';
  return await httpAction(httpUrl, data, method);
};

export const generator = async () => {
  return await getAction(GenTableApiUrl.GENERATOR, {});
};

export const getListDBTable = async (data: GenDatabase) => {
  try {
    const res: any = await getAction(GenTableApiUrl.LIST_DB_TABLE, data);
    if (res.code === 200) {
      return res.data || [];
    }
    return [];
  } catch (error) {
    console.error('获取列表失败:', error);
    return [];
  }
}

export const getListDBTableColumn = async (genDatabase: GenDatabase, tableName: string) => {
  try {
    const res: any = await getAction(GenTableApiUrl.LIST_DB_TABLE_COLUMN, { ...genDatabase, tableName });
    if (res.code === 200) {
      return res.data || {};
    }
    return {};
  } catch (error) {
    console.error('获取列表失败:', error);
    return {};
  }
}

export const getListTableColumn = async (dataBaseId: string, tableName: string) => {
  try {
    const res: any = await getAction(GenTableApiUrl.LIST_TABLE_COLUMN, { dataBaseId, tableName });
    if (res.code === 200) {
      return res.data || [];
    }
    return [];
  } catch (error) {
    console.error('获取列表失败:', error);
    return [];
  }
}