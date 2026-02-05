import { reactive } from 'vue';
/** 查询参数 */
export const queryParams = reactive({
  userName: '',
  loginTimeStart: '',
  loginTimeEnd: '',
  status: '',
})
/** 查询参数 */
export const queryFormItems = [
  {
    name: 'userName',
    label: '用户名',
    type: 'input' as const,
    placeholder: '请输入用户名'
  },
  {
    name: 'loginTimeStart',
    label: '登录时间(起)',
    type: 'input' as const,
    placeholder: '请输入登录时间(起)'
  },
  {
    name: 'loginTimeEnd',
    label: '登录时间(止)',
    type: 'input' as const,
    placeholder: '请输入登录时间(止)'
  },
  {
    name: 'status',
    label: '登录状态',
    type: 'select' as const,
    placeholder: '请选择登录状态',
    options: [
      { label: '成功', value: '1' },
      { label: '失败', value: '0' },
    ]
  }
];

/** 定义表格的列 列表页*/
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
    title: '用户名',
    dataIndex: 'userName_dictText',
    align: 'left',
    width: 100
  },

  {
    title: '登录时间',
    dataIndex: 'loginTime',
    align: 'left',
    width: 150
  },
  {
    title: 'IP地址',
    dataIndex: 'ipAddress',
    align: 'left',
    width: 150
  },
  {
    title: '登录状态',
    dataIndex: 'status',
    align: 'center',
    width: 100
  },
  {
    title: '描述信息',
    dataIndex: 'description',
    align: 'left',
  }
];