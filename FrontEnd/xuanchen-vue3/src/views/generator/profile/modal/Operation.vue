<template>
  <XCModal v-model:open="visible" :title="operationTitle" :width="720" :show-fullscreen-btn="false" @ok="handleOk">
    <a-form :model="model" :rules="rules" ref="rulesRef" autoComplete="off" class="modal-form-style">
      <a-row :gutter="12">
        <a-col :span="12">
          <a-form-item name="profileName" label="预设名称" :labelCol="labelCol" :wrapperCol="wrapperCol">
            <a-input v-model:value="model.profileName" placeholder="如：萱晨后台 / 客户A项目" allowClear />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item name="databaseId" label="数据源" :labelCol="labelCol" :wrapperCol="wrapperCol">
            <a-select v-model:value="model.databaseId" :options="databaseOptions" allowClear
              :fieldNames="{ label: 'connName', value: 'id' }" placeholder="请选择数据源" />
          </a-form-item>
        </a-col>

        <a-col :span="12">
          <a-form-item name="packageName" label="包名" :labelCol="labelCol" :wrapperCol="wrapperCol">
            <a-input v-model:value="model.packageName" placeholder="如 user，可多段 a.b" allowClear />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item name="moduleName" label="模块名" :labelCol="labelCol" :wrapperCol="wrapperCol">
            <a-input v-model:value="model.moduleName" placeholder="如 system，仅字母数字下划线" allowClear />
          </a-form-item>
        </a-col>

        <a-col :span="24">
          <a-form-item name="outputDir" label="输出目录" :labelCol="labelCol" :wrapperCol="wrapperCol">
            <a-input-group compact class="output-dir-group">
              <a-input v-model:value="model.outputDir" readonly placeholder="点击右侧「选择目录」"
                class="output-dir-input" />
              <a-button type="primary" @click="dirPickerOpen = true">
                <template #icon>
                  <FolderOpenOutlined />
                </template>选择目录
              </a-button>
            </a-input-group>
          </a-form-item>
        </a-col>

        <a-col :span="12">
          <a-form-item name="backendRelPath" label="后端相对路径" :labelCol="labelCol" :wrapperCol="wrapperCol">
            <div class="path-group">
              <a-input v-model:value="model.backendRelPath" placeholder="如 BackEnd/xxx/src/main/java" allowClear />
              <a-tooltip :title="model.outputDir ? '浏览输出目录下的目录，自动换算相对路径' : '请先选择输出目录'">
                <a-button type="text" :disabled="!model.outputDir" @click="openRelPicker('backend')">
                  <template #icon>
                    <FolderOpenOutlined />
                  </template>
                </a-button>
              </a-tooltip>
            </div>
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item name="frontendRelPath" label="前端相对路径" :labelCol="labelCol" :wrapperCol="wrapperCol">
            <div class="path-group">
              <a-input v-model:value="model.frontendRelPath" placeholder="如 FrontEnd/xxx/src/views" allowClear />
              <a-tooltip :title="model.outputDir ? '浏览输出目录下的目录，自动换算相对路径' : '请先选择输出目录'">
                <a-button type="text" :disabled="!model.outputDir" @click="openRelPicker('frontend')">
                  <template #icon>
                    <FolderOpenOutlined />
                  </template>
                </a-button>
              </a-tooltip>
            </div>
          </a-form-item>
        </a-col>

        <a-col :span="8">
          <a-form-item name="parentMenuId" label="上级菜单" :labelCol="labelCol" :wrapperCol="wrapperCol">
            <a-tree-select v-model:value="model.parentMenuId" :tree-data="menuTreeOptions" show-search allowClear
              :field-names="{ children: 'children', label: 'title', value: 'value' }"
              :dropdown-style="{ maxHeight: '400px', overflow: 'auto' }" tree-node-filter-prop="title"
              placeholder="请选择上级菜单（根菜单=顶级）" />
          </a-form-item>
        </a-col>
        <a-col :span="8">
          <a-form-item name="template" label="生成模板" :labelCol="labelCol" :wrapperCol="wrapperCol">
            <a-select v-model:value="model.template" :options="templateOptions" />
          </a-form-item>
        </a-col>
        <a-col :span="8">
          <a-form-item name="orderNo" label="排序码" :labelCol="labelCol" :wrapperCol="wrapperCol">
            <a-input-number v-model:value="model.orderNo" placeholder="越小越前" style="width: 100%" />
          </a-form-item>
        </a-col>
      </a-row>
    </a-form>

    <DirPicker v-model:open="dirPickerOpen" :value="model.outputDir || ''" @select="handleSelectDir" />
    <!-- 相对路径同样走目录浏览：选完自动换算成「相对输出目录」的路径 -->
    <DirPicker v-model:open="relPickerOpen" :value="relPickerValue" :title="relPickerTitle"
      @select="handleRelDirSelect" />
  </XCModal>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue';
import type { Rule } from 'ant-design-vue/es/form';
import { message } from 'ant-design-vue';
import XCModal from '@/components/xuanchen/XCModal.vue';
import DirPicker from '../../gentable/components/DirPicker.vue';
import { getGenDatabaseSelect } from '../../gendatabase/gendatabase.api';
import { getMenuTreeOptions } from '@/utils/menuTree';
import type { MenuTreeOption } from '@/utils/menuTree';
import { saveOrUpdate } from '../profile.api';
import type { GenProjectProfile } from '../../gentable/gentable.types';
import type { SelectOption } from '@/types/api';

defineProps({
  operationTitle: {
    type: String,
    default: '编辑',
  },
});

const emit = defineEmits(['childOK']);
const labelCol = { span: 6 };
const wrapperCol = { span: 18 };

const visible = ref(false);
const rulesRef = ref();

const templateOptions = [
  { label: '单表', value: 'single' },
  { label: '一对多', value: 'one2many' },
];

/** 历史模板值归一化：crud / backend / one2one 等旧值统一按一对多处理（与后端 GenTemplate.of 口径一致） */
const normalizeTemplate = (value?: string) => (value === 'single' || value === 'one2many' ? value : 'one2many');

const model = reactive<GenProjectProfile>({
  id: '',
  profileName: '',
  databaseId: undefined,
  outputDir: '',
  backendRelPath: '',
  frontendRelPath: '',
  packageName: '',
  moduleName: '',
  parentMenuId: '0',
  template: 'single',
  orderNo: 0,
});

const IDENTIFIER_PATTERN = /^[A-Za-z_][A-Za-z0-9_]*(\.[A-Za-z_][A-Za-z0-9_]*)*$/;

const rules: Record<string, Rule[]> = {
  profileName: [
    { required: true, message: '请输入预设名称', trigger: 'blur' },
    { max: 64, message: '预设名称不能超过 64 个字符', trigger: 'blur' },
  ],
  packageName: [
    { pattern: IDENTIFIER_PATTERN, message: '包名仅允许字母、数字、下划线，多段以英文句点分隔', trigger: 'blur' },
  ],
  moduleName: [
    { pattern: IDENTIFIER_PATTERN, message: '模块名仅允许字母、数字、下划线，多段以英文句点分隔', trigger: 'blur' },
  ],
};

const databaseOptions = ref<SelectOption[]>([]);
const loadDatabases = async () => {
  const res = await getGenDatabaseSelect();
  databaseOptions.value = res.data ?? [];
};

/**
 * 「上级菜单」树形下拉。
 * 组件挂载即拉一次，不依赖 add()/edit() 的调用时机；之后每次打开弹窗再刷新，
 * 保证刚维护过的菜单能立刻出现在下拉里。
 */
const menuTreeOptions = ref<MenuTreeOption[]>([]);
const loadMenuTree = async () => {
  try {
    menuTreeOptions.value = await getMenuTreeOptions();
  } catch (error) {
    console.warn('【generator】加载上级菜单失败:', error);
  }
};
loadMenuTree();

const dirPickerOpen = ref(false);
const handleSelectDir = (path: string) => {
  model.outputDir = path;
};

// ---------------------------------------------------------------- 相对路径目录选择

/** 当前正在选择的是后端还是前端相对路径 */
const relPickTarget = ref<'backend' | 'frontend'>('backend');
const relPickerOpen = ref(false);

/** 相对路径拼成绝对路径，交给目录选择器定位 */
const joinPath = (base?: string, rel?: string) => {
  const b = (base || '').replace(/\\/g, '/').replace(/\/+$/, '');
  const r = (rel || '').replace(/\\/g, '/').replace(/^\/+/, '');
  if (!b) {
    return '';
  }
  return r ? `${b}/${r}` : b;
};

