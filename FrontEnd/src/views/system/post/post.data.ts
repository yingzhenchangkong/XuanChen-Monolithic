import { reactive } from 'vue';

/** 查询参数 */
export const queryParams = reactive({
  postName: '',
  postDescription: '',
})
/** 查询配置 */
export const queryFormItems = [
  {
    name: 'postName',
    label: '岗位名称',
    type: 'input' as const,
    placeholder: '请输入岗位名称'
  },
  {
    name: 'postDescription',
    label: '岗位描述',
    type: 'input' as const,
    placeholder: '请输入岗位描述'
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
    title: '岗位编码',
    dataIndex: 'postCode',
    align: 'left',
    width: 110
  },
  {
    title: '岗位名称',
    dataIndex: 'postName',
    align: 'left',
    width: 150
  },
  {
    title: '岗位描述',
    dataIndex: 'postDescription',
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
    width: 220
  },
];
/** 定义表格的列 回收站*/
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
    title: '岗位编码',
    dataIndex: 'postCode',
    align: 'left',
    width: 110
  },
  {
    title: '岗位名称',
    dataIndex: 'postName',
    align: 'left',
    width: 150
  },
  {
    title: '岗位描述',
    dataIndex: 'postDescription',
    align: 'left',
  },
  {
    title: '操作',
    dataIndex: 'operation',
    align: 'center',
    width: 180
  },
];

/** 定义表格的列 分配用户*/
export const columnsAssignUser = [
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
    title: '用户名',
    dataIndex: 'userName',
    align: 'center',
    width: 140
  },
  {
    title: '昵称',
    dataIndex: 'nickName',
    align: 'center',
    width: 140
  },
  {
    title: '操作',
    dataIndex: 'operation',
    align: 'center',
    width: 240
  },
];