<template>
  <!-- 右侧属性面板：未选中组件时编辑画布，选中后编辑该组件的布局/样式/数据 -->
  <div class="bs-property">
    <template v-if="!widget">
      <div class="bs-panel-title">画布设置</div>
      <a-form layout="vertical" size="small" class="bs-form">
        <a-form-item label="设计分辨率">
          <a-space>
            <a-input-number v-model:value="canvas.width" :min="640" :max="7680" :step="10" style="width: 96px"
              @change="emit('change')" />
            <span class="bs-mul">×</span>
            <a-input-number v-model:value="canvas.height" :min="360" :max="4320" :step="10" style="width: 96px"
              @change="emit('change')" />
          </a-space>
        </a-form-item>
        <a-form-item label="常用分辨率">
          <a-space wrap>
            <a-tag v-for="preset in presets" :key="preset.label" class="bs-tag" @click="applyPreset(preset)">
              {{ preset.label }}
            </a-tag>
          </a-space>
        </a-form-item>
        <a-form-item label="背景色">
          <a-input v-model:value="canvas.background" placeholder="#0b1020" @change="emit('change')" />
        </a-form-item>
        <a-form-item label="背景图地址">
          <a-input v-model:value="canvas.backgroundImage" placeholder="留空则使用纯色背景（相对路径或外链）"
            allow-clear @change="emit('change')" />
        </a-form-item>
        <a-form-item label="缩放方式">
          <a-radio-group v-model:value="canvas.scaleMode" button-style="solid" size="small" @change="emit('change')">
            <a-radio-button value="fit">等比留边</a-radio-button>
            <a-radio-button value="full">拉伸铺满</a-radio-button>
          </a-radio-group>
        </a-form-item>
        <a-form-item label="设计网格">
          <a-switch v-model:checked="canvas.showGrid" checked-children="显示" un-checked-children="隐藏"
            @change="emit('change')" />
        </a-form-item>
      </a-form>
      <div class="bs-empty-tip">选中画布中的组件后，可在此编辑位置、样式与数据绑定。</div>
    </template>

    <template v-else>
      <div class="bs-property-head">
        <span class="bs-panel-title">{{ widget.name }}</span>
        <a-space :size="2">
          <a-tooltip title="上移一层">
            <a-button type="text" size="small" @click="emit('to-front')">
              <template #icon>
                <VerticalAlignTopOutlined />
              </template>
            </a-button>
          </a-tooltip>
          <a-tooltip title="下移一层">
            <a-button type="text" size="small" @click="emit('to-back')">
              <template #icon>
                <VerticalAlignBottomOutlined />
              </template>
            </a-button>
          </a-tooltip>
          <a-tooltip title="删除组件">
            <a-button type="text" size="small" danger @click="emit('delete')">
              <template #icon>
                <DeleteOutlined />
              </template>
            </a-button>
          </a-tooltip>
        </a-space>
      </div>

      <a-tabs v-model:activeKey="activeTab" size="small" class="bs-tabs">
        <!-- 布局 -->
        <a-tab-pane key="layout" tab="布局">
          <a-form layout="vertical" size="small" class="bs-form">
            <a-form-item label="组件名称">
              <a-input v-model:value="widget.name" @change="emit('change')" />
            </a-form-item>
            <a-row :gutter="8">
              <a-col :span="12">
                <a-form-item label="X">
                  <a-input-number v-model:value="widget.x" :min="0" style="width: 100%" @change="emit('change')" />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="Y">
                  <a-input-number v-model:value="widget.y" :min="0" style="width: 100%" @change="emit('change')" />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="宽度">
                  <a-input-number v-model:value="widget.w" :min="20" style="width: 100%" @change="emit('change')" />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="高度">
                  <a-input-number v-model:value="widget.h" :min="20" style="width: 100%" @change="emit('change')" />
                </a-form-item>
              </a-col>
            </a-row>
            <a-form-item label="层级">
              <a-input-number v-model:value="widget.zIndex" :min="1" style="width: 100%" @change="emit('change')" />
            </a-form-item>
          </a-form>
        </a-tab-pane>

        <!-- 样式 -->
        <a-tab-pane key="style" tab="样式">
          <a-form layout="vertical" size="small" class="bs-form">
            <template v-if="widget.type === 'text'">
              <a-form-item label="文本内容">
                <a-textarea v-model:value="widget.props.text" :rows="4" @change="emit('change')" />
              </a-form-item>
              <a-row :gutter="8">
                <a-col :span="12">
                  <a-form-item label="字号">
                    <a-input-number v-model:value="widget.props.fontSize" :min="10" :max="120" style="width: 100%"
                      @change="emit('change')" />
                  </a-form-item>
                </a-col>
                <a-col :span="12">
                  <a-form-item label="字间距">
                    <a-input-number v-model:value="widget.props.letterSpacing" :min="0" :max="20" style="width: 100%"
                      @change="emit('change')" />
                  </a-form-item>
                </a-col>
              </a-row>
              <a-form-item label="对齐">
                <a-radio-group v-model:value="widget.props.align" size="small" @change="emit('change')">
                  <a-radio-button value="left">左</a-radio-button>
                  <a-radio-button value="center">中</a-radio-button>
                  <a-radio-button value="right">右</a-radio-button>
                </a-radio-group>
              </a-form-item>
              <a-form-item label="字重">
                <a-select v-model:value="widget.props.fontWeight" :options="fontWeightOptions" @change="emit('change')" />
              </a-form-item>
            </template>

            <template v-if="widget.type === 'number'">
              <a-form-item label="指标标题">
                <a-input v-model:value="widget.props.title" @change="emit('change')" />
              </a-form-item>
              <a-row :gutter="8">
                <a-col :span="12">
                  <a-form-item label="数值字号">
                    <a-input-number v-model:value="widget.props.fontSize" :min="12" :max="120" style="width: 100%"
                      @change="emit('change')" />
                  </a-form-item>
                </a-col>
                <a-col :span="12">
                  <a-form-item label="小数位">
                    <a-input-number v-model:value="widget.props.precision" :min="0" :max="6" style="width: 100%"
                      @change="emit('change')" />
                  </a-form-item>
                </a-col>
              </a-row>
              <a-form-item label="单位">
                <a-input v-model:value="widget.props.unit" placeholder="如 万元 / %" @change="emit('change')" />
              </a-form-item>
              <a-form-item label="数值颜色">
                <a-input v-model:value="widget.props.color" @change="emit('change')" />
              </a-form-item>
            </template>

            <template v-if="isChart">
              <a-form-item label="图表标题">
                <a-input v-model:value="widget.props.title" @change="emit('change')" />
              </a-form-item>
              <a-form-item label="主色（逗号分隔可配多系列）">
                <a-input v-model:value="widget.props.color" placeholder="#40a9ff,#36cfc9" @change="emit('change')" />
              </a-form-item>
              <a-form-item label="显示图例">
                <a-switch v-model:checked="widget.props.showLegend" @change="emit('change')" />
              </a-form-item>
              <template v-if="widget.type === 'line'">
                <a-form-item label="平滑曲线">
                  <a-switch v-model:checked="widget.props.smooth" @change="emit('change')" />
                </a-form-item>
                <a-form-item label="面积填充">
                  <a-switch v-model:checked="widget.props.area" @change="emit('change')" />
                </a-form-item>
              </template>
              <a-form-item v-if="widget.type === 'pie'" label="南丁格尔玫瑰图">
                <a-switch v-model:checked="widget.props.roseType" @change="emit('change')" />
              </a-form-item>
            </template>

            <template v-if="widget.type === 'table'">
              <a-form-item label="表格标题">
                <a-input v-model:value="widget.props.title" @change="emit('change')" />
              </a-form-item>
              <a-row :gutter="8">
                <a-col :span="12">
                  <a-form-item label="字号">
                    <a-input-number v-model:value="widget.props.fontSize" :min="10" :max="28" style="width: 100%"
                      @change="emit('change')" />
                  </a-form-item>
                </a-col>
                <a-col :span="12">
                  <a-form-item label="最多行数">
                    <a-input-number v-model:value="widget.props.maxRows" :min="1" :max="50" style="width: 100%"
                      @change="emit('change')" />
                  </a-form-item>
                </a-col>
              </a-row>
              <a-form-item label="表头背景">
                <a-input v-model:value="widget.props.headerBg" @change="emit('change')" />
              </a-form-item>
              <a-form-item label="斑马纹">
                <a-switch v-model:checked="widget.props.striped" @change="emit('change')" />
              </a-form-item>
            </template>

            <template v-if="widget.type === 'image'">
              <a-form-item label="图片地址">
                <a-input v-model:value="widget.props.src" placeholder="相对路径（visualization/..）或外链"
                  allow-clear @change="emit('change')" />
              </a-form-item>
              <a-form-item label="填充方式">
                <a-select v-model:value="widget.props.fit" :options="fitOptions" @change="emit('change')" />
              </a-form-item>
            </template>

            <template v-if="widget.type === 'border'">
              <a-form-item label="分组标题">
                <a-input v-model:value="widget.props.title" @change="emit('change')" />
              </a-form-item>
              <a-form-item label="边框颜色">
                <a-input v-model:value="widget.props.borderColor" @change="emit('change')" />
              </a-form-item>
              <a-form-item label="标题颜色">
                <a-input v-model:value="widget.props.color" @change="emit('change')" />
              </a-form-item>
            </template>

            <a-form-item label="背景（支持 rgba）">
              <a-input v-model:value="widget.props.background" placeholder="transparent" allow-clear
                @change="emit('change')" />
            </a-form-item>
            <a-form-item label="圆角">
              <a-input-number v-model:value="widget.props.radius" :min="0" :max="80" style="width: 100%"
                @change="emit('change')" />
            </a-form-item>
          </a-form>
        </a-tab-pane>

        <!-- 数据 -->
        <a-tab-pane v-if="isDataWidget" key="data" tab="数据">
          <a-form layout="vertical" size="small" class="bs-form">
            <a-form-item label="数据来源">
              <a-radio-group v-model:value="widget.data.mode" size="small" @change="emit('change')">
                <a-radio-button value="dataset">数据集</a-radio-button>
                <a-radio-button value="static">静态数据</a-radio-button>
              </a-radio-group>
            </a-form-item>

            <template v-if="widget.data.mode === 'dataset'">
              <a-form-item label="数据集">
                <a-select v-model:value="widget.data.datasetKey" :options="datasetOptions" style="width: 100%"
                  placeholder="请选择数据集" allow-clear @change="emit('change')" />
              </a-form-item>
              <a-form-item>
                <a-space>
                  <a-button size="small" :loading="loadingKey === widget.data.datasetKey"
                    :disabled="!widget.data.datasetKey" @click="emit('load-data', widget.data.datasetKey)">
                    <template #icon>
                      <ThunderboltOutlined />
                    </template>加载预览数据
                  </a-button>
                  <a-button size="small" type="link" @click="emit('manage-dataset')">管理数据集</a-button>
                </a-space>
              </a-form-item>
              <a-alert v-if="currentColumns.length === 0" type="info" show-icon
                message="尚未加载字段：先点「加载预览数据」，再选择字段映射" class="bs-alert" />
              <a-form-item v-if="widget.type !== 'pie'" label="类目字段">
                <a-select v-model:value="mapping.x" :options="columnOptions" allow-clear
                  placeholder="如 stat_date" @change="emit('change')" />
              </a-form-item>
              <a-form-item v-if="widget.type !== 'pie'" label="数值字段">
                <a-select v-model:value="mapping.y" :options="columnOptions" allow-clear
                  placeholder="如 amount" @change="emit('change')" />
              </a-form-item>
              <a-form-item v-if="widget.type === 'bar' || widget.type === 'line'" label="系列字段（可空）">
                <a-select v-model:value="mapping.series" :options="columnOptions" allow-clear
                  placeholder="多系列时选择，如 channel" @change="emit('change')" />
              </a-form-item>
              <a-form-item v-if="widget.type === 'pie'" label="名称字段">
                <a-select v-model:value="mapping.name" :options="columnOptions" allow-clear
                  @change="emit('change')" />
              </a-form-item>
              <a-form-item v-if="widget.type === 'pie'" label="数值字段">
                <a-select v-model:value="mapping.value" :options="columnOptions" allow-clear
                  @change="emit('change')" />
              </a-form-item>
            </template>

            <template v-else>
              <a-form-item label="静态数据（JSON 数组）">
                <a-textarea v-model:value="staticText" :rows="8" placeholder='[{"name":"A","value":10}]'
                  @change="staticError = ''" />
              </a-form-item>
              <a-form-item>
                <a-space>
                  <a-button size="small" @click="applyStatic">
                    <template #icon>
                      <CheckOutlined />
                    </template>应用数据
                  </a-button>
                  <a-button size="small" type="link" @click="fillSample">填充示例</a-button>
                </a-space>
              </a-form-item>
              <a-alert v-if="staticError" type="error" show-icon :message="staticError" class="bs-alert" />
            </template>
          </a-form>
        </a-tab-pane>
      </a-tabs>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { message } from 'ant-design-vue';
import {
  CheckOutlined, DeleteOutlined, ThunderboltOutlined,
  VerticalAlignBottomOutlined, VerticalAlignTopOutlined,
} from '@ant-design/icons-vue';

import type { ScreenCanvas, ScreenDataset, ScreenWidget, WidgetMapping } from '../../visualization.types';

const props = withDefaults(defineProps<{
  widget?: ScreenWidget | null;
  canvas: ScreenCanvas;
  datasets?: ScreenDataset[];
  datasetCache?: Record<string, { rows: Record<string, unknown>[]; columns: string[] }>;
  loadingKey?: string;
}>(), {
  widget: null,
  datasets: () => [],
  datasetCache: () => ({}),
  loadingKey: '',
});

const emit = defineEmits<{
  (e: 'change'): void;
  (e: 'delete'): void;
  (e: 'to-front'): void;
  (e: 'to-back'): void;
  (e: 'load-data', datasetKey?: string): void;
  (e: 'manage-dataset'): void;
}>();

const activeTab = ref('layout');
const staticText = ref('[]');
const staticError = ref('');

const fontWeightOptions = [
  { label: '常规', value: 'normal' },
  { label: '加粗', value: 'bold' },
  { label: '特粗', value: '900' },
];
const fitOptions = [
  { label: '覆盖（裁剪）', value: 'cover' },
  { label: '完整显示', value: 'contain' },
  { label: '拉伸', value: 'fill' },
];

const presets = [
  { label: '1920×1080', width: 1920, height: 1080 },
  { label: '2560×1440', width: 2560, height: 1440 },
  { label: '3840×2160', width: 3840, height: 2160 },
  { label: '1080×1920（竖屏）', width: 1080, height: 1920 },
];

const applyPreset = (preset: { width: number; height: number }) => {
  props.canvas.width = preset.width;
  props.canvas.height = preset.height;
  emit('change');
};

const isChart = computed(() => ['bar', 'line', 'pie'].includes(props.widget?.type ?? ''));
const isDataWidget = computed(() => ['number', 'bar', 'line', 'pie', 'table'].includes(props.widget?.type ?? ''));

/**
 * 字段映射：模板里直接 v-model 到这个对象。
 * 历史配置可能没有 mapping 字段，这里按需补一个空对象，避免非空断言出现在模板表达式中。
 */
const mapping = computed<WidgetMapping>(() => {
  const widget = props.widget;
  if (!widget) return {};
  if (!widget.data.mapping) {
    widget.data.mapping = {};
  }
  return widget.data.mapping;
});

const datasetOptions = computed(() =>
  (props.datasets ?? []).map((item) => ({
    label: `${item.datasetName ?? ''}（${item.datasetKey ?? ''}）`,
    value: String(item.datasetKey ?? ''),
  })),
);

