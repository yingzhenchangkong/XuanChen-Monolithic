import { getAction, postAction, deleteAction } from '@/utils/httpAction';
import type { Result } from '@/types/api';
import type {
  ReportRecord, DatasetRecord, DatasourceRecord, DatasetResult, ViewConfigData,
} from './report.types';

enum ReportApiUrl {
  INDEX_LIST = '/report/list',
  INDEX_DELETE = '/report/delete',
  INDEX_DELETE_BATCH = '/report/deleteBatch',
  OPERATION_ADD = '/report/add',
  OPERATION_EDIT = '/report/edit',
  COPY = '/report/copy',
  /** 查看页加载配置（按报表编码） */
  VIEW_CONFIG = '/report/viewConfig',

  DATASET_LIST = '/report/dataset/list',
  DATASET_ADD = '/report/dataset/add',
  DATASET_EDIT = '/report/dataset/edit',
  DATASET_DELETE = '/report/dataset/delete',
  /** 设计器试执行（按数据集 id） */
  DATASET_EXECUTE = '/report/dataset/execute',
  /** 查看页取数（报表编码 + 数据集编码） */
  DATASET_DATA = '/report/dataset/data',

  /** 数据源下拉：统一走系统管理的只读端点（只返回 id/名称/类型，不含连接串与口令） */
  DS_LIST = '/system/datasource/select',
}

export { ReportApiUrl };

// ---------- 报表 ----------

export const saveOrUpdateReport = (data: ReportRecord): Promise<Result<null>> => {
  const url = data.id ? ReportApiUrl.OPERATION_EDIT : ReportApiUrl.OPERATION_ADD;
  return postAction<null>(url, data);
};

export const copyReportApi = (id: string, reportCode: string, reportName: string): Promise<Result<null>> => {
  return postAction<null>(ReportApiUrl.COPY, { id, reportCode, reportName });
};

/** 查看页加载（按报表编码） */
export const getViewConfigApi = async (code: string): Promise<ViewConfigData> => {
  const res = await getAction<ViewConfigData>(ReportApiUrl.VIEW_CONFIG, { code });
  if (res.code !== 200) {
    return Promise.reject(res.msg || '加载报表失败');
  }
  return res.data;
};

// ---------- 数据集 ----------

export const getDatasetListApi = async (templateId: string): Promise<DatasetRecord[]> => {
  const res = await getAction<DatasetRecord[]>(ReportApiUrl.DATASET_LIST, { templateId });
  if (res.code !== 200) {
    return Promise.reject(res.msg || '加载数据集失败');
  }
  return res.data ?? [];
};

export const saveOrUpdateDataset = (data: DatasetRecord): Promise<Result<null>> => {
  const url = data.id ? ReportApiUrl.DATASET_EDIT : ReportApiUrl.DATASET_ADD;
  return postAction<null>(url, data);
};

export const deleteDatasetApi = (id: string): Promise<Result<null>> => deleteAction<null>(ReportApiUrl.DATASET_DELETE, { id });

/** 设计器试执行 */
export const executeDatasetApi = (datasetId: string, params: Record<string, unknown> = {}): Promise<Result<DatasetResult>> =>
  postAction<DatasetResult>(ReportApiUrl.DATASET_EXECUTE, { datasetId, params });

/** 查看页取数 */
export const fetchDatasetDataApi = (
  reportCode: string,
  datasetCode: string,
  params: Record<string, unknown> = {}
): Promise<Result<DatasetResult>> => postAction<DatasetResult>(ReportApiUrl.DATASET_DATA, { reportCode, datasetCode, params });

// ---------- 数据源 ----------

export const getDatasourceListApi = async (): Promise<DatasourceRecord[]> => {
  const res = await getAction<DatasourceRecord[]>(ReportApiUrl.DS_LIST);
  if (res.code !== 200) {
    return Promise.reject(res.msg || '加载数据源失败');
  }
  return res.data ?? [];
};
