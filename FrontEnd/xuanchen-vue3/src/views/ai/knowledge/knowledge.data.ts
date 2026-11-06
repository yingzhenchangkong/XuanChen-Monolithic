import { reactive } from 'vue';

/** 解析状态 -> 显示文案 */
export const parseStatusText = (status?: string): string => {
  switch (status) {
    case 'ready':
      return '就绪';
    case 'parsing':
      return '解析中';
    case 'failed':
      return '失败';
    default:
      return '待解析';
  }
};

/** 解析状态 -> 标签色 */
export const parseStatusColor = (status?: string): string => {
  switch (status) {
    case 'ready':
      return 'green';
    case 'parsing':
      return 'processing';
    case 'failed':
      return 'red';
    default:
      return 'default';
  }
};

/** 文件大小格式化 */
export const formatSize = (size?: number): string => {
  if (!size) return '-';
  if (size < 1024) return `${size} B`;
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`;
  return `${(size / 1024 / 1024).toFixed(2)} MB`;
};

/** ===== 知识库列表页 ===== */

/** 查询参数 */
export const queryParams = reactive({
  kbName: '',
  status: undefined,
});

/** 查询配置 */
export const queryFormItems = reactive([
  {
    name: 'kbName',
    label: '名称',
    type: 'input' as const,
    placeholder: '请输入知识库名称',
  },
  {
    name: 'status',
    label: '状态',
    type: 'select' as const,
    placeholder: '请选择状态',
    width: '140px',
    options: [
      { label: '启用', value: 1 },
      { label: '停用', value: 0 },
    ],
  },
]);

/** 列表列 */
export const columnsIndex = [
  {
    title: '#',
    dataIndex: '',
    key: 'rowIndex',
    align: 'center' as const,
    width: 60,
    customRender: ({ index }: { index: number }) => index + 1,
  },
  {
    title: '知识库名称',
    dataIndex: 'kbName',
    align: 'left' as const,
  },
  {
    title: '描述',
    dataIndex: 'kbDesc',
    align: 'left' as const,
    ellipsis: true,
  },
  {
    title: '向量化模型',
    dataIndex: 'embedModel',
    align: 'center' as const,
    width: 130,
  },
  {
    title: '分块大小',
    dataIndex: 'chunkSize',
    align: 'center' as const,
    width: 90,
  },
  {
    title: '分块重叠',
    dataIndex: 'chunkOverlap',
    align: 'center' as const,
    width: 90,
  },
  {
    title: '状态',
    dataIndex: 'status',
    align: 'center' as const,
    width: 80,
  },
  {
    title: '创建时间',
    dataIndex: 'createTime',
    align: 'center' as const,
    width: 160,
  },
  {
    title: '操作',
    dataIndex: 'operation',
    key: 'operation',
    align: 'center' as const,
    width: 220,
  },
];

/** 回收站列 */
export const columnsRecBin = [
  {
    title: '#',
    dataIndex: '',
    key: 'rowIndex',
    align: 'center' as const,
    width: 60,
    customRender: ({ index }: { index: number }) => index + 1,
  },
  {
    title: '知识库名称',
    dataIndex: 'kbName',
    align: 'left' as const,
  },
  {
    title: '描述',
    dataIndex: 'kbDesc',
    align: 'left' as const,
    ellipsis: true,
  },
  {
    title: '创建时间',
    dataIndex: 'createTime',
    align: 'center' as const,
    width: 160,
  },
];

/** ===== 文档管理 ===== */

export const columnsDoc = [
  {
    title: '#',
    dataIndex: '',
    key: 'rowIndex',
    align: 'center' as const,
    width: 55,
    customRender: ({ index }: { index: number }) => index + 1,
  },
  {
    title: '文件名',
    dataIndex: 'fileName',
    align: 'left' as const,
    ellipsis: true,
  },
  {
    title: '类型',
    dataIndex: 'fileType',
    align: 'center' as const,
    width: 80,
  },
  {
    title: '大小',
    dataIndex: 'fileSize',
    align: 'center' as const,
    width: 90,
    customRender: ({ text }: { text: number }) => formatSize(text),
  },
  {
    title: '解析状态',
    dataIndex: 'status',
    align: 'center' as const,
    width: 90,
  },
  {
    title: '分块数',
    dataIndex: 'chunkCount',
    align: 'center' as const,
    width: 80,
  },
  {
    title: '失败原因',
    dataIndex: 'errorMsg',
    align: 'left' as const,
    ellipsis: true,
  },
  {
    title: '操作',
    dataIndex: 'operation',
    key: 'operation',
    align: 'center' as const,
    width: 150,
  },
];
