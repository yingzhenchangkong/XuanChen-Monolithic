import { reactive } from 'vue';

/** 人员类型映射（与 sys_dict person_type 一致） */
export const personTypeMap: Record<number, { text: string; color: string }> = {
  1: { text: '正式员工', color: 'green' },
  2: { text: '外协', color: 'blue' },
  3: { text: '临时人员', color: 'orange' },
};

/** 人员状态映射（与 sys_dict person_status 一致） */
export const personStatusMap: Record<number, { text: string; color: string }> = {
  1: { text: '在职', color: 'green' },
  2: { text: '试用', color: 'blue' },
  3: { text: '离职', color: 'volcano' },
  4: { text: '退休', color: 'default' },
};

/** 性别映射 */
export const genderMap: Record<number, string> = {
  1: '男',
  2: '女',
};

/** 婚姻状况映射 */
export const maritalMap: Record<number, string> = {
  1: '未婚',
  2: '已婚',
  3: '离异',
  4: '丧偶',
};

/** 学历映射 */
export const educationMap: Record<number, string> = {
  1: '小学',
  2: '初中',
  3: '高中',
  4: '大专',
  5: '本科',
  6: '研究生',
};

/** 性别选项 */
export const genderOptions = [
  { label: '男', value: 1 },
  { label: '女', value: 2 },
];

/** 婚姻状况选项 */
export const maritalOptions = [
  { label: '未婚', value: 1 },
  { label: '已婚', value: 2 },
  { label: '离异', value: 3 },
  { label: '丧偶', value: 4 },
];

/** 学历选项 */
export const educationOptions = [
  { label: '小学', value: 1 },
  { label: '初中', value: 2 },
  { label: '高中', value: 3 },
  { label: '大专', value: 4 },
  { label: '本科', value: 5 },
  { label: '研究生', value: 6 },
];

/** 人员类型选项 */
export const personTypeOptions = [
  { label: '正式员工', value: 1 },
  { label: '外协', value: 2 },
  { label: '临时人员', value: 3 },
];

/** 人员状态选项 */
export const personStatusOptions = [
  { label: '在职', value: 1 },
  { label: '试用', value: 2 },
  { label: '离职', value: 3 },
  { label: '退休', value: 4 },
];

/** 查询参数 */
export const queryParams = reactive({
  realName: '',
  mobile: '',
  personCode: '',
  personType: undefined as string | number | undefined,
  status: undefined as string | number | undefined,
})

/** 查询配置 */
export const queryFormItems = reactive([
  {
    name: 'realName',
    label: '姓名',
    type: 'input' as const,
    placeholder: '请输入姓名'
  },
  {
    name: 'personCode',
    label: '人员编码',
    type: 'input' as const,
    placeholder: '请输入人员编码'
  },
  {
    name: 'mobile',
    label: '手机号',
    type: 'input' as const,
    placeholder: '请输入手机号'
  },
]);

/** 定义表格的列 */
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
    title: '姓名',
    dataIndex: 'realName',
    align: 'left',
    width: 110
  },
  {
    title: '照片',
    dataIndex: 'photo',
    align: 'center',
    width: 80
  },
  {
    title: '性别',
    dataIndex: 'gender',
    align: 'center',
    width: 70
  },
  {
    title: '人员编码',
    dataIndex: 'personCode',
    align: 'left',
    width: 110
  },
  {
    title: '部门',
    dataIndex: 'deptNames',
    align: 'left',
    width: 180,
    ellipsis: true
  },
  {
    title: '岗位',
    dataIndex: 'postNames',
    align: 'left',
    width: 160,
    ellipsis: true
  },
  {
    title: '手机号',
    dataIndex: 'mobile',
    align: 'left',
    width: 125
  },
  {
    title: '人员类型',
    dataIndex: 'personType',
    align: 'center',
    width: 100
  },
  {
    title: '人员状态',
    dataIndex: 'status',
    align: 'center',
    width: 95
  },
  {
    title: '操作',
    dataIndex: 'operation',
    align: 'center',
    width: 210,
    // 固定在表格右侧，横向滚动时始终可见
    fixed: 'right' as const
  },
];

/** 回收站查询配置 */
export const queryFormItemsRcvBin = reactive([
  {
    name: 'realName',
    label: '姓名',
    type: 'input' as const,
    placeholder: '请输入姓名'
  },
  {
    name: 'mobile',
    label: '手机号',
    type: 'input' as const,
    placeholder: '请输入手机号'
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
    title: '姓名',
    dataIndex: 'realName',
    align: 'left',
    width: 120
  },
  {
    title: '人员编码',
    dataIndex: 'personCode',
    align: 'left',
    width: 120
  },
  {
    title: '手机号',
    dataIndex: 'mobile',
    align: 'left',
    width: 130
  },
  {
    title: '操作',
    dataIndex: 'operation',
    align: 'center',
    width: 220
  },
];
