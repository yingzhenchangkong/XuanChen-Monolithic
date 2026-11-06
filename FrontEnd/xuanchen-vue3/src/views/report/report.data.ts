import { reactive } from 'vue';

/** 报表类型：1明细表 2汇总表 3自由格式 */
export const reportTypeOptions = [
  { label: '明细表', value: 1 },
  { label: '汇总表', value: 2 },
  { label: '自由格式', value: 3 },
];

export const reportTypeText = (type: number) =>
  ({ 1: '明细表', 2: '汇总表', 3: '自由格式' })[type] ?? '未知';

/** 数据源类型 */
export const dsTypeOptions = [
  { label: 'MySQL', value: 'mysql' },
  { label: 'Oracle', value: 'oracle' },
  { label: 'SQL Server', value: 'sqlserver' },
  { label: 'PostgreSQL', value: 'postgresql' },
];

/** 参数控件类型 */
export const paramTypeOptions = [
  { label: '文本', value: 'string' },
  { label: '数字', value: 'number' },
  { label: '日期', value: 'date' },
  { label: '字典', value: 'dict' },
];

/** 查询参数 */
export const queryParams = reactive({
  reportName: '',
  reportCode: '',
});

/** 查询配置 */
export const queryFormItems = reactive([
  {
    name: 'reportName',
    label: '报表名称',
    type: 'input' as const,
    placeholder: '请输入报表名称',
  },
  {
    name: 'reportCode',
    label: '报表编码',
    type: 'input' as const,
    placeholder: '请输入报表编码',
  },
]);

/** 列表列定义 */
export const columns = [
  {
    title: '#',
    dataIndex: '',
    key: 'rowIndex',
    align: 'center',
    width: 60,
    customRender: ({ index }: { index: number }) => index + 1,
  },
  {
    title: '报表编码',
    dataIndex: 'reportCode',
    align: 'left',
    width: 170,
  },
  {
    title: '报表名称',
    dataIndex: 'reportName',
    align: 'left',
  },
  {
    title: '模板',
    dataIndex: 'templateName',
    align: 'left',
    width: 150,
  },
  {
    title: '类型',
    dataIndex: 'reportType',
    align: 'center',
    width: 90,
  },
  {
    title: '操作',
    dataIndex: 'operation',
    align: 'center',
    width: 280,
    fixed: 'right' as const,
  },
];
