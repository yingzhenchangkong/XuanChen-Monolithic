import { getAction, postAction, deleteAction } from '@/utils/httpAction';
import type { Result } from '@/types/api';
import type {
  VisualizationRecord, ScreenDataset, ScreenDatasource, ScreenDesignConfig, ScreenViewConfig,
} from './visualization.types';

enum VisualizationApiUrl {
  INDEX_LIST = '/visualization/list',
  INDEX_DELETE = '/visualization/delete',
  INDEX_DELETE_BATCH = '/visualization/deleteBatch',
  OPERATION_ADD = '/visualization/add',
  OPERATION_EDIT = '/visualization/edit',
  COPY = '/visualization/copy',
  PUBLISH = '/visualization/publish',
  UNPUBLISH = '/visualization/unpublish',
  /** 设计器加载：大屏信息 + 配置 JSON + 数据集 */
  CONFIG = '/visualization/config',
  /** 保存画布/组件配置 */
  SAVE_CONFIG = '/visualization/saveConfig',
  /** 展示页加载（按编码，含服务端取数结果） */
  VIEW_CONFIG = '/visualization/viewConfig',

  DATASET_LIST = '/visualization/dataset/list',
  DATASET_ADD = '/visualization/dataset/add',
  DATASET_EDIT = '/visualization/dataset/edit',
  DATASET_DELETE = '/visualization/dataset/delete',
  DATASET_PREVIEW = '/visualization/dataset/preview',
  /** 统一数据源下拉（system 模块提供，四模块共用） */
  DATASOURCE_LIST = '/system/datasource/select',
}

export { VisualizationApiUrl };

// ---------- 大屏 ----------

export const saveOrUpdateScreen = (data: VisualizationRecord): Promise<Result<null>> => {
  const url = data.id ? VisualizationApiUrl.OPERATION_EDIT : VisualizationApiUrl.OPERATION_ADD;
  return postAction<null>(url, data);
};

export const copyScreenApi = (id: string, screenCode: string, screenName: string): Promise<Result<null>> =>
  postAction<null>(VisualizationApiUrl.COPY, { id, screenCode, screenName });

export const publishScreenApi = (id: string): Promise<Result<null>> =>
  postAction<null>(VisualizationApiUrl.PUBLISH, { id });

export const unpublishScreenApi = (id: string): Promise<Result<null>> =>
  postAction<null>(VisualizationApiUrl.UNPUBLISH, { id });

/** 设计器加载（配置为空时返回大屏基本信息，前端补默认画布） */
export const getScreenConfigApi = async (id: string): Promise<ScreenDesignConfig> => {
  const res = await getAction<ScreenDesignConfig>(VisualizationApiUrl.CONFIG, { id });
  if (res.code !== 200) {
    return Promise.reject(res.msg || '加载大屏失败');
  }
  return res.data;
};

export const saveScreenConfigApi = (id: string, configJson: string): Promise<Result<null>> =>
  postAction<null>(VisualizationApiUrl.SAVE_CONFIG, { id, configJson });

/** 展示页加载（按编码，仅已发布；含各数据集取数结果） */
export const getScreenViewConfigApi = async (code: string): Promise<ScreenViewConfig> => {
  const res = await getAction<ScreenViewConfig>(VisualizationApiUrl.VIEW_CONFIG, { code });
  if (res.code !== 200) {
    return Promise.reject(res.msg || '加载大屏失败');
  }
  return res.data;
};

// ---------- 数据集 ----------

export const getScreenDatasetListApi = async (screenId: string): Promise<ScreenDataset[]> => {
  const res = await getAction<ScreenDataset[]>(VisualizationApiUrl.DATASET_LIST, { screenId });
  if (res.code !== 200) {
    return Promise.reject(res.msg || '加载数据集失败');
  }
  return res.data ?? [];
};

export const saveOrUpdateScreenDataset = (data: ScreenDataset): Promise<Result<null>> => {
  const url = data.id ? VisualizationApiUrl.DATASET_EDIT : VisualizationApiUrl.DATASET_ADD;
  return postAction<null>(url, data);
};

export const deleteScreenDatasetApi = (id: string): Promise<Result<null>> =>
  deleteAction<null>(VisualizationApiUrl.DATASET_DELETE, { id });

/** 试执行：传 sql + dsId（未保存内容）或 datasetId（已保存数据集） */
export const previewScreenDatasetApi = (payload: { sql?: string; dsId?: string | null; datasetId?: string }): Promise<Result<{
  columns: string[];
  rows: Record<string, unknown>[];
  total: number;
}>> => postAction(VisualizationApiUrl.DATASET_PREVIEW, payload);

/** 外部数据源列表 */
export const getScreenDatasourceListApi = (): Promise<Result<ScreenDatasource[]>> =>
  getAction(VisualizationApiUrl.DATASOURCE_LIST);
