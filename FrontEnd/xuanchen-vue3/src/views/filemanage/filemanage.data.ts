import { reactive } from 'vue';

/** 文件大小格式化（字节 → 可读） */
export const formatFileSize = (size?: number): string => {
  if (size === undefined || size === null) return '-';
  if (size < 1024) return `${size} B`;
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`;
  if (size < 1024 * 1024 * 1024) return `${(size / 1024 / 1024).toFixed(2)} MB`;
  return `${(size / 1024 / 1024 / 1024).toFixed(2)} GB`;
};

/** 可内联预览的扩展名（与后端 FileInfoController#isInline 保持一致） */
export const INLINE_EXTS = ['jpg', 'png', 'gif', 'webp', 'bmp', 'pdf', 'txt', 'md', 'csv', 'json'];

export const isInlineExt = (ext?: string): boolean => !!ext && INLINE_EXTS.includes(ext);

/** Office 文档扩展名（走 ONLYOFFICE 原格式只读预览） */
export const OFFICE_EXTS = ['doc', 'docx', 'xls', 'xlsx', 'ppt', 'pptx'];

export const isOfficeExt = (ext?: string): boolean => !!ext && OFFICE_EXTS.includes(ext);

/** 图片扩展名（预览弹窗用 img 渲染，其余 pdf/txt 用 iframe） */
export const IMAGE_EXTS = ['jpg', 'png', 'gif', 'webp', 'bmp'];

export const isImageExt = (ext?: string): boolean => !!ext && IMAGE_EXTS.includes(ext);

/**
 * 文本扩展名：走 /fileinfo/text 取解码后的内容、用 pre 渲染。
 * 不能像 pdf 那样把预签名地址丢给 iframe——text/plain 不带 charset，浏览器按默认编码渲染会乱码，
 * 且 GBK 文件即使声明了 charset=UTF-8 也照样乱码（编码探测只能在服务端做）。
 * 与后端 FileInfoServiceImpl#TEXT_EXTS 保持一致。
 */
export const TEXT_EXTS = ['txt', 'md', 'markdown', 'csv', 'json', 'log', 'xml', 'yml', 'yaml', 'ini', 'properties', 'sql'];

export const isTextExt = (ext?: string): boolean => !!ext && TEXT_EXTS.includes(ext);

/** 存储类型文案 */
export const storageTypeText = (storageType?: number) =>
  ({ 0: '本地', 1: 'MinIO' } as Record<number, string>)[Number(storageType)] ?? '未知';

/** 查询参数（与 XCListPage 共用同一份 reactive 对象） */
export const queryParams = reactive<Record<string, unknown>>({
  keyword: '',
  ext: undefined as string | undefined,
  /** 当前文件夹：'0'=根目录（默认与文件夹树选中项一致），undefined=全部 */
  folderId: '0',
  pageNo: 1,
  pageSize: 10,
});

/** 查询配置 */
export const queryFormItems = reactive([
  { name: 'keyword', label: '文件名', type: 'input' as const, placeholder: '请输入文件名关键字' },
  {
    name: 'ext',
    label: '类型',
    type: 'select' as const,
    placeholder: '请选择类型',
    options: [
      { label: '图片', value: 'jpg' },
      { label: 'PDF', value: 'pdf' },
      { label: 'Word', value: 'docx' },
      { label: 'Excel', value: 'xlsx' },
      { label: 'PPT', value: 'pptx' },
      { label: '文本', value: 'txt' },
      { label: '压缩包', value: 'zip' },
    ],
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
  { title: '文件名', dataIndex: 'originalName', align: 'left', ellipsis: true },
  { title: '类型', dataIndex: 'ext', align: 'center', width: 90 },
  { title: '大小', dataIndex: 'fileSize', align: 'right', width: 110 },
  { title: '存储', dataIndex: 'storageType', align: 'center', width: 90 },
  { title: '上传时间', dataIndex: 'createTime', align: 'center', width: 90 },
  { title: '操作', dataIndex: 'operation', align: 'center', width: 150, fixed: 'right' as const },
];

/** 回收站列定义 */
export const recycleColumns = [
  {
    title: '#',
    dataIndex: '',
    key: 'rowIndex',
    align: 'center',
    width: 60,
    customRender: ({ index }: { index: number }) => index + 1,
  },
  { title: '文件名', dataIndex: 'originalName', align: 'left', width: 320, ellipsis: true },
  { title: '类型', dataIndex: 'ext', align: 'center', width: 90 },
  { title: '大小', dataIndex: 'fileSize', align: 'right', width: 110 },
  { title: '归属人', dataIndex: 'username', align: 'center', width: 120 },
  { title: '删除时间', dataIndex: 'deleteTime', align: 'center', width: 170 },
  { title: '操作', dataIndex: 'operation', align: 'center', width: 180, fixed: 'right' as const },
];
