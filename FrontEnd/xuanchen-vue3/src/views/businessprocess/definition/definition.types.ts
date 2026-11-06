export interface BpmDefinitionRecord {
  id?: string;
  name?: string;
  code?: string;
  businessType?: string;
  version?: number;
  /** 状态：0草稿 1启用 2停用 */
  status?: number;
  bpmnXml?: string;
  /** 绑定的在线表单ID（空=不使用在线表单） */
  onlineFormId?: string | null;
  onlineFormName?: string | null;
  remark?: string;
  createBy?: string;
  createName?: string;
  createTime?: string;
  updateBy?: string;
  updateName?: string;
  updateTime?: string;
  [key: string]: unknown;
}
