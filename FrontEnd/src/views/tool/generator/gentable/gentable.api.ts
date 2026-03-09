import { getAction, httpAction } from '@/utils/httpAction';
import type { GenTable, GenDatabase } from './gentable.types';

enum GenTableApiUrl {
  INDEX_LIST = '/tool/generator/table/list',
  INDEX_LIST_BY_TABLE_ID = '/tool/generator/table/listByTableId',
  INDEX_DELETE = '/tool/generator/table/delete',
  INDEX_DELETE_BATCH = '/tool/generator/table/deleteBatch',

  OPERATION_ADD = '/tool/generator/table/add',
  OPERATION_EDIT = '/tool/generator/table/edit',

  GENERATOR = '/codeGenerator',

  TABLE_LIST = '/tool/generator/table/getTableList',
  TABLE_INFO = '/tool/generator/table/getTableInfo',
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

export const getListByTableId = async (tableId: string) => {
  try {
    const res: any = await getAction(GenTableApiUrl.INDEX_LIST_BY_TABLE_ID, { tableId });
    if (res.code === 200) {
      return res.data || [];
    }
    return [];
  } catch (error) {
    console.error('获取列表失败:', error);
    return [];
  }
}

export const getTableList = async (data: GenDatabase) => {
  try {
    const res: any = await getAction(GenTableApiUrl.TABLE_LIST, data);
    if (res.code === 200) {
      return res.data || [];
    }
    return [];
  } catch (error) {
    console.error('获取列表失败:', error);
    return [];
  }
}

export const getTableInfo = async (genDatabase: GenDatabase, tableName: string) => {
  try {
    const res: any = await getAction(GenTableApiUrl.TABLE_INFO, { ...genDatabase, tableName });
    if (res.code === 200) {
      return res.data || {};
    }
    return {};
  } catch (error) {
    console.error('获取列表失败:', error);
    return {};
  }
}