import { reactive } from 'vue';

/** 查询参数 */
export const queryParams = reactive({
  userName: '',
  nickName: '',
  mobile: ''
})

/** 查询配置 */
export const queryFormItems = [
  {
    name: 'userName',
    label: '用户名',
    type: 'input' as const,
    placeholder: '请输入用户名'
  },
  {
    name: 'nickName',
    label: '昵称',
    type: 'input' as const,
    placeholder: '请输入昵称'
  },
  {
    name: 'mobile',
    label: '手机号',
    type: 'input' as const,
    placeholder: '请输入手机号'
  },
];

/** 定义表格的列 */
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
    title: '头像',
    dataIndex: 'avatar',
    align: 'center',
    width: 100
  },
  {
    title: '用户名',
    dataIndex: 'userName',
    align: 'left',
    width: 140
  },
  {
    title: '昵称',
    dataIndex: 'nickName',
    align: 'left',
    width: 140
  },
  {
    title: '手机号',
    dataIndex: 'mobile',
    align: 'left',
    width: 115
  },
  {
    title: '邮箱',
    dataIndex: 'email',
    align: 'left',
  },
  {
    title: '账号状态',
    dataIndex: 'status',
    align: 'center',
    width: 80,
  },
  {
    title: '操作',
    dataIndex: 'operation',
    align: 'center',
    width: 240
  },
];

/** 查询配置 */
export const queryFormItemsRcvBin = [
  {
    name: 'userName',
    label: '用户名',
    type: 'input' as const,
    placeholder: '请输入用户名'
  },
  {
    name: 'nickName',
    label: '昵称',
    type: 'input' as const,
    placeholder: '请输入昵称'
  },
];

/** 定义表格的列 */
export const columnsRcvBin = [
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
    width: 110
  },
  {
    title: '昵称',
    dataIndex: 'nickName',
    align: 'center',
    width: 150
  },
  {
    title: '手机号',
    dataIndex: 'mobile',
    align: 'center',
  },
  {
    title: '邮箱',
    dataIndex: 'email',
    align: 'center',
  },
  {
    title: '操作',
    dataIndex: 'operation',
    align: 'center',
    width: 240
  },
];