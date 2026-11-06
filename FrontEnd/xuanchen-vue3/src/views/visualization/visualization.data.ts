import { reactive } from 'vue';
import type { ScreenConfig } from './visualization.types';

/** 大屏状态：0草稿 1已发布 2停用 */
export const screenStatusOptions = [
  { label: '草稿', value: 0 },
  { label: '已发布', value: 1 },
  { label: '停用', value: 2 },
];

export const screenStatusText = (status?: number) =>
  ({ 0: '草稿', 1: '已发布', 2: '停用' } as Record<number, string>)[Number(status)] ?? '未知';

export const screenStatusColor = (status?: number) =>
  ({ 0: 'default', 1: 'green', 2: 'volcano' } as Record<number, string>)[Number(status)] ?? 'default';

/** 查询参数（与 XCListPage 共用同一份 reactive 对象） */
export const queryParams = reactive<Record<string, unknown>>({
  screenName: '',
  screenCode: '',
  status: undefined as number | undefined,
  pageNo: 1,
  pageSize: 10,
});

/** 查询配置 */
export const queryFormItems = reactive([
  { name: 'screenName', label: '大屏名称', type: 'input' as const, placeholder: '请输入大屏名称' },
  { name: 'screenCode', label: '大屏编码', type: 'input' as const, placeholder: '请输入大屏编码' },
  { name: 'status', label: '状态', type: 'select' as const, placeholder: '请选择状态', options: screenStatusOptions },
]);

/** 列表列定义 */
export const columns = [
  {
    title: '#',
    dataIndex: '',
    key: 'rowIndex',
    align: 'center',
    width: 60,
    customRender: ({ index }: { index: number }) => index + 1,
  },
  { title: '大屏编码', dataIndex: 'screenCode', align: 'left', width: 180 },
  { title: '大屏名称', dataIndex: 'screenName', align: 'left', width: 200 },
  { title: '状态', dataIndex: 'status', align: 'center', width: 90 },
  { title: '发布时间', dataIndex: 'publishTime', align: 'center', width: 160 },
  { title: '备注', dataIndex: 'remark', align: 'left', ellipsis: true },
  { title: '操作', dataIndex: 'operation', align: 'center', width: 350, fixed: 'right' as const },
];

/** 默认画布：1920x1080 是行业通用大屏分辨率，等比缩放到容器 */
export const DEFAULT_CANVAS: ScreenConfig['canvas'] = {
  width: 1920,
  height: 1080,
  background: '#0b1020',
  backgroundImage: '',
  scaleMode: 'fit',
  showGrid: true,
};

/** 新建大屏时的空白配置 */
export const createDefaultConfig = (): ScreenConfig => ({
  canvas: { ...DEFAULT_CANVAS },
  widgets: [],
});

/** 解析配置 JSON，容错为空/损坏时回退默认配置；同时补齐历史配置缺失的字段 */
export const parseScreenConfig = (configJson?: string): ScreenConfig => {
  if (!configJson) {
    return createDefaultConfig();
  }
  try {
    const parsed = JSON.parse(configJson) as ScreenConfig;
    const widgets = Array.isArray(parsed?.widgets) ? parsed.widgets : [];
    return {
      canvas: { ...DEFAULT_CANVAS, ...(parsed?.canvas ?? {}) },
      // 老配置可能缺 data/mapping/props：补齐后再交给画布与属性面板，避免各处判空
      widgets: widgets.map((widget) => ({
        ...widget,
        props: widget.props ?? {},
        data: {
          mode: widget.data?.mode ?? 'static',
          datasetKey: widget.data?.datasetKey ?? '',
          staticRows: widget.data?.staticRows ?? [],
          mapping: widget.data?.mapping ?? {},
        },
      })),
    };
  } catch {
    return createDefaultConfig();
  }
};
