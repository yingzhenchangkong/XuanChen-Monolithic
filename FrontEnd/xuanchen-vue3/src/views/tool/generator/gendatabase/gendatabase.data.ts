import { reactive } from 'vue';
/** 查询参数 */
export const queryParams = reactive({
  connType: undefined,
  connName: '',
  host: '',
})
/** 查询配置 */
export const queryFormItems = reactive([
  {
    name: 'connType',
    label: '连接类型',
    type: 'select' as const,
    placeholder: '请选择连接类型',
    options: [],
    fieldNames: {
      label: 'dictItemText',
      value: 'dictItemValue'
    }
  },
  {
    name: 'connName',
    label: '连接名称',
    type: 'input' as const,
    placeholder: '请输入连接名称'
  },
  {
    name: 'host',
    label: '主机地址',
    type: 'input' as const,
    placeholder: '请输入主机地址'
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
    title: '连接类型',
    dataIndex: 'connType',
    align: 'left',
  },
  {
    title: '连接名称',
    dataIndex: 'connName',
    align: 'left',
  },
  {
    title: '主机地址',
    dataIndex: 'host',
    align: 'left',
  },
  {
    title: '端口',
    dataIndex: 'port',
    align: 'left',
  },
  {
    title: '数据库名称',
    dataIndex: 'dbName',
    align: 'left',
  },
  {
    title: '用户名',
    dataIndex: 'userName',
    align: 'left',
  },
  {
    title: '密码',
    dataIndex: 'password',
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
    width: 145
  },
];

/** 定义表格的列 回收站 */
export const columnsRecBin = [
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
    title: '连接类型',
    dataIndex: 'connType',
    align: 'left',
  },
  {
    title: '连接名称',
    dataIndex: 'connName',
    align: 'left',
  },
  {
    title: '主机地址',
    dataIndex: 'host',
    align: 'left',
  },
  {
    title: '端口',
    dataIndex: 'port',
    align: 'left',
  },
  {
    title: '数据库名称',
    dataIndex: 'dbName',
    align: 'left',
  },
  {
    title: '操作',
    dataIndex: 'operation',
    align: 'center',
    width: 180
  },
];