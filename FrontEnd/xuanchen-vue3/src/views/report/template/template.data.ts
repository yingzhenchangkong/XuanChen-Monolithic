import { reactive } from 'vue';

/** 模板状态：1启用 0停用 */
export const templateStatusOptions = [
  { label: '启用', value: 1 },
  { label: '停用', value: 0 },
];

export const templateStatusText = (status: number) => ({ 1: '启用', 0: '停用' })[status] ?? '未知';

/** 模板列表查询参数 */
export const templateQueryParams = reactive({
  templateName: '',
  templateCode: '',
});

/** 模板列表查询配置 */
export const templateQueryFormItems = reactive([
  {
    name: 'templateName',
    label: '模板名称',
    type: 'input' as const,
    placeholder: '请输入模板名称',
  },
  {
    name: 'templateCode',
    label: '模板编码',
    type: 'input' as const,
    placeholder: '请输入模板编码',
  },
]);

/** 模板列表列定义 */
export const templateColumns = [
  {
    title: '#',
    dataIndex: '',
    key: 'rowIndex',
    align: 'center',
    width: 60,
    customRender: ({ index }: { index: number }) => index + 1,
  },
  {
    title: '模板编码',
    dataIndex: 'templateCode',
    align: 'left',
    width: 170,
  },
  {
    title: '模板名称',
    dataIndex: 'templateName',
    align: 'left',
  },
  {
    title: '类型',
    dataIndex: 'templateType',
    align: 'center',
    width: 100,
  },
  {
    title: '状态',
    dataIndex: 'status',
    align: 'center',
    width: 90,
  },
  {
    title: '操作',
    dataIndex: 'operation',
    align: 'center',
    width: 300,
    fixed: 'right' as const,
  },
];
