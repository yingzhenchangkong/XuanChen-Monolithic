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
    title: '描述',
    dataIndex: 'description',
    align: 'left',
    width: 150
  },
  {
    title: '值',
    dataIndex: 'value',
    align: 'left',
  },
];