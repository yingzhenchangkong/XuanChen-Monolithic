/** 大屏记录（vis_screen） */
export interface VisualizationRecord {
  id?: string;
  screenCode?: string;
  screenName?: string;
  thumbnail?: string;
  status?: number;
  publishTime?: string;
  remark?: string;
  configJson?: string;
  createBy?: string;
  createName?: string;
  createTime?: string;
  updateBy?: string;
  updateName?: string;
  updateTime?: string;
  [key: string]: unknown;
}

/** 大屏数据集（vis_dataset） */
export interface ScreenDataset {
  id?: string;
  screenId?: string;
  datasetKey?: string;
  datasetName?: string;
  sqlText?: string;
  /** 外部数据源ID（空=主库） */
  dsId?: string | null;
  remark?: string;
  [key: string]: unknown;
}

/** 外部数据源记录（下拉用） */
export interface ScreenDatasource {
  id: string;
  dsName: string;
  dsType: string;
}

/** 画布配置 */
export interface ScreenCanvas {
  /** 设计分辨率宽度（等比缩放到容器） */
  width: number;
  /** 设计分辨率高度 */
  height: number;
  background: string;
  /** 画布背景图（相对路径或外链） */
  backgroundImage?: string;
  /** 缩放方式：fit=等比留黑边，full=拉伸铺满 */
  scaleMode: 'fit' | 'full';
  /** 设计器是否显示网格 */
  showGrid?: boolean;
}

/** 组件类型 */
export type WidgetType = 'text' | 'number' | 'bar' | 'line' | 'pie' | 'table' | 'image' | 'border';

/** 数据字段映射：不同组件用到的键不同 */
export interface WidgetMapping {
  /** 类目字段（柱/线/饼的维度、表格首列） */
  x?: string;
  /** 数值字段（柱/线/数值卡的指标） */
  y?: string;
  /** 系列字段（多系列柱/线） */
  series?: string;
  /** 饼图数值字段 */
  value?: string;
  /** 饼图/系列名称字段 */
  name?: string;
}

/** 大屏组件实例 */
export interface ScreenWidget {
  id: string;
  type: WidgetType;
  /** 图层面板显示名 */
  name: string;
  x: number;
  y: number;
  w: number;
  h: number;
  zIndex: number;
  /** 组件样式与文案 */
  props: Record<string, any>;
  /** 数据来源 */
  data: {
    mode: 'static' | 'dataset';
    datasetKey?: string;
    staticRows?: Record<string, unknown>[];
    mapping?: WidgetMapping;
  };
}

/** 大屏整体配置（存 configJson） */
export interface ScreenConfig {
  canvas: ScreenCanvas;
  widgets: ScreenWidget[];
}

/** 取数结果（单个数据集） */
export interface DatasetData {
  datasetKey: string;
  datasetName?: string;
  columns?: string[];
  rows?: Record<string, unknown>[];
  error?: string;
}

/** 展示页加载结果 */
export interface ScreenViewConfig {
  id: string;
  screenCode: string;
  screenName: string;
  configJson: string;
  datasets: DatasetData[];
}

/** 设计器加载结果 */
export interface ScreenDesignConfig {
  screen: VisualizationRecord;
  datasets: ScreenDataset[];
}
