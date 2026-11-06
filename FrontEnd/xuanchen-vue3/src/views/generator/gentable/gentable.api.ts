import { getAction, httpAction, postAction, exportAction, deleteAction } from '@/utils/httpAction';
import { message } from 'ant-design-vue';
import type { Result, PageResult } from '@/types/api';
import type {
  GenTable, GenTableColumn, GenTableSub, DBTable, DirListing, GenPreviewFile, GenGenerateLog,
} from './gentable.types';

// 项目预设的接口归 profile 模块维护，这里只做转出，避免两处各写一套 URL
export {
  getProfileSelect as getProjectProfiles,
  saveOrUpdate as saveProjectProfile,
  deleteProfile as deleteProjectProfile,
} from '../profile/profile.api';

enum GenTableApiUrl {
  INDEX_LIST = '/tool/generator/table/list',
  INDEX_DELETE = '/tool/generator/table/delete',
  INDEX_DELETE_BATCH = '/tool/generator/table/deleteBatch',

  OPERATION_ADD = '/tool/generator/table/add',
  OPERATION_EDIT = '/tool/generator/table/edit',
  SAVE_WITH_COLUMNS = '/tool/generator/table/saveWithColumns',

  GENERATOR = '/codeGenerator/generator',
  DOWNLOAD_ZIP = '/codeGenerator/downloadZip',
  PREVIEW = '/codeGenerator/preview',

  LOG_LIST = '/tool/generator/log/list',
  LOG_CLEAR = '/tool/generator/log/clear',

  RECBIN_LIST = '/tool/generator/table/listRecycleBin',
  RECBIN_DELETE = '/tool/generator/table/deleteRecycleBin',
  RECBIN_DELETE_BATCH = '/tool/generator/table/deleteRecycleBinBatch',
  RECBIN_REVERT = '/tool/generator/table/revertRecycleBin',
  RECBIN_REVERT_BATCH = '/tool/generator/table/revertRecycleBinBatch',

  SAVE_TABLE_COLUMN = '/tool/generator/table/saveTableColumn',
  LIST_TABLE_COLUMN_BY_ID = '/tool/generator/table/listTableColumnByTableId',
  LIST_SUB_TABLE = '/tool/generator/table/listSubTable',

  LIST_DB_TABLE = '/tool/generator/table/listDBTable',
  LIST_DB_TABLE_COLUMN = '/tool/generator/table/listDBTableColumn',
  LIST_TABLE_COLUMN = '/tool/generator/table/listTableColumn',

  DIR_LIST = '/tool/generator/directory/list',
  DIR_MKDIR = '/tool/generator/directory/mkdir',
}

export { GenTableApiUrl };

export const saveOrUpdate = (data: GenTable): Promise<Result<unknown>> => {
  const httpUrl = data.id ? GenTableApiUrl.OPERATION_EDIT : GenTableApiUrl.OPERATION_ADD;
  const method = data.id ? 'put' : 'post';
  return httpAction(httpUrl, data, method);
};

/**
 * 表配置 + 主表字段 + 多子表（含各自字段）一次性保存（后端同一事务）。
 * 取代「先 add/edit 再 saveTableColumn」的两次请求，避免第二步失败留下半成品。
 */
export const saveWithColumns = (
  table: GenTable,
  columns: GenTableColumn[],
  subTables: GenTableSub[] | null,
): Promise<Result<string>> => {
  return postAction<string>(GenTableApiUrl.SAVE_WITH_COLUMNS, { table, columns, subTables });
};

/** 按表配置 id 查询已配置的子表（含各子表字段），编辑回显用 */
export const getListSubTable = async (tableId: string): Promise<GenTableSub[]> => {
  try {
    const res = await getAction<GenTableSub[]>(GenTableApiUrl.LIST_SUB_TABLE, { tableId });
    if (res.code === 200) {
      return res.data || [];
    }
    return [];
  } catch (error) {
    console.error('获取子表配置失败:', error);
    return [];
  }
};

/** 按已保存的表配置 id 触发代码生成，返回本次实际写出的文件（相对输出目录） */
export const generator = (id: string): Promise<Result<string[]>> => {
  return getAction(GenTableApiUrl.GENERATOR, { id });
};

