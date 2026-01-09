import { reactive } from 'vue';
/** 查询参数 */
export const queryParamsIndex = reactive({
  dictName: '',
})
/** 查询配置 */
export const queryFormItemsIndex = [
  {
    name: 'dictName',
    label: '字典名称',
    type: 'input' as const,
    placeholder: '请输入字典名称'
  },
];

/** 定义表格的列 */
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
    title: '字典编码',
    dataIndex: 'dictCode',
    align: 'left',
    width: 200
  },
  {
    title: '字典名称',
    dataIndex: 'dictName',
    align: 'left',
  },
  {
    title: '排序码',
    dataIndex: 'orderNo',
    align: 'center',
    width: 80
  },
  {
    title: '状态',
    dataIndex: 'status',
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

/** 查询参数（项目） */
export const queryParamsItem = reactive({
  dictCode: '',
})
/** 定义表格的列（项目） */
export const columnsItem = [
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
    title: '数据值',
    dataIndex: 'dictItemValue',
    align: 'left',
    width: 150
  },
  {
    title: '名称',
    dataIndex: 'dictItemText',
    align: 'left',
  },
  {
    title: '排序码',
    dataIndex: 'orderNo',
    align: 'center',
    width: 80
  },
  {
    title: '状态',
    dataIndex: 'status',
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