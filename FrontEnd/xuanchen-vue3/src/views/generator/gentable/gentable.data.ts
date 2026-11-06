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
      ({ index }: { index: number }) => index + 1,
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
    title: '创建表',
    dataIndex: 'ifCreateTable',
    align: 'center',
    width: 90,
    customRender: ({ text }: { text: boolean }) => (text ? '是' : '否'),
  },
  {
    title: '模板',
    dataIndex: 'template',
    align: 'center',
    width: 100,
    customRender: ({ text }: { text: string }) => (text === 'single' ? '单表' : '一对多'),
  },
  {
    title: '操作',
    dataIndex: 'operation',
    align: 'center',
    width: 400,
  },
];

/**
 * 字段配置表：
 * 一行 = 一个字段，只保留库表属性；该字段的页面配置收进「行下方展开区」，
 * 默认折叠，点操作列的「页面配置」再展开，避免表格过宽。
 * 只有窄控件列给固定宽度，字段名/注释两列不设宽度，吃掉剩余空间，避免横向滚动条。
 */
export const columnsFieldConfig: any[] = [
  {
    title: '',
    dataIndex: 'dragSort',
    key: 'dragSort',
    align: 'center',
    width: 32,
  },
  {
    title: '#',
    dataIndex: '',
    key: 'rowIndex',
    align: 'center',
    width: 40,
    customRender:
      ({ index }: { index: number }) => index + 1,
  },
  {
    title: '字段配置',
    key: 'fieldConfigGroup',
    children: [
      {
        title: '字段名',
        dataIndex: 'columnName',
        align: 'left',
        width: 170,
      },
      {
        title: '字段注释',
        dataIndex: 'columnComment',
        align: 'left',
        width: 190,
      },
      {
        title: '类型',
        dataIndex: 'columnType',
        align: 'left',
        width: 96,
      },
      {
        title: '长度',
        dataIndex: 'columnLength',
        align: 'left',
        width: 66,
      },
      {
        title: '精度',
        dataIndex: 'columnScale',
        align: 'left',
        width: 66,
      },
      {
        title: '默认值',
        dataIndex: 'columnDefault',
        align: 'left',
        width: 86,
      },
      {
        title: '主键',
        dataIndex: 'columnIsPk',
        align: 'center',
        width: 48,
      },
      {
        title: '可空',
        dataIndex: 'columnIsNullable',
        align: 'center',
        width: 52,
      },
    ],
  },
  {
    title: '操作',
    dataIndex: 'operation',
    align: 'center',
    width: 110,
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
    customRender: ({ index }: { index: number }) => index + 1,
  },
  {
    title: '表名',
    dataIndex: 'tableName',
    align: 'left',
    width: 160,
  },
  {
    title: '表注释',
    dataIndex: 'tableComment',
    align: 'left',
  },
  {
    title: '模块名',
    dataIndex: 'moduleName',
    align: 'center',
    width: 120,
  },
  {
    title: '包名',
    dataIndex: 'packageName',
    align: 'center',
    width: 120,
  },
  {
    title: '操作',
    dataIndex: 'operation',
    align: 'center',
    width: 180,
  },
];

/**
 * 「是否创建表 = 是」时的默认字段模板。
 * 列名必须是 snake_case（与真实建表后的列名、实体字段映射一致），
 * 主键 id + 审计字段 + del_flag 与本项目现有表保持一致。
 */
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
    uiIsQuery: false,
    uiIsEdit: false,
    uiIsRequired: false,
    uiIsList: false,
  },
  {
    id: '',
    tableId: '',
    columnName: 'create_by',
    columnComment: '创建人',
    columnType: 'varchar',
    columnLength: '32',
    columnScale: '',
    columnDefault: '',
    columnIsPk: false,
    columnIsNullable: true,
    uiIsQuery: false,
    uiIsEdit: false,
    uiIsRequired: false,
    uiIsList: false,
  },
  {
    id: '',
    tableId: '',
    columnName: 'create_time',
    columnComment: '创建时间',
    columnType: 'datetime',
    columnLength: '',
    columnScale: '',
    columnDefault: 'CURRENT_TIMESTAMP',
    columnIsPk: false,
    columnIsNullable: true,
    uiIsQuery: false,
    uiIsEdit: false,
    uiIsRequired: false,
    uiIsList: false,
  },
  {
    id: '',
    tableId: '',
    columnName: 'update_by',
    columnComment: '更新人',
    columnType: 'varchar',
    columnLength: '32',
    columnScale: '',
    columnDefault: '',
    columnIsPk: false,
    columnIsNullable: true,
    uiIsQuery: false,
    uiIsEdit: false,
    uiIsRequired: false,
    uiIsList: false,
  },
  {
    id: '',
    tableId: '',
    columnName: 'update_time',
    columnComment: '更新时间',
    columnType: 'datetime',
    columnLength: '',
    columnScale: '',
    columnDefault: '',
    columnIsPk: false,
    columnIsNullable: true,
    uiIsQuery: false,
    uiIsEdit: false,
    uiIsRequired: false,
    uiIsList: false,
  },
  {
    id: '',
    tableId: '',
    columnName: 'del_flag',
    columnComment: '删除状态（0正常，1已删除）',
    columnType: 'tinyint',
    columnLength: '1',
    columnScale: '',
    columnDefault: '0',
    columnIsPk: false,
    columnIsNullable: true,
    uiIsQuery: false,
    uiIsEdit: false,
    uiIsRequired: false,
    uiIsList: false,
  },
];