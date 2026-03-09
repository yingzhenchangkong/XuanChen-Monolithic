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

export const columnsTable = [
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
    title: '字段名',
    dataIndex: 'columnName',
    align: 'left',
  },
  {
    title: '字段注释',
    dataIndex: 'columnComment',
    align: 'left',
  },
  {
    title: '字段类型',
    dataIndex: 'columnType',
    align: 'left',
  },
  {
    title: '字段长度',
    dataIndex: 'columnLength',
    align: 'left',
  },
  {
    title: '字段精度',
    dataIndex: 'columnScale',
    align: 'left',
  },
  {
    title: '默认值',
    dataIndex: 'columnDefault',
    align: 'left',
  },
  {
    title: '是否主键',
    dataIndex: 'columnIsPk',
    align: 'center',
  },
  {
    title: '是否可为空',
    dataIndex: 'columnIsNullable',
    align: 'center',
  },
];

export const columnsUI = [
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
    title: '字段名',
    dataIndex: 'columnName',
    align: 'left',
  },
  {
    title: '字段注释',
    dataIndex: 'columnComment',
    align: 'left',
  },
  {
    title: '是否查询',
    dataIndex: 'uiIsQuery',
    align: 'center',
  },
  {
    title: '查询方式',
    dataIndex: 'uiQueryType',
    align: 'left',
  },
  {
    title: '是否编辑',
    dataIndex: 'uiIsEdit',
    align: 'center',
  },
  {
    title: '是否必填',
    dataIndex: 'uiIsRequired',
    align: 'center',
  },
  {
    title: '编辑方式',
    dataIndex: 'uiEditType',
    align: 'center',
  },
  {
    title: '是否列表',
    dataIndex: 'uiIsList',
    align: 'center',
  },
  
];

export const tableColumn = [
  {
    id: '',
    tableId: '',
    columnName: 'id',
    columnComment: '主键',
    columnType: 'varchar',
    columnLength: '32',
    columnScale: '',
    columnDefault: '',
    columnIsPk: true,
    columnIsNullable: false,
  },
  {
    id: '',
    tableId: '',
    columnName: 'createBy',
    columnComment: '创建人',
    columnType: 'varchar',
    columnLength: '32',
    columnScale: '',
    columnDefault: '',
    columnIsPk: false,
    columnIsNullable: true,
  },
  {
    id: '',
    tableId: '',
    columnName: 'createTime',
    columnComment: '创建时间',
    columnType: 'datetime',
    columnLength: '',
    columnScale: '',
    columnDefault: '',
    columnIsPk: false,
    columnIsNullable: true,
  },
  {
    id: '',
    tableId: '',
    columnName: 'updateBy',
    columnComment: '更新人',
    columnType: 'varchar',
    columnLength: '32',
    columnScale: '',
    columnDefault: '',
    columnIsPk: false,
    columnIsNullable: true,
  },
  {
    id: '',
    tableId: '',
    columnName: 'updateTime',
    columnComment: '更新时间',
    columnType: 'datetime',
    columnLength: '',
    columnScale: '',
    columnDefault: '',
    columnIsPk: false,
    columnIsNullable: true,
  },
];