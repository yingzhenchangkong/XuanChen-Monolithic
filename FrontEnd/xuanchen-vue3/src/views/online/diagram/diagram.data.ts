import { reactive } from 'vue';

/** 查询参数 */
export const queryParams = reactive({
  keyword: '',
  pageNo: 1,
  pageSize: 10,
})

/** 查询配置 */
export const queryFormItems = reactive([
  {
    name: 'keyword',
    label: '名称',
    type: 'input' as const,
    placeholder: '按文档名称搜索'
  },
]);

/** 定义表格的列 列表页*/
export const columnsIndex = [
  {
    title: '#',
    dataIndex: '',
    key: 'rowIndex',
    align: 'center',
    width: 60,
    customRender: ({ index }: { index: number }) => index + 1,
  },
  { title: '文档名称', dataIndex: 'docName', align: 'left', ellipsis: true },
  { title: '创建人', dataIndex: 'createBy', align: 'center', width: 140 },
  { title: '创建时间', dataIndex: 'createTime', align: 'center', width: 170 },
  { title: '更新时间', dataIndex: 'updateTime', align: 'center', width: 170 },
  { title: '操作', dataIndex: 'operation', align: 'center', width: 140, fixed: 'right' as const },
];
