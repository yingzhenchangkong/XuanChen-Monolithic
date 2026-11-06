import { reactive } from 'vue';

/** 查询参数 */
export const queryParams = reactive({
  profileName: '',
});

/** 查询配置 */
export const queryFormItems = reactive([
  {
    name: 'profileName',
    label: '预设名称',
    type: 'input' as const,
    placeholder: '请输入预设名称',
  },
]);

/** 定义表格的列 列表页 */
export const columnsIndex = [
  {
    title: '#',
    dataIndex: '',
    key: 'rowIndex',
    align: 'center',
    width: 60,
    customRender: ({ index }: { index: number }) => index + 1,
  },
  {
    title: '预设名称',
    dataIndex: 'profileName',
    align: 'left',
    width: 160,
  },
  {
    title: '数据源',
    dataIndex: 'databaseId',
    align: 'left',
    width: 160,
  },
  {
    title: '输出目录',
    dataIndex: 'outputDir',
    align: 'left',
  },
  {
    title: '后端相对路径',
    dataIndex: 'backendRelPath',
    align: 'left',
  },
  {
    title: '前端相对路径',
    dataIndex: 'frontendRelPath',
    align: 'left',
  },
  {
    title: '包名',
    dataIndex: 'packageName',
    align: 'left',
    width: 120,
  },
  {
    title: '模块名',
    dataIndex: 'moduleName',
    align: 'left',
    width: 120,
  },
  {
    title: '模板',
    dataIndex: 'template',
    align: 'center',
    width: 110,
  },
  {
    title: '排序码',
    dataIndex: 'orderNo',
    align: 'center',
    width: 80,
  },
  {
    title: '操作',
    dataIndex: 'operation',
    align: 'center',
    width: 145,
  },
];
