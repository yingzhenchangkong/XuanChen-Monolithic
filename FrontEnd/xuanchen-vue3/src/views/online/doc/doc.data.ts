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

/** 文件扩展名 → 标签颜色 */
export const extColor = (ext: string) => {
  const map: Record<string, string> = { docx: 'blue', doc: 'blue', xlsx: 'green', xls: 'green', pptx: 'orange', ppt: 'orange' };
  return map[ext] ?? 'default';
};

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
  { title: '文档名称', dataIndex: 'originalName', align: 'left', ellipsis: true },
  { title: '类型', dataIndex: 'ext', align: 'center', width: 90 },
  { title: '我的角色', dataIndex: 'role', align: 'center', width: 100 },
  { title: '协作权限', dataIndex: 'permission', align: 'center', width: 100 },
  { title: '属主', dataIndex: 'username', align: 'center', width: 130 },
  { title: '更新时间', dataIndex: 'updateTime', align: 'center', width: 170 },
  { title: '操作', dataIndex: 'operation', align: 'center', width: 300, fixed: 'right' as const },
];
