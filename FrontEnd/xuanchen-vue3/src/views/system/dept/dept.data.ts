/**
 * 部门用户表格列字段
 */
export const columnsDeptUser = [
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
  },
  {
    title: '昵称',
    dataIndex: 'nickName',
    align: 'left',
  },
  {
    title: '操作',
    dataIndex: 'operation',
    align: 'center',
    width: 125
  },
];