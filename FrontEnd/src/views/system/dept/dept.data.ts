
/**
 * 部门传输数据接口
 * 用于定义部门数据传输时的结构
 */
export interface DeptTranData {
  selectedKey: string;
  ifAddChild: boolean;
}

/**
 * 部门用户表格列字段
 */
export const columnsDeptUser = [
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