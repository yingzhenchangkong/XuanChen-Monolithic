/**
 * 部门人员表格列字段（数据来源人员表 sys_person）
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
    title: '人员编码',
    dataIndex: 'personCode',
    align: 'left',
  },
  {
    title: '姓名',
    dataIndex: 'realName',
    align: 'left',
  },
  {
    title: '操作',
    dataIndex: 'operation',
    align: 'center',
    width: 125
  },
];