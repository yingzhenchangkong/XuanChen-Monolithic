import { reactive } from 'vue';

/** 优先级选项（1高 2中 3低） */
export const TODO_PRIORITY_OPTIONS = [
  { label: '高', value: 1 },
  { label: '中', value: 2 },
  { label: '低', value: 3 },
] as const;

/** 优先级 -> 显示文案 */
export const priorityText = (priority?: number): string =>
  TODO_PRIORITY_OPTIONS.find((item) => item.value === priority)?.label ?? '中';

/** 优先级 -> 标签色 */
export const priorityColor = (priority?: number): string => {
  switch (priority) {
    case 1:
      return 'red';
    case 3:
      return 'blue';
    default:
      return 'orange';
  }
}

/** 状态 -> 显示文案 */
export const statusText = (status?: number): string => (status === 1 ? '已完成' : '待办');

/** 状态 -> 标签色 */
export const statusColor = (status?: number): string => (status === 1 ? 'green' : 'processing');

/** 查询参数（我的待办） */
export const queryParamsIndex = reactive({
  title: '',
  priority: undefined,
  status: undefined,
})

/** 查询配置（我的待办） */
export const queryFormItemsIndex = reactive([
  {
    name: 'title',
    label: '标题',
    type: 'input' as const,
    placeholder: '请输入待办标题',
  },
  {
    name: 'priority',
    label: '优先级',
    type: 'select' as const,
    placeholder: '请选择优先级',
    width: '160px',
    options: TODO_PRIORITY_OPTIONS.map(({ label, value }) => ({ label, value })),
  },
  {
    name: 'status',
    label: '状态',
    type: 'select' as const,
    placeholder: '请选择状态',
    width: '160px',
    options: [
      { label: '待办', value: 0 },
      { label: '已完成', value: 1 },
    ],
  },
]);

/** 定义表格的列（我的待办） */
export const columnsIndex = [
  {
    title: '#',
    dataIndex: '',
    key: 'rowIndex',
    align: 'center',
    width: 60,
    customRender:
      ({ index }: { index: number }) => index + 1,
  },
  {
    title: '标题',
    dataIndex: 'title',
    align: 'left',
  },
  {
    title: '内容',
    dataIndex: 'content',
    align: 'left',
    width: 240,
    ellipsis: true,
  },
  {
    title: '优先级',
    dataIndex: 'priority',
    align: 'center',
    width: 90,
  },
  {
    title: '状态',
    dataIndex: 'status',
    align: 'center',
    width: 90,
  },
  {
    title: '截止时间',
    dataIndex: 'dueDate',
    align: 'center',
    width: 150,
  },
  {
    title: '完成时间',
    dataIndex: 'finishTime',
    align: 'center',
    width: 150,
  },
  {
    title: '创建时间',
    dataIndex: 'createTime',
    align: 'center',
    width: 150,
  },
  {
    title: '操作',
    dataIndex: 'operation',
    align: 'center',
    width: 180,
  },
];
