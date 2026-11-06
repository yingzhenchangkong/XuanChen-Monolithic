/** 状态映射与公共配置（流程审批模块） */
import { reactive } from 'vue';

/** 定义状态：0草稿 1启用 2停用 */
export const definitionStatusText = (status?: number) =>
  ({ 0: '草稿', 1: '启用', 2: '停用' } as Record<number, string>)[Number(status)] ?? '未知';

export const definitionStatusColor = (status?: number) =>
  ({ 0: 'default', 1: 'green', 2: 'volcano' } as Record<number, string>)[Number(status)] ?? 'default';

export const definitionStatusOptions = [
  { label: '草稿', value: 0 },
  { label: '启用', value: 1 },
  { label: '停用', value: 2 },
];

/** 实例状态：1审批中 2通过 3驳回 4撤回 */
export const instanceStatusText = (status?: number) =>
  ({ 1: '审批中', 2: '通过', 3: '驳回', 4: '撤回' } as Record<number, string>)[Number(status)] ?? '未知';

export const instanceStatusColor = (status?: number) =>
  ({ 1: 'processing', 2: 'success', 3: 'error', 4: 'default' } as Record<number, string>)[Number(status)] ?? 'default';

export const instanceStatusOptions = [
  { label: '审批中', value: 1 },
  { label: '通过', value: 2 },
  { label: '驳回', value: 3 },
  { label: '撤回', value: 4 },
];

/** 审批人规则类型：1指定用户 2指定角色 3发起人自选 4部门负责人 5直属上级(第N级主管) 6表单字段指定 */
export const ruleTypeText = (type?: number) =>
  ({ 1: '指定用户', 2: '指定角色', 3: '发起人自选', 4: '部门负责人', 5: '直属上级', 6: '表单字段' } as Record<number, string>)[Number(type)] ?? '未知';

/** 审批方式：1或签 2会签 */
export const approvalModeText = (mode?: number) =>
  ({ 1: '或签（一人通过即过）', 2: '会签（全员通过才过）' } as Record<number, string>)[Number(mode)] ?? '或签';

/** 时间线动作：1提交 2同意 3驳回 4撤回 5转办 6抄送 7催办 */
export const recordActionText = (action?: number) =>
  ({ 1: '提交申请', 2: '同意', 3: '驳回', 4: '撤回', 5: '转办', 6: '抄送', 7: '催办' } as Record<number, string>)[Number(action)] ?? '操作';

export const recordActionColor = (action?: number) =>
  ({ 1: 'blue', 2: 'green', 3: 'red', 4: 'default', 5: 'orange', 6: 'cyan', 7: 'purple' } as Record<number, string>)[Number(action)] ?? 'blue';

/** 通用业务类型选项（发起申请分类展示；业务模块接入后在此扩充） */
export const businessTypeOptions = [
  { label: '通用审批', value: 'common' },
];

// ==================== 流程配置列表 ====================

export const queryParams = reactive<Record<string, unknown>>({
  name: '',
  code: '',
  status: undefined as number | undefined,
  pageNo: 1,
  pageSize: 10,
});

export const queryFormItems = reactive([
  { name: 'name', label: '流程名称', type: 'input' as const, placeholder: '请输入流程名称' },
  { name: 'code', label: '流程编码', type: 'input' as const, placeholder: '请输入流程编码' },
  { name: 'status', label: '状态', type: 'select' as const, placeholder: '请选择状态', options: definitionStatusOptions },
]);

export const columns = [
  {
    title: '#',
    dataIndex: '',
    key: 'rowIndex',
    align: 'center',
    width: 60,
    customRender: ({ index }: { index: number }) => index + 1,
  },
  { title: '流程编码', dataIndex: 'code', align: 'left', width: 160 },
  { title: '流程名称', dataIndex: 'name', align: 'left', width: 180 },
  { title: '业务类型', dataIndex: 'businessType', align: 'left', width: 140 },
  { title: '版本', dataIndex: 'version', align: 'center', width: 70 },
  { title: '状态', dataIndex: 'status', align: 'center', width: 90 },
  { title: '备注', dataIndex: 'remark', align: 'left', ellipsis: true },
  { title: '操作', dataIndex: 'operation', align: 'center', width: 300, fixed: 'right' as const },
];
