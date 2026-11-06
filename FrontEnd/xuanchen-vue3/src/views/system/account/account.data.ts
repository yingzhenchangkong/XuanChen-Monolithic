import { reactive } from 'vue';

/**
 * 账号管理只查询 sys_account 自身的列（用户名 + 状态）。
 * 昵称/头像/手机号/邮箱属账号（登录凭证 + 展示身份），在账号管理中维护；
 * 人员管理只维护 sys_person（姓名/证件/学历/档案联系方式等）。
 */

/** 查询参数 */
export const queryParams = reactive({
  userName: ''
})

/** 查询配置 */
export const queryFormItems = reactive([
  {
    name: 'userName',
    label: '用户名',
    type: 'input' as const,
    placeholder: '请输入用户名'
  },
]);

/** 定义表格的列（personName 为只读展示的绑定人员姓名） */
export const columns = [
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
    title: '用户名',
    dataIndex: 'userName',
    align: 'left',
    width: 150
  },
  {
    title: '昵称',
    dataIndex: 'nickName',
    align: 'left',
    width: 130
  },
  {
    title: '头像',
    dataIndex: 'avatar',
    align: 'center',
    width: 80
  },
  {
    title: '手机号',
    dataIndex: 'mobile',
    align: 'left',
    width: 125
  },
  {
    title: '邮箱',
    dataIndex: 'email',
    align: 'left',
    width: 200,
    ellipsis: true
  },
  {
    title: '绑定人员',
    dataIndex: 'personName',
    align: 'left',
    width: 120,
  },
  {
    title: '账号状态',
    dataIndex: 'status',
    align: 'center',
    width: 90,
  },
  {
    title: '操作',
    dataIndex: 'operation',
    align: 'center',
    width: 240,
    fixed: 'right' as const
  },
];

/** 回收站查询配置 */
export const queryFormItemsRcvBin = reactive([
  {
    name: 'userName',
    label: '用户名',
    type: 'input' as const,
    placeholder: '请输入用户名'
  },
]);

/** 回收站表格的列 */
export const columnsRcvBin = [
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
    title: '用户名',
    dataIndex: 'userName',
    align: 'left',
    width: 160
  },
  {
    title: '昵称',
    dataIndex: 'nickName',
    align: 'left',
    width: 140
  },
  {
    title: '绑定人员',
    dataIndex: 'personName',
    align: 'left',
    width: 120
  },
  {
    title: '操作',
    dataIndex: 'operation',
    align: 'center',
    width: 240
  },
];
