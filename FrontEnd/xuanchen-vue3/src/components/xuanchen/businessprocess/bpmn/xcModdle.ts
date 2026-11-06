/**
 * XuanChen BPMN 扩展描述符（与后端 BpmnDiagramParser 的 xc 命名空间一致）
 *
 * - bpmn:UserTask    追加 xc:approvalMode（1或签 2会签）、xc:rules（JSON 数组字符串）
 * - bpmn:SendTask    追加 xc:rules（抄送对象规则，仅指定用户/指定角色）
 * - bpmn:SequenceFlow 追加 xc:condition（条件表达式；网关默认流用 BPMN 标准 default 属性）
 */
export const XcModdleDescriptor = {
  name: 'XuanChen BusinessProcess',
  uri: 'http://xuanchen-monolithic.cn/schema/bpmn/xc',
  prefix: 'xc',
  xml: { tagAlias: 'lowerCase' },
  types: [
    {
      name: 'XcUserTaskExt',
      extends: ['bpmn:UserTask'],
      properties: [
        { name: 'approvalMode', isAttr: true, type: 'String' },
        { name: 'rules', isAttr: true, type: 'String' },
      ],
    },
    {
      name: 'XcSendTaskExt',
      extends: ['bpmn:SendTask'],
      properties: [{ name: 'rules', isAttr: true, type: 'String' }],
    },
    {
      name: 'XcSequenceFlowExt',
      extends: ['bpmn:SequenceFlow'],
      properties: [{ name: 'condition', isAttr: true, type: 'String' }],
    },
  ],
  enumerations: [],
  associations: [],
};

/** 审批人规则项（随 xc:rules JSON 存于用户任务） */
export interface XcRuleItem {
  ruleType: number; // 1指定用户 2指定角色 3发起人自选 4部门负责人 5直属上级(第N级主管) 6表单字段指定
  ruleValue: string;
}

/** 从用户任务元素读取规则（容错） */
export const readRules = (element: any): XcRuleItem[] => {
  try {
    const raw = element?.businessObject?.get?.('xc:rules') ?? element?.businessObject?.rules;
    if (!raw) return [];
    const parsed = JSON.parse(raw);
    return Array.isArray(parsed) ? parsed : [];
  } catch {
    return [];
  }
};

/** 写入用户任务规则 */
export const writeRules = (modeling: any, element: any, rules: XcRuleItem[]) => {
  modeling.updateProperties(element, { 'xc:rules': JSON.stringify(rules) });
};
