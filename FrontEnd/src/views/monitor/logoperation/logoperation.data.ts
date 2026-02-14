import { reactive } from 'vue';
/** 查询参数 */
export const queryParams = reactive({
  userName: undefined,
  timeRange: [],
  beginTime: undefined,
  endTime: undefined,
  status: undefined,
})
/** 查询参数 */
export const queryFormItems = reactive([
  {
    name: 'userName',
    label: '用户名',
    type: 'select' as const,
    placeholder: '请选择用户名',
    width: '180px',
    options: [],
    fieldNames: {
      label: 'nickName',
      value: 'userName'
    }
  },
  {
    name: 'timeRange',
    label: '时间范围',
    type: 'rangePicker' as const,
    placeholder: ['开始日期', '结束日期'],
    showTime: false,
    valueFormat: 'YYYY-MM-DD',
    displayFormat: 'YYYY-MM-DD',
  },
  {
    name: 'status',
    label: '登录状态',
    type: 'select' as const,
    placeholder: '请选择登录状态',
    options: [],
    fieldNames: {
      label: 'dictItemText',
      value: 'dictItemValue'
    }
  }
]);

/** 定义表格的列 列表页*/
export const columns = [
  {
    title: '#',
    dataIndex: '',
    key: 'rowIndex',
    align: 'center',
    width: 60,
    customRender:
      function (text: any, record: any, index: any, column: any) {
        return parseInt(text.index) + 1;
      }
  },
  {
    title: '操作用户',
    dataIndex: 'userName_dictText',
    align: 'left'
  },
  {
    title: '模块',
    dataIndex: 'module',
    align: 'left'
  },
  {
    title: '操作时间',
    dataIndex: 'operationTime',
    align: 'left'
  },
  {
    title: '操作类型',
    dataIndex: 'operationType',
    align: 'center'
  },
  {
    title: '请求URL',
    dataIndex: 'requestUrl',
    align: 'left'
  },
  {
    title: '请求参数',
    dataIndex: 'requestParams',
    align: 'left'
  },
  {
    title: 'IP地址',
    dataIndex: 'ipAddress',
    align: 'left'
  },
  {
    title: '状态',
    dataIndex: 'status',
    align: 'left'
  },
  {
    title: '操作描述',
    dataIndex: 'description',
    align: 'left'
  }
];