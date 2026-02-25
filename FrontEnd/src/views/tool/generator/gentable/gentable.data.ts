import { reactive } from 'vue';
/** 查询参数 */
export const queryParams = reactive({
  tableName: '',
  tableComment: '',
})
/** 查询配置 */
export const queryFormItems = reactive([
  {
    name: 'tableName',
    label: '表名',
    type: 'input' as const,
    placeholder: '请输入表名',
  },
  {
    name: 'tableComment',
    label: '表注释',
    type: 'input' as const,
    placeholder: '请输入表注释',
  },
]);

/** 定义表格的列 列表页 */
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
    title: '表名',
    dataIndex: 'tableName',
    align: 'left',
    width: 120,
  },
  {
    title: '表注释',
    dataIndex: 'tableComment',
    align: 'left',
  },
  {
    title: '操作',
    dataIndex: 'operation',
    align: 'center',
    width: 145,
  },
];