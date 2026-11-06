import { reactive } from 'vue';
import type { SelectOption } from '@/types/api';

/** 查询参数 */
export const queryParams = reactive({
  tableName: '',
  moduleName: '',
  generateType: undefined,
  createBy: '',
  success: undefined,
  timeRange: [],
  beginTime: undefined,
  endTime: undefined,
});

/** 查询表单项 */
export const queryFormItems = reactive([
  {
    name: 'tableName',
    label: '表名',
    type: 'input' as const,
    placeholder: '请输入物理表名',
  },
  {
    name: 'moduleName',
    label: '模块',
    type: 'input' as const,
    placeholder: '请输入模块名',
  },
  {
    name: 'generateType',
    label: '生成方式',
    type: 'select' as const,
    placeholder: '请选择生成方式',
    width: '140px',
    options: [
      { label: '落盘生成', value: 'DISK' },
      { label: '打包下载', value: 'ZIP' },
      { label: '预览', value: 'PREVIEW' },
    ] as SelectOption[],
  },
  {
    name: 'success',
    label: '生成结果',
    type: 'select' as const,
    placeholder: '请选择生成结果',
    width: '140px',
    options: [] as SelectOption[],
    fieldNames: {
      label: 'dictItemText',
      value: 'dictItemValue',
    },
  },
  {
    name: 'timeRange',
    label: '生成时间',
    type: 'rangePicker' as const,
    placeholder: ['开始时间', '结束时间'],
    showTime: true,
    valueFormat: 'YYYY-MM-DD HH:mm:ss',
    displayFormat: 'YYYY-MM-DD HH:mm:ss',
  },
]);

/** 定义表格的列 列表页 */
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
    title: '生成时间',
    dataIndex: 'createTime',
    align: 'left',
    width: 170,
  },
  {
    title: '表名',
    dataIndex: 'tableName',
    align: 'left',
    width: 220,
  },
  {
    title: '模块',
    dataIndex: 'moduleName',
    align: 'left',
    width: 120,
  },
  {
    title: '生成方式',
    dataIndex: 'generateType',
    align: 'center',
    width: 110,
  },
  {
    title: '模板',
    dataIndex: 'template',
    align: 'center',
    width: 100,
  },
  {
    title: '文件',
    dataIndex: 'fileList',
    align: 'center',
    width: 110,
  },
  {
    title: '结果',
    dataIndex: 'success',
    align: 'center',
    width: 90,
  },
  {
    title: '操作人',
    dataIndex: 'createBy',
    align: 'center',
    width: 110,
  },
  {
    title: '说明',
    dataIndex: 'message',
    align: 'left',
    width: 300,
  },
];
