import { getAction, httpAction } from '@/utils/httpAction';
import type { Result } from '@/types/api';
import type { GenTable, GenTableColumn, DBTable } from './gentable.types';

enum GenTableApiUrl {
  INDEX_LIST = '/tool/generator/table/list',
  INDEX_DELETE = '/tool/generator/table/delete',
  INDEX_DELETE_BATCH = '/tool/generator/table/deleteBatch',

  OPERATION_ADD = '/tool/generator/table/add',
  OPERATION_EDIT = '/tool/generator/table/edit',

  GENERATOR = '/codeGenerator/generator',

  LIST_DB_TABLE = '/tool/generator/table/listDBTable',
  LIST_DB_TABLE_COLUMN = '/tool/generator/table/listDBTableColumn',
  LIST_TABLE_COLUMN = '/tool/generator/table/listTableColumn',
}

export { GenTableApiUrl };

export const saveOrUpdate = (data: GenTable): Promise<Result<unknown>> => {
  const httpUrl = data.id ? GenTableApiUrl.OPERATION_EDIT : GenTableApiUrl.OPERATION_ADD;
  const method = data.id ? 'put' : 'post';
  return httpAction(httpUrl, data, method);
};

/** 按已保存的表配置 id 触发代码生成（数据源与输出配置均由后端按 id 组装） */
export const generator = (id: string): Promise<Result<unknown>> => {
  return getAction(GenTableApiUrl.GENERATOR, { id });
};

/**
 * 查询已保存数据源下的物理表列表。
 * 仅传数据源 id：主机/端口/库名/密码全部由后端按 id 从库内读取，
 * 前端不再回传任何连接参数（防 SSRF 与 JDBC 属性注入）
 */
export const getListDBTable = async (databaseId: string): Promise<DBTable[]> => {
  try {
    const res = await getAction<DBTable[]>(GenTableApiUrl.LIST_DB_TABLE, { id: databaseId });
    if (res.code === 200) {
      return res.data || [];
    }
    return [];
  } catch (error) {
    console.error('获取列表失败:', error);
    return [];
  }
}

/** 查询已保存数据源中某物理表的字段列表（库中实时读取，仅传 id + 表名） */
export const getListDBTableColumn = async (databaseId: string, tableName: string): Promise<GenTableColumn[]> => {
  try {
    const res = await getAction<GenTableColumn[]>(GenTableApiUrl.LIST_DB_TABLE_COLUMN, { id: databaseId, tableName });
    if (res.code === 200) {
      return res.data || [];
    }
    return [];
  } catch (error) {
    console.error('获取列表失败:', error);
    return [];
  }
}

/** 查询已保存配置的表字段列表 */
export const getListTableColumn = async (dataBaseId: string, tableName: string): Promise<GenTableColumn[]> => {
  try {
    const res = await getAction<GenTableColumn[]>(GenTableApiUrl.LIST_TABLE_COLUMN, { dataBaseId, tableName });
    if (res.code === 200) {
      return res.data || [];
    }
    return [];
  } catch (error) {
    console.error('获取列表失败:', error);
    return [];
  }
}