const currentColumns = computed(() => {
  const key = props.widget?.data?.datasetKey;
  if (!key) return [];
  return props.datasetCache[key]?.columns ?? [];
});

const columnOptions = computed(() => currentColumns.value.map((col) => ({ label: col, value: col })));

/** 切换组件/切换数据源时同步静态数据编辑框 */
watch(
  () => [props.widget?.id, props.widget?.data?.datasetKey, props.widget?.data?.mode],
  () => {
    staticText.value = JSON.stringify(props.widget?.data?.staticRows ?? [], null, 2);
    staticError.value = '';
    if (!props.widget) activeTab.value = 'layout';
  },
  { immediate: true },
);

const applyStatic = () => {
  try {
    const parsed = JSON.parse(staticText.value || '[]');
    if (!Array.isArray(parsed)) {
      staticError.value = '静态数据必须是 JSON 数组';
      return;
    }
    if (props.widget) {
      props.widget.data.staticRows = parsed as Record<string, unknown>[];
      staticError.value = '';
      emit('change');
      message.success('静态数据已应用');
    }
  } catch (e) {
    staticError.value = `JSON 解析失败：${(e as Error).message}`;
  }
};

const fillSample = () => {
  const sample = [
    { name: '一月', value: 120 },
    { name: '二月', value: 180 },
    { name: '三月', value: 150 },
    { name: '四月', value: 240 },
  ];
  staticText.value = JSON.stringify(sample, null, 2);
  if (props.widget) {
    props.widget.data.staticRows = sample;
    // 示例数据同时给出字段映射，图表可直接出图
    if (props.widget.data.mapping) {
      props.widget.data.mapping.x = 'name';
      props.widget.data.mapping.y = 'value';
      props.widget.data.mapping.name = 'name';
      props.widget.data.mapping.value = 'value';
    }
    emit('change');
  }
};
</script>

<style scoped>
.bs-property {
  width: 288px;
  flex-shrink: 0;
  height: 100%;
  overflow: auto;
  padding: 10px 12px;
  box-sizing: border-box;
  background: #0d1729;
  border-left: 1px solid rgba(64, 169, 255, 0.18);
}

.bs-panel-title {
  font-size: 13px;
  color: #cfe3ff;
  letter-spacing: 1px;
}

.bs-property-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 4px;
}

.bs-form {
  margin-top: 8px;
}

.bs-mul {
  color: #9fc4e8;
}

.bs-tag {
  cursor: pointer;
}

.bs-alert {
  margin-bottom: 10px;
}

.bs-empty-tip {
  margin-top: 12px;
  font-size: 12px;
  line-height: 1.7;
  color: rgba(159, 196, 232, 0.6);
}
</style>
