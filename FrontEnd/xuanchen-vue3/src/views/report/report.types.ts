/**
 * 报表管理类型定义。
 * configJson 结构（前端与后端共同维护，模板与报表同构）：
 *   {
 *     snapshot: IWorkbookData,        // Univer 工作簿快照（模板样式/内容）
 *     params: ReportParam[],          // 查看页顶栏查询参数
 *     bindings: Record<string, ReportBinding>  // 单元格/区域绑定（key = 绑定标识）
 *   }
 * 设计整体在「模板设计」完成；报表新增时复制模板 configJson，之后与模板解耦。
 */

/** 报表参数定义（查看页查询栏控件） */
export interface ReportParam {
  code: string;
  label: string;
  /** string 文本 / number 数字 / date 日期 / dict 字典下拉 */
  type: 'string' | 'number' | 'date' | 'dict';
  /** type=dict 时的字典类型编码（sys_dict） */
  dictType?: string;
  defaultValue?: string;
  required?: boolean;
}

/** list 绑定的列映射：A -> 字段名 或 { field, numFmt } */
export type BindingColumnMap = Record<string, string | { field: string; numFmt?: string }>;

/** 区域绑定（明细列表扩展区） */
export interface ReportListBinding {
  type: 'list';
  /** 左上角锚定单元格，如 A5 */
  anchor: string;
  /** 模板行区域（单行），如 A5:F5 */
  range: string;
  /** 行数上限（默认取数据行数，可限制） */
  limit?: number;
  dataset: string;
  columns: BindingColumnMap;
}

/** 单值绑定（汇总单元格） */
export interface ReportCellBinding {
  type: 'cell';
  /** 单元格引用，如 C3 */
  ref: string;
  dataset: string;
  /** 取数字段；agg 非空时由前端对 rows 做聚合 */
  field: string;
  /** 聚合方式：SUM/AVG/COUNT/MAX/MIN，空 = 直接取第一行字段值 */
  agg?: 'SUM' | 'AVG' | 'COUNT' | 'MAX' | 'MIN' | '';
  numFmt?: string;
}

export type ReportBinding = ReportListBinding | ReportCellBinding;

/** 报表配置 JSON 整体结构 */
export interface ReportConfig {
  snapshot?: Record<string, unknown>;
  params?: ReportParam[];
  bindings?: Record<string, ReportBinding>;
}

/** 数据集参数定义（与后端 rpt_dataset.params_json 一致） */
export interface DatasetParam {
  code: string;
  label: string;
  type: 'string' | 'number' | 'date' | 'dict';
  dictType?: string;
  defaultValue?: string;
  required?: boolean;
}

/** 报表记录（rpt_report） */
export interface ReportRecord {
  id?: string;
  reportCode?: string;
  reportName?: string;
  reportType?: number | string;
  /** 来源模板ID（新增时选择；模板快照已复制进 configJson，之后与模板解耦） */
  templateId?: string;
  /** 来源模板名称（列表展示用） */
  templateName?: string;
  configJson?: string;
  /** 0草稿 1已发布 2停用 */
  status?: number;
  publishTime?: string;
  remark?: string;
  createBy?: string;
  createName?: string;
  createTime?: string;
  updateBy?: string;
  updateName?: string;
  updateTime?: string;
  [key: string]: unknown;
}

/** 数据集记录（rpt_dataset） */
export interface DatasetRecord {
  id?: string;
  /** 所属模板ID（设计整体在「模板设计」，数据集随模板复用） */
  templateId?: string;
  dsId?: string | null;
  datasetCode?: string;
  datasetName?: string;
  sqlText?: string;
  paramsJson?: string;
  dsName?: string;
  createTime?: string;
  [key: string]: unknown;
}

/** 数据源下拉项（/system/datasource/select 返回，不含连接串与口令） */
export interface DatasourceRecord {
  id?: string;
  dsName?: string;
  dsType?: string;
  status?: number;
  [key: string]: unknown;
}

/** 数据集取数结果 */
export interface DatasetResult {
  columns: string[];
  rows: Record<string, unknown>[];
  total: number;
}

/** 查看页加载的配置数据（后端 /report/viewConfig 返回） */
export interface ViewConfigData {
  id: string;
  reportCode: string;
  reportName: string;
  configJson: string;
  datasets: { datasetCode: string; datasetName: string; paramsJson: string }[];
}
