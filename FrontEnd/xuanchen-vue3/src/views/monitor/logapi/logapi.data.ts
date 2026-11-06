import { reactive } from 'vue';
import type { SelectOption } from '@/types/api';

/** 查询参数 */
export const queryParams = reactive({
  apiCode: '',
  triggerType: undefined,
  success: undefined,
  timeRange: [],
  beginTime: undefined,
  endTime: undefined,
});

/** 查询表单项 */
export const queryFormItems = reactive([
  {
    name: 'apiCode',
    label: '接口编码',
    type: 'input' as const,
    placeholder: '请输入接口编码',
  },
  {
    name: 'triggerType',
    label: '触发方式',
    type: 'select' as const,
    placeholder: '请选择触发方式',
    width: '140px',
    options: [
      { label: '手动调用', value: 1 },
      { label: '定时采集', value: 2 },
      { label: '测试运行', value: 3 },
    ] as SelectOption[],
  },
  {
    name: 'success',
    label: '调用结果',
    type: 'select' as const,
    placeholder: '请选择调用结果',
    width: '140px',
    options: [] as SelectOption[],
    fieldNames: {
      label: 'dictItemText',
      value: 'dictItemValue',
    },
  },
  {
    name: 'timeRange',
    label: '调用时间',
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
    title: '接口',
    dataIndex: 'apiName',
    align: 'left',
    width: 220,
  },
  {
    title: '触发方式',
    dataIndex: 'triggerType',
    align: 'center',
    width: 100,
  },
  {
    title: '调用结果',
    dataIndex: 'success',
    align: 'center',
    width: 100,
  },
  {
    title: '耗时',
    dataIndex: 'costMs',
    align: 'right',
    width: 100,
  },
  {
    title: '失败原因',
    dataIndex: 'errorMsg',
    align: 'left',
    width: 300,
  },
  {
    title: '调用时间',
    dataIndex: 'createTime',
    align: 'left',
    width: 170,
  },
];
