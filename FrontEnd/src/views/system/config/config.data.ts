import { reactive } from 'vue';
/** 查询参数 */
export const queryParams = reactive({
  configName: '',
  configDescription: '',
})
/** 查询参数 */
export const queryFormItems = [
  {
    name: 'configName',
    label: '参数名称',
    type: 'input' as const,
    placeholder: '请输入参数名称'
  },
  {
    name: 'configDescription',
    label: '参数描述',
    type: 'input' as const,
    placeholder: '请输入参数描述'
  }
];

/** 定义表格的列 列表页*/
export const columnsIndex = [
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
    title: '参数编码',
    dataIndex: 'configCode',
    align: 'left',
    width: 110
  },
  {
    title: '参数名称',
    dataIndex: 'configName',
    align: 'left',
    width: 150
  },
  {
    title: '参数描述',
    dataIndex: 'configDescription',
    align: 'left',
    width: 150
  },
  {
    title: '参数键名',
    dataIndex: 'configKey',
    align: 'left',
    width: 150
  },
  {
    title: '参数键值',
    dataIndex: 'configValue',
    align: 'left',
  },
  {
    title: '状态',
    dataIndex: 'status',
    align: 'center',
    width: 80
  },
  {
    title: '排序码',
    dataIndex: 'orderNo',
    align: 'center',
    width: 80
  },
  {
    title: '操作',
    dataIndex: 'operation',
    align: 'center',
    width: 150
  },
];

/** 定义表格的列 回收站*/
export const columnsRecBin = [
  {
    title: '#',
    dataIndex: '',
    key: 'rowIndex',
    align: 'center',
    customRender:
      function (text: any, record: any, index: any, column: any) {
        return parseInt(text.index) + 1;
      }
  },
  {
    title: '参数编码',
    dataIndex: 'configCode',
    align: 'center',
    width: 110
  },
  {
    title: '参数名称',
    dataIndex: 'configName',
    align: 'center',
    width: 150
  },
  {
    title: '参数描述',
    dataIndex: 'configDescription',
    align: 'left',
    width: 150
  },
  {
    title: '参数键名',
    dataIndex: 'configKey',
    align: 'left',
    width: 150
  },
  {
    title: '参数键值',
    dataIndex: 'configValue',
    align: 'left',
  },
  {
    title: '操作',
    dataIndex: 'operation',
    align: 'center',
    width: 240
  },
];