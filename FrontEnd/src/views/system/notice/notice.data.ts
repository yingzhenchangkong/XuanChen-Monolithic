import { reactive } from 'vue';

/** 查询参数(通知列表) */
export const queryParamsIndex = reactive({
  title: '',
  readStatus: undefined,
})
/** 查询配置(通知列表) */
export const queryFormItemsIndex = reactive([
  {
    name: 'title',
    label: '通知标题',
    type: 'input' as const,
    placeholder: '请输入通知标题'
  },
  {
    name: 'readStatus',
    label: '是否已读',
    type: 'select' as const,
    placeholder: '请选择是否已读',
    width: '180px',
    options: [],
    fieldNames: {
      label: 'dictItemText',
      value: 'dictItemValue'
    }
  }
]);

/** 定义表格的列(通知列表) */
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
    title: '通知标题',
    dataIndex: 'title',
    align: 'left',
  },
  {
    title: '发布人',
    dataIndex: 'createName',
    align: 'center',
    width: 150
  },
  {
    title: '发布时间',
    dataIndex: 'createTime',
    align: 'center',
    width: 150
  },
  {
    title: '状态',
    dataIndex: 'readStatusName',
    align: 'center',
    width: 100,
  },
  {
    title: '阅读时间',
    dataIndex: 'readTime',
    align: 'center',
    width: 150,
  },
  {
    title: '操作',
    dataIndex: 'operation',
    align: 'center',
    width: 120
  },
];

/** 查询参数(通知管理 抬头) */
export const queryParamsManage = reactive({
  title: '',
})
/** 查询配置(通知管理 抬头) */
export const queryFormItemsManage = reactive([
  {
    name: 'title',
    label: '通知标题',
    type: 'input' as const,
    placeholder: '请输入通知标题'
  },
]);
/** 定义表格的列(通知管理 抬头) */
export const columnsManage = [
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
    title: '通知标题',
    dataIndex: 'title',
    align: 'left',
  },
  {
    title: '发布人',
    dataIndex: 'createName',
    align: 'center',
    width: 150
  },
  {
    title: '发布时间',
    dataIndex: 'createTime',
    align: 'center',
    width: 150
  },
  {
    title: '状态',
    dataIndex: 'statusName',
    align: 'center',
    width: 100,
  },
  {
    title: '操作',
    dataIndex: 'operation',
    align: 'center',
    width: 200
  },
];

/** 查询参数(通知管理 状态) */
export const queryParamsStatus = reactive({
  noticeId: '',
})
/** 定义表格的列(通知管理 状态) */
export const columnsStatus = [
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
    title: '接收人',
    dataIndex: 'userName',
    align: 'left',
  },
  {
    title: '状态',
    dataIndex: 'readStatusName',
    align: 'center',
    width: 100,
  },
  {
    title: '阅读时间',
    dataIndex: 'readTime',
    align: 'center',
    width: 150,
  },
];