export interface TemplateRecord {
  id?: string;
  templateCode?: string;
  templateName?: string;
  /** 1明细表 2汇总表 3自由格式 */
  templateType?: number | string;
  configJson?: string;
  /** 1启用 0停用 */
  status?: number;
  remark?: string;
  createBy?: string;
  createName?: string;
  createTime?: string;
  updateBy?: string;
  updateName?: string;
  updateTime?: string;
  [key: string]: unknown;
}