const relPickerValue = computed(() => joinPath(
  model.outputDir,
  relPickTarget.value === 'backend' ? model.backendRelPath : model.frontendRelPath,
));

const relPickerTitle = computed(() => (
  relPickTarget.value === 'backend' ? '选择后端相对路径目录' : '选择前端相对路径目录'
));

const openRelPicker = (target: 'backend' | 'frontend') => {
  if (!model.outputDir) {
    message.warning('请先选择输出目录');
    return;
  }
  relPickTarget.value = target;
  relPickerOpen.value = true;
};

/**
 * 绝对路径换算成相对输出目录的路径。
 * 返回 undefined 表示不在输出目录之下（不允许），空串表示就是输出目录本身。
 */
const toRelativePath = (abs: string, base: string): string | undefined => {
  const a = abs.replace(/\\/g, '/').replace(/\/+$/, '');
  const b = base.replace(/\\/g, '/').replace(/\/+$/, '');
  if (!b) {
    return undefined;
  }
  const lowerA = a.toLowerCase();
  const lowerB = b.toLowerCase();
  if (lowerA === lowerB) {
    return '';
  }
  if (!lowerA.startsWith(`${lowerB}/`)) {
    return undefined;
  }
  return a.slice(b.length + 1);
};

const handleRelDirSelect = (absPath: string) => {
  const relative = toRelativePath(absPath, model.outputDir || '');
  if (relative === undefined) {
    message.warning('只能选择「输出目录」之内的目录；需要写到别处请先调整输出目录');
    return;
  }
  if (relPickTarget.value === 'backend') {
    model.backendRelPath = relative;
  } else {
    model.frontendRelPath = relative;
  }
  message.success(relative ? `已设置相对路径：${relative}` : '已指向输出目录根');
};

//打开弹窗（新增）
const add = () => {
  visible.value = true;
  if (rulesRef.value) {
    rulesRef.value.resetFields();
  }
  model.id = '';
  model.profileName = '';
  model.databaseId = undefined;
  model.outputDir = '';
  model.backendRelPath = '';
  model.frontendRelPath = '';
  model.packageName = '';
  model.moduleName = '';
  model.parentMenuId = '0';
  model.template = 'single';
  model.orderNo = 0;
  loadDatabases();
  loadMenuTree();
};

//打开弹窗（编辑）
const edit = (record: GenProjectProfile) => {
  visible.value = true;
  if (rulesRef.value) {
    rulesRef.value.resetFields();
  }
  model.id = record.id;
  model.profileName = record.profileName;
  model.databaseId = record.databaseId;
  model.outputDir = record.outputDir;
  model.backendRelPath = record.backendRelPath || '';
  model.frontendRelPath = record.frontendRelPath || '';
  model.packageName = record.packageName || '';
  model.moduleName = record.moduleName || '';
  model.parentMenuId = record.parentMenuId || '0';
  model.template = normalizeTemplate(record.template);
  model.orderNo = record.orderNo ?? 0;
  loadDatabases();
  loadMenuTree();
};

const handleOk = async () => {
  await rulesRef.value.validate();
  try {
    const res = await saveOrUpdate(model);
    if (res.code !== 200) {
      message.error(res.msg || '保存失败');
      return;
    }
    message.success(res.msg || '保存成功');
    emit('childOK');
    visible.value = false;
  } catch {
    // 网络/HTTP 错误已由响应拦截器统一提示，这里保持弹窗打开
  }
};

defineExpose({
  add,
  edit,
});
</script>

<style lang="scss" scoped>
.modal-form-style :deep(.ant-form-item) {
  margin-left: 0;
  margin-right: 0;
}

.output-dir-group {
  display: flex;
  width: 100%;
}

/* 输入框 + 目录浏览按钮并排，输入框吃掉剩余宽度 */
.path-group {
  display: flex;
  align-items: center;
  gap: 4px;
  width: 100%;
}

.path-group :deep(.ant-input-affix-wrapper),
.path-group :deep(.ant-input) {
  flex: 1;
  min-width: 0;
}

.output-dir-input {
  flex: 1;
  min-width: 0;
  font-family: Consolas, Monaco, monospace;
}
</style>