/** 生成预览：临时目录生成并返回文件内容，不写入配置的输出目录 */
export const previewCode = async (id: string): Promise<GenPreviewFile[]> => {
  try {
    const res = await getAction<GenPreviewFile[]>(GenTableApiUrl.PREVIEW, { id });
    if (res.code === 200) {
      return res.data || [];
    }
    message.error(res.msg || '生成预览失败');
    return [];
  } catch {
    return [];
  }
};

/** 全量覆盖保存表字段及界面配置（tableId + columns，兼容旧入口） */
export const saveTableColumn = (data: { tableId: string; columns: GenTableColumn[] }): Promise<Result<unknown>> => {
  return postAction(GenTableApiUrl.SAVE_TABLE_COLUMN, data);
};

/** 按表配置 id 查询已保存的字段及界面配置（编辑回显，含主表与子表） */
export const getListTableColumnByTableId = async (id: string): Promise<GenTableColumn[]> => {
  try {
    const res = await getAction<GenTableColumn[]>(GenTableApiUrl.LIST_TABLE_COLUMN_BY_ID, { id });
    if (res.code === 200) {
      return res.data || [];
    }
    return [];
  } catch (error) {
    console.error('获取字段配置失败:', error);
    return [];
  }
};

/**
 * 生成代码并打包 zip 下载（产物含 menu.sql 菜单权限 SQL）。
 * 生成在服务器临时目录完成，不占用输出目录白名单。
 */
export const downloadZip = async (id: string, tableName: string): Promise<void> => {
  try {
    const res = await exportAction(GenTableApiUrl.DOWNLOAD_ZIP, { id });
    const data: Blob = res instanceof Blob ? res : new Blob([res]);
    // 拦截后端 JSON 错误体（HTTP 200 但业务失败时不落损坏文件）
    if (/json/i.test(data.type || '')) {
      try {
        const errBody = JSON.parse(await data.text()) as Result;
        message.error(errBody.msg || '下载失败!');
      } catch {
        message.error('下载失败!');
      }
      return;
    }
    const blob = new Blob([data], { type: 'application/zip' });
    const link = document.createElement('a');
    link.style.display = 'none';
    link.href = URL.createObjectURL(blob);
    link.setAttribute('download', (tableName || 'code') + '-code.zip');
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(link.href);
  } catch (error) {
    console.warn('【gentable】打包下载失败:', error);
  }
};

/** 生成记录（按表配置分页查询） */
export const getGenerateLogs = async (
  tableId: string,
  pageNo = 1,
  pageSize = 20,
): Promise<PageResult<GenGenerateLog>> => {
  const empty: PageResult<GenGenerateLog> = {
    records: [], total: 0, current: pageNo, size: pageSize,
  } as PageResult<GenGenerateLog>;
  try {
    const res = await getAction<PageResult<GenGenerateLog>>(GenTableApiUrl.LOG_LIST, { tableId, pageNo, pageSize });
    return res.code === 200 && res.data ? res.data : empty;
  } catch {
    return empty;
  }
};

/** 清空某表配置的生成记录 */
export const clearGenerateLogs = (tableId: string): Promise<Result<unknown>> => {
  return deleteAction(GenTableApiUrl.LOG_CLEAR, { tableId });
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

/**
 * 目录选择器：列出目录内容（path 为空只返回可跳转的根目录）。
 * 目录在服务端浏览，前端只展示可选路径。
 */
export const getDirListing = async (path?: string): Promise<DirListing | undefined> => {
  try {
    const res = await getAction<DirListing>(GenTableApiUrl.DIR_LIST, path ? { path } : {});
    if (res.code === 200) {
      return res.data;
    }
    message.error(res.msg || '读取目录失败');
    return undefined;
  } catch (error) {
    console.warn('【gentable】读取目录失败:', error);
    return undefined;
  }
};

/** 在指定目录下新建文件夹，返回新目录绝对路径 */
export const createDir = async (parent: string, name: string): Promise<string | undefined> => {
  const res = await postAction(GenTableApiUrl.DIR_MKDIR, { parent, name });
  if (res.code !== 200) {
    message.error(res.msg || '新建文件夹失败');
    return undefined;
  }
  return typeof res.data === 'string' ? res.data : undefined;
};
