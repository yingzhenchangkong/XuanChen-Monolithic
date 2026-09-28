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
    width: 150
  },
  {
    title: '手机号',
    dataIndex: 'mobile',
    align: 'left',
    width: 150
  },
  {
    title: '登录 IP',
    dataIndex: 'ip',
    align: 'left',
    width: 150
  },
  {
    title: '终端类型',
    dataIndex: 'deviceType',
    align: 'left',
    width: 100
  },
  {
    title: '邮箱',
    dataIndex: 'email',
    align: 'left'
  },
  {
    title: '操作',
    dataIndex: 'operation',
    align: 'center',
    width: 150
  },
];