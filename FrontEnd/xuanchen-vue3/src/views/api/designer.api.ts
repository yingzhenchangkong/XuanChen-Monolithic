import { getAction, deleteAction, postFormAction } from '@/utils/httpAction';
import type { Result, PageResult } from '@/types/api';

/** 接口定义记录（与后端 Api 对应） */
export interface ApiRecord {
  id: string;
  apiCode: string;
  apiName: string;
  apiKey?: string;
  /** 0草稿 1已发布 2已停用 */
  status: number;
  /** 步骤链配置 JSON 字符串 */
  steps?: string;
  remark?: string;
  createBy?: string;
  createTime?: string;
  updateBy?: string;
  updateTime?: string;
}

/** 步骤定义（前端编辑态） */
export interface StepDefItem {
  type: 'sql' | 'http' | 'map' | 'save';
  name: string;
  config: Record<string, unknown>;
}

/** 采集数据记录 */
export interface ApiDataRecord {
  id: string;
  apiCode: string;
  data?: string;
  createTime?: string;
}

/** 定时采集配置记录 */
export interface ApiScheduleRecord {
  id: string;
  apiCode: string;
  cron: string;
  enabled: number;
  lastRunTime?: string;
  nextRunTime?: string;
  remark?: string;
}

/** 外部数据源记录（供 sql 步骤下拉选择） */
export interface ApiDatasourceRecord {
  id: string;
  dsName: string;
  dsType?: string;
  status?: number;
}

enum ApiUrl {
  LIST = '/api/list',
  DETAIL = '/api/detail',
  SAVE = '/api/save',
  DELETE = '/api/delete',
  PUBLISH = '/api/publish',
  STOP = '/api/stop',
  TEST = '/api/test',
  /** 统一数据源下拉（system 模块提供，四模块共用） */
  DATASOURCE_LIST = '/system/datasource/select',
  SCHEDULE_LIST = '/api/schedule/list',
  SCHEDULE_SAVE = '/api/schedule/save',
  SCHEDULE_DELETE = '/api/schedule/delete',
  DATA_LIST = '/api/data/list',
  DATA_DELETE = '/api/data/delete',
}

/** 接口分页列表 */
export const getApiListUrl = ApiUrl.LIST;

/** 接口详情 */
export const getApiDetailApi = (id: string): Promise<Result<ApiRecord>> =>
  getAction<ApiRecord>(ApiUrl.DETAIL, { id });

/** 保存接口（FormData：id 可空、apiCode、apiName、apiKey、steps JSON、remark） */
export const saveApiApi = (
  id: string | undefined,
  apiCode: string,
  apiName: string,
  apiKey: string,
  steps: string,
  remark: string,
): Promise<Result<ApiRecord>> => {
  const formData = new FormData();
  if (id) formData.append('id', id);
  formData.append('apiCode', apiCode);
  formData.append('apiName', apiName);
  formData.append('apiKey', apiKey);
  formData.append('steps', steps);
  formData.append('remark', remark);
  return postFormAction<ApiRecord>(ApiUrl.SAVE, formData);
};

/** 删除接口 */
export const deleteApiApi = (id: string): Promise<Result<null>> =>
  deleteAction<null>(ApiUrl.DELETE, { id });

/** 发布接口 */
export const publishApiApi = (id: string): Promise<Result<null>> =>
  postFormAction<null>(`${ApiUrl.PUBLISH}?id=${encodeURIComponent(id)}`, new FormData());

/** 停用接口 */
export const stopApiApi = (id: string): Promise<Result<null>> =>
  postFormAction<null>(`${ApiUrl.STOP}?id=${encodeURIComponent(id)}`, new FormData());

/** 测试运行（paramsJson 为入参 JSON 字符串） */
export const testApiApi = (id: string, paramsJson: string): Promise<Result<{ data: unknown; costMs: number }>> => {
  const formData = new FormData();
  formData.append('id', id);
  formData.append('params', paramsJson);
  return postFormAction<{ data: unknown; costMs: number }>(ApiUrl.TEST, formData);
};

/** 定时配置列表 */
export const getScheduleListApi = (apiCode: string): Promise<Result<PageResult<ApiScheduleRecord>>> =>
  getAction<PageResult<ApiScheduleRecord>>(ApiUrl.SCHEDULE_LIST, { apiCode, pageNo: 1, pageSize: 50 });

/** 保存定时配置 */
export const saveScheduleApi = (
  apiCode: string,
  cron: string,
  enabled: boolean,
  remark: string,
): Promise<Result<ApiScheduleRecord>> => {
  const formData = new FormData();
  formData.append('apiCode', apiCode);
  formData.append('cron', cron);
  formData.append('enabled', enabled ? '1' : '0');
  formData.append('remark', remark);
  return postFormAction<ApiScheduleRecord>(ApiUrl.SCHEDULE_SAVE, formData);
};

/** 删除定时配置 */
export const deleteScheduleApi = (id: string): Promise<Result<null>> =>
  deleteAction<null>(ApiUrl.SCHEDULE_DELETE, { id });

/** 采集数据分页列表（供 XCListPage 使用） */
export const getDataListUrl = ApiUrl.DATA_LIST;

/** 删除采集数据 */
export const deleteDataApi = (id: string): Promise<Result<null>> =>
  deleteAction<null>(ApiUrl.DATA_DELETE, { id });

/** 外部数据源列表（sql 步骤下拉） */
export const getDatasourceListApi = (): Promise<Result<ApiDatasourceRecord[]>> =>
  getAction<ApiDatasourceRecord[]>(ApiUrl.DATASOURCE_LIST);
