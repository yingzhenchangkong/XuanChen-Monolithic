import { deleteAction, getAction, postAction } from '@/utils/httpAction';
import type { Result } from '@/types/api';

/** 统一数据源（sys_datasource） API */
enum SysDatasourceApiUrl {
  LIST = '/system/datasource/list',
  ADD = '/system/datasource/add',
  EDIT = '/system/datasource/edit',
  DELETE = '/system/datasource/delete',
  TEST_CONNECTION = '/system/datasource/testConnection',
}

export interface SysDatasourceModel {
  id?: string;
  dsName?: string;         // 数据源名称
  dsType?: string;         // 数据源类型（mysql/oracle/sqlserver/postgresql/dm/kingbase/h2）
  jdbcUrl?: string;        // JDBC 连接串
  username?: string;       // 用户名
  password?: string;       // 密码（新增/修改时传明文；回显时为空，hasPassword=true 表示已配置）
  hasPassword?: boolean;   // 列表返回：是否已配置密码
  status?: number;         // 状态（1启用 0停用）
  createBy?: string;
  createTime?: string;
  updateBy?: string;
  updateTime?: string;
}

export const getSysDatasourceListApi = (): Promise<Result<SysDatasourceModel[]>> =>
  getAction<SysDatasourceModel[]>(SysDatasourceApiUrl.LIST, {});

export const saveSysDatasourceApi = (model: SysDatasourceModel): Promise<Result<string>> =>
  postAction<string>(model.id ? SysDatasourceApiUrl.EDIT : SysDatasourceApiUrl.ADD, model);

export const deleteSysDatasourceApi = (id: string): Promise<Result<string>> =>
  deleteAction<string>(SysDatasourceApiUrl.DELETE, { id });

export const testSysDatasourceConnectionApi = (model: SysDatasourceModel): Promise<Result<string>> =>
  postAction<string>(SysDatasourceApiUrl.TEST_CONNECTION, model);
