/** 审批流程模块前后端契约类型（对应 com.xuanchen.businessprocess.*） */

/** 流程实例（列表行） */
export interface BpmInstanceRecord {
  id: string;
  definitionName?: string;
  definitionCode?: string;
  version?: number;
  businessType?: string;
  businessId?: string;
  businessSummary?: string;
  applyUser?: string;
  applyUserName?: string;
  /** 1审批中 2通过 3驳回 4撤回 */
  status?: number;
  currentNodeKey?: string;
  createTime?: string;
  finishTime?: string;
}

/** 审批任务 */
export interface BpmTaskRecord {
  id: string;
  instanceId: string;
  nodeKey?: string;
  nodeName?: string;
  assignee?: string;
  assigneeName?: string;
  /** 1待处理 2已处理 3已作废 */
  status?: number;
  /** 1同意 2驳回 */
  result?: number;
  opinion?: string;
  handleTime?: string;
  createTime?: string;
  businessSummary?: string;
}

/** 审批记录（时间线） */
export interface BpmRecordItem {
  id?: string;
  nodeKey?: string;
  nodeName?: string;
  operator?: string;
  operatorName?: string;
  /** 1提交 2同意 3驳回 4撤回 5转办 6抄送 7催办 */
  action?: number;
  opinion?: string;
  createTime?: string;
}

/** 可驳回目标节点（rejectTargets） */
export interface BpmRejectTarget {
  nodeKey: string;
  nodeName: string;
}

/** 审批委托规则 */
export interface BpmDelegateRecord {
  id: string;
  userName?: string;
  delegateUserName?: string;
  startTime?: string;
  endTime?: string;
  /** 1启用 0停用 */
  status?: number;
  remark?: string;
  createTime?: string;
}

/** 发起申请请求 */
export interface BpmSubmitPayload {
  definitionId: string;
  businessId?: string;
  businessSummary: string;
  formData?: Record<string, unknown>;
  chosenAssignees?: Record<string, string[]>;
}

/** 图渲染状态（diagramState） */
export interface BpmDiagramState {
  bpmnXml?: string;
  currentNodeKey?: string;
  passedNodeKeys?: string[];
  pendingNodeKeys?: string[];
  status?: number;
}

// ---------- 在线表单 ----------

/** 在线表单字段类型 */
export type FormFieldType = 'input' | 'textarea' | 'number' | 'date' | 'select' | 'radio' | 'checkbox' | 'switch';

/** 在线表单字段设计 */
export interface FormFieldSchema {
  /** 控件类型 */
  type: FormFieldType;
  /** 显示标签 */
  label: string;
  /** 字段名（提交进 formData 的 key） */
  key: string;
  placeholder?: string;
  required?: boolean;
  /** select/radio/checkbox 的可选项 */
  options?: string[];
  /** 占列宽度（antd 24 栅格：24整行 18=3/4 16=2/3 12=半行 8=1/3 6=1/4；缺省按类型兜底） */
  span?: number;
}

/** 在线表单设计（schema_json 的结构） */
export interface FormSchema {
  fields: FormFieldSchema[];
}

// ---------- 公共辅助 ----------

/** 用户下拉项 */
export interface BpmUserOption {
  userName: string;
  nickName: string;
}
