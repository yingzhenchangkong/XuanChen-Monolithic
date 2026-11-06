<template>
  <a-form :model="model" :rules="rules" ref="rulesRef" autoComplete="off" class="modal-form-style">
    <a-row :gutter="12">
      <a-col :span="6">
        <a-form-item label="项目预设" :labelCol="labelCol" :wrapperCol="wrapperCol">
          <!-- 选中即带出「数据源 + 输出目录 + 相对路径 + 包名/模块名 + 模板」 -->
          <div class="profile-group">
            <a-select v-model:value="selectedProfileId" :options="profiles" allowClear class="profile-select"
              :fieldNames="{ label: 'profileName', value: 'id' }" placeholder="选择预设，一键带出项目配置"
              @change="applyProfile"></a-select>
            <a-tooltip :title="selectedProfileId ? '用当前表单覆盖所选预设' : '把当前表单保存为预设'">
              <a-button type="text" @click="openProfileModal">
                <template #icon>
                  <SaveOutlined />
                </template>
              </a-button>
            </a-tooltip>
            <a-popconfirm v-if="selectedProfileId" title="确定删除该预设吗？" placement="left" @confirm="removeProfile">
              <a-button type="text" danger>
                <template #icon>
                  <DeleteOutlined />
                </template>
              </a-button>
            </a-popconfirm>
          </div>
        </a-form-item>
      </a-col>
      <a-col :span="6">
        <a-form-item name="databaseId" label="统一数据源" :labelCol="labelCol" :wrapperCol="wrapperCol">
          <a-select v-model:value="model.databaseId" :options="optionsDatabase" placeholder="请选择数据源"
            :fieldNames="{ label: 'connName', value: 'id' }" @change="handleChangeDatabase" allowClear
            class="db-select"></a-select>
        </a-form-item>
      </a-col>
      <a-col :span="6">
        <a-form-item name="ifCreateTable" label="是否创建表" :labelCol="labelCol" :wrapperCol="wrapperCol">
          <div class="ctrl-with-tip">
            <a-switch v-model:checked="model.ifCreateTable" checked-children="是" un-checked-children="否"
              @change="handleChangeIfCreateTable" />
            <XCTipIcon title="是：目标库还没有这张表，生成时按「数据库表配置」里的字段自动建表" />
          </div>
        </a-form-item>
      </a-col>
      <a-col :span="6">
        <a-form-item name="tableName" label="表名" :labelCol="labelCol" :wrapperCol="wrapperCol">
          <a-input v-if="model.ifCreateTable" v-model:value="model.tableName" placeholder="字母/下划线开头" allowClear />
          <a-select v-else v-model:value="model.tableName" :options="optionsTable" placeholder="请选择表名"
            :fieldNames="{ label: 'tableName', value: 'tableName' }" @change="handleChangeTable" allowClear></a-select>
        </a-form-item>
      </a-col>
      <a-col :span="6">
        <a-form-item name="tableComment" label="表注释" :labelCol="labelCol" :wrapperCol="wrapperCol">
          <a-input v-model:value="model.tableComment" placeholder="请输入表注释" allowClear />
        </a-form-item>
      </a-col>

      <a-col :span="6">
        <a-form-item name="template" label="生成模板" :labelCol="labelCol" :wrapperCol="wrapperCol">
          <div class="ctrl-with-tip">
            <a-select v-model:value="model.template" :options="templateOptions" placeholder="请选择模板" />
            <XCTipIcon title="单表=标准 CRUD 页面；一对多=字典管理式，主表列表页下方维护子表明细（需先在下方添加子表）" />
          </div>
        </a-form-item>
      </a-col>
      <a-col :span="6">
        <a-form-item name="parentMenuId" label="上级菜单" :labelCol="labelCol" :wrapperCol="wrapperCol">
          <div class="ctrl-with-tip">
            <a-tree-select v-model:value="model.parentMenuId" :tree-data="menuTreeOptions" show-search allowClear
              :field-names="{ children: 'children', label: 'title', value: 'value' }"
              :dropdown-style="{ maxHeight: '400px', overflow: 'auto' }" tree-node-filter-prop="title"
              placeholder="请选择上级菜单（根菜单=顶级）" @change="emitTableChange" />
            <XCTipIcon title="menu.sql 中菜单挂到哪个菜单下面，根菜单表示顶级菜单" />
          </div>
        </a-form-item>
      </a-col>

      <a-col :span="6">
        <a-form-item name="outputDir" label="输出目录" :labelCol="labelCol" :wrapperCol="wrapperCol">
          <a-input-group compact class="output-dir-group">
            <a-input v-model:value="model.outputDir" readonly placeholder="点击右侧「选择目录」" class="output-dir-input" />
            <a-button type="primary" @click="dirPickerOpen = true">
              <template #icon>
                <FolderOpenOutlined />
              </template>选择目录
            </a-button>
          </a-input-group>
        </a-form-item>
      </a-col>
      <a-col :span="6">
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
            <XCTipIcon title="留空=输出目录根；可指向模块的 src/main/java" />
          </div>
        </a-form-item>
      </a-col>
      <a-col :span="6">
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
            <XCTipIcon title="留空=输出目录/vue；可指向 src/views" />
          </div>
        </a-form-item>
      </a-col>
      <a-col :span="6">
        <a-form-item name="moduleName" label="模块名" :labelCol="labelCol" :wrapperCol="wrapperCol">
          <a-input v-model:value="model.moduleName" placeholder="如 system，仅字母数字下划线" allowClear />
        </a-form-item>
      </a-col>
      <a-col :span="6">
        <a-form-item name="packageName" label="包名" :labelCol="labelCol" :wrapperCol="wrapperCol">
          <a-input v-model:value="model.packageName" placeholder="如 user，可多段 a.b" allowClear />
        </a-form-item>
      </a-col>
    </a-row>

    <!-- 输出目录在服务端浏览：选完即写入该目录，无需手工输入路径 -->
    <DirPicker v-model:open="dirPickerOpen" :value="model.outputDir || ''" @select="handleSelectDir" />
    <!-- 相对路径也用目录浏览：选完自动换算成「相对输出目录」的路径 -->
    <DirPicker v-model:open="relPickerOpen" :value="relPickerValue" :title="relPickerTitle"
      @select="handleRelDirSelect" />

    <!-- 保存为项目预设：只问名称，其余取当前表单值 -->
    <XCModal v-model:open="profileModalOpen" :title="selectedProfileId ? '覆盖项目预设' : '保存为项目预设'" :width="440"
      :show-fullscreen-btn="false" @ok="submitProfile">
      <a-form layout="vertical">
        <a-form-item label="预设名称" required>
          <a-input v-model:value="profileName" placeholder="如：萱晨后台 / 客户A项目" allowClear @pressEnter="submitProfile" />
        </a-form-item>
        <div class="profile-tip">
          将保存当前表单的：数据源、输出目录、后端/前端相对路径、包名、模块名、上级菜单 与生成模板。
        </div>
      </a-form>
    </XCModal>
  </a-form>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue';
import type { Rule } from 'ant-design-vue/es/form';
import { message } from 'ant-design-vue';
import XCModal from '@/components/xuanchen/XCModal.vue';
import { getGenDatabaseSelect } from '@/views/generator/gendatabase/gendatabase.api';
import { getListDBTable, getDirListing, getProjectProfiles, saveProjectProfile, deleteProjectProfile } from '../gentable.api';
import type { GenTable, GenTableColumn, DBTable, GenProjectProfile } from '../gentable.types';
import type { SelectOption } from '@/types/api';
import DirPicker from './DirPicker.vue';
import XCTipIcon from '@/components/xuanchen/XCTipIcon.vue';
import { getMenuTreeOptions } from '@/utils/menuTree';
import type { MenuTreeOption } from '@/utils/menuTree';

defineProps({
  operationTitle: {
    type: String,
    default: '编辑'
  }
});
const emit = defineEmits(['childData']);

const labelCol = { span: 8 };
const wrapperCol = { span: 16 };

const model = reactive<GenTable>({
  id: '',
  databaseId: undefined,
  tableName: undefined,
  ifCreateTable: false,
  tableComment: '',
  parentMenuId: '0',
  template: 'single',
  outputDir: '',
  backendRelPath: '',
  frontendRelPath: '',
  packageName: '',
  moduleName: '',
});

/** 模板选项：单表=标准 CRUD；一对多=字典管理式主子页面（需配置子表） */
const templateOptions = [
  { label: '单表', value: 'single' },
  { label: '一对多', value: 'one2many' },
];

/** 历史模板值归一化：crud / backend / one2one 等旧值统一按一对多处理（与后端 GenTemplate.of 口径一致） */
const normalizeTemplate = (value?: string) => (value === 'single' || value === 'one2many' ? value : 'one2many');

// 与后端 GeneratorSafetyValidator 一致的标识符白名单（最终以后端校验为准）
const IDENTIFIER_PATTERN = /^[A-Za-z_][A-Za-z0-9_]*(\.[A-Za-z_][A-Za-z0-9_]*)*$/;
const TABLE_NAME_PATTERN = /^[A-Za-z_][A-Za-z0-9_]{0,63}$/;

/** 相对路径：禁止绝对路径与 ".." 段 */
const validateRelativePath = (_rule: unknown, value: string) => {
  if (!value) {
    return Promise.resolve();
  }
  const v = value.trim().replace(/\\/g, '/');
  if (v.startsWith('/') || /^[A-Za-z]:/.test(v)) {
    return Promise.reject(new Error('请填写相对输出目录的路径，不要用绝对路径或盘符'));
  }
  if (v.split('/').some((seg) => seg === '..' || seg === '.' || seg === '')) {
    return Promise.reject(new Error('相对路径不能包含 "."、".." 或空段'));
  }
  return Promise.resolve();
};

const rulesRef = ref();
const rules: Record<string, Rule[]> = {
  databaseId: [
    { required: true, message: '请输选择数据库', trigger: 'blur' },
  ],
  tableName: [
    { required: true, message: '请输入表名', trigger: 'blur' },
    { pattern: TABLE_NAME_PATTERN, message: '表名仅允许字母、数字、下划线，须以字母或下划线开头', trigger: 'blur' },
  ],
  tableComment: [
    { required: true, message: '请输入表注释', trigger: 'blur' },
  ],
  outputDir: [
    { required: true, message: '请输入输出目录', trigger: 'blur' },
    {
      validator: (_rule: unknown, value: string) => {
        if (!value) {
          return Promise.resolve();
        }
        const v = value.trim();
        // 控制字符 + Windows 文件名非法字符；冒号单独处理（盘符 D:/ 是合法的）
        if (/[\x00-\x1f\x7f<>"|?*]/.test(v)) {
          return Promise.reject(new Error('输出目录包含非法字符（控制字符或 < > " | ? *）'));
        }
        if (v.replace(/^[A-Za-z]:/, '').includes(':')) {
          return Promise.reject(new Error('输出目录路径格式不合法：冒号只允许出现在盘符后（如 D:/code/gen）'));
        }
        // 只拒绝作为路径段的 ".."（文件名里的 a..b 不该被误伤）
        if (v.split(/[\\/]+/).some((seg) => seg === '..')) {
          return Promise.reject(new Error('输出目录禁止包含 ../ 上一级跳转，请直接填写白名单根目录下的路径'));
        }
        return Promise.resolve();
      },
      trigger: 'blur',
    },
  ],
  backendRelPath: [{ validator: validateRelativePath, trigger: 'blur' }],
  frontendRelPath: [{ validator: validateRelativePath, trigger: 'blur' }],
  packageName: [
    { required: true, message: '请输入包名', trigger: 'blur' },
    { pattern: IDENTIFIER_PATTERN, message: '包名仅允许字母、数字、下划线，多段以英文句点分隔', trigger: 'blur' },
  ],
  moduleName: [
    { required: true, message: '请输入模块名', trigger: 'blur' },
    { pattern: IDENTIFIER_PATTERN, message: '模块名仅允许字母、数字、下划线，多段以英文句点分隔', trigger: 'blur' },
  ],
}

/**
 * 「上级菜单」树形下拉。
 * 组件挂载（弹窗首次打开）就先拉一次：新增流程由父组件直接渲染本组件，
 * 并不会调用下面的 add()，只靠 add()/edit() 会漏掉「新增」这条路径。
 * 之后每次打开弹窗再刷新一次，保证刚维护过的菜单能立刻出现在下拉里。
 */
const menuTreeOptions = ref<MenuTreeOption[]>([]);
const loadMenuTree = async () => {
  try {
    menuTreeOptions.value = await getMenuTreeOptions();
  } catch (error) {
    // 兜底：菜单树拿不到不该影响整张表单，保留上一次的选项即可
    console.warn('【generator】加载上级菜单失败:', error);
  }
};
loadMenuTree();

const init = (data: GenTableColumn[]) => { };

//打开弹窗
const add = async () => {
  if (rulesRef.value) {
    rulesRef.value.resetFields();
  }
  // 新建时不沿用上一次选中的预设，避免用户以为已经套用了
  selectedProfileId.value = undefined;
  model.id = '';
  model.tableName = undefined;
  model.ifCreateTable = false;
  model.tableComment = '';
  model.parentMenuId = '0';
  model.template = 'single';
  model.outputDir = '';
  model.backendRelPath = '';
  model.frontendRelPath = '';
  model.packageName = '';
  model.moduleName = '';
  optionsTable.value = [];
  loadMenuTree();
  // 默认给一个可用目录（服务器根目录/第一个盘符），省得用户每次都从头浏览
  await fillDefaultOutputDir();
}
const edit = (records: GenTable) => {
  if (rulesRef.value) {
    rulesRef.value.resetFields();
  }
  selectedProfileId.value = undefined;
  model.id = records.id;
  model.databaseId = records.databaseId;
  model.tableName = records.tableName;
  model.ifCreateTable = !!records.ifCreateTable;
  model.tableComment = records.tableComment;
  model.parentMenuId = records.parentMenuId || '0';
  model.template = normalizeTemplate(records.template);
  model.outputDir = records.outputDir;
  model.backendRelPath = records.backendRelPath;
  model.frontendRelPath = records.frontendRelPath;
  model.packageName = records.packageName;
  model.moduleName = records.moduleName;
  // 只回填下拉候选，不自动改已保存的表名/注释
  loadTableOptions();
  loadMenuTree();
}

/**
 * 校验并返回表配置（不保存）。
 * 统一由 Operation 调用「保存表配置 + 主/子表字段」的事务接口，避免两次请求产生半成品。
 */
const validate = async (): Promise<GenTable | undefined> => {
  try {
    await rulesRef.value.validate();
    // 路径首尾空格在服务器上会被当成目录名的一部分，入库前统一去掉
    if (model.outputDir) {
      model.outputDir = model.outputDir.trim();
    }
    if (model.backendRelPath) {
      model.backendRelPath = model.backendRelPath.trim().replace(/\\/g, '/');
    }
    if (model.frontendRelPath) {
      model.frontendRelPath = model.frontendRelPath.trim().replace(/\\/g, '/');
    }
    return { ...model };
  } catch {
    return undefined;
  }
};

/** 兼容旧调用：校验后立即保存表配置 */
const submit = async (): Promise<{ id: string; ifCreateTable: boolean } | undefined> => {
  const data = await validate();
  if (!data) {
    return undefined;
  }
  const { saveOrUpdate } = await import('../gentable.api');
  const res = await saveOrUpdate(data);
  if (res.code !== 200) {
    message.error(res.msg);
    return undefined;
  }
  message.success(res.msg);
  if (!model.id && typeof res.data === 'string' && res.data) {
    model.id = res.data;
  }
  return { id: model.id || '', ifCreateTable: !!model.ifCreateTable };
};

/** 编辑回显时填充表名下拉候选（不改变已保存的值） */
const loadTableOptions = async () => {
  if (model.ifCreateTable || !model.databaseId) {
    return;
  }
  const res = await getListDBTable(model.databaseId);
  optionsTable.value = res || [];
  if (!optionsTable.value.some((item) => item.tableName === model.tableName)) {
    optionsTable.value = [
      { tableName: model.tableName, tableComment: model.tableComment } as DBTable,
      ...optionsTable.value,
    ];
  }
};

const optionsDatabase = ref<SelectOption[]>([]);

/** 只刷新数据源下拉候选，不做自动选中 */
const loadDatabaseOptions = async () => {
  const res = await getGenDatabaseSelect();
  optionsDatabase.value = res.data ?? [];
};

const getDatabaseSelect = async () => {
  await loadDatabaseOptions();
  if (optionsDatabase.value.length === 1) {
    const dbId = optionsDatabase.value[0].id;
    if (typeof dbId === 'string') {
      model.databaseId = dbId;
    }
    getTable();
  }
}
getDatabaseSelect();

// ---------------------------------------------------------------- 项目预设

/** 预设列表：选中即带出数据源/输出目录/相对路径/包名/模块名/模板 */
const profiles = ref<GenProjectProfile[]>([]);
const selectedProfileId = ref<string>();
const profileModalOpen = ref(false);
const profileName = ref('');

const loadProfiles = async () => {
  profiles.value = await getProjectProfiles();
};
loadProfiles();

const applyProfile = (id?: string) => {
  if (!id) {
    return;
  }
  const profile = profiles.value.find((item) => item.id === id);
  if (!profile) {
    return;
  }
  if (profile.databaseId) {
    model.databaseId = profile.databaseId;
    handleChangeDatabase(profile.databaseId);
  }
  if (profile.outputDir) {
    model.outputDir = profile.outputDir;
  }
  model.backendRelPath = profile.backendRelPath || '';
  model.frontendRelPath = profile.frontendRelPath || '';
  model.packageName = profile.packageName || '';
  model.moduleName = profile.moduleName || '';
  model.parentMenuId = profile.parentMenuId || '0';
  model.template = normalizeTemplate(profile.template);
  emitTableChange();
  message.success(`已应用预设「${profile.profileName}」`);
};

/** 打开命名弹窗：已选中预设时默认带出名称，保存即覆盖 */
const openProfileModal = () => {
  const current = profiles.value.find((item) => item.id === selectedProfileId.value);
  profileName.value = current?.profileName || '';
  profileModalOpen.value = true;
};

const submitProfile = async () => {
  const name = profileName.value.trim();
  if (!name) {
    message.warning('请输入预设名称');
    return;
  }
  const res = await saveProjectProfile({
    id: selectedProfileId.value,
    profileName: name,
    databaseId: model.databaseId,
    outputDir: model.outputDir,
    backendRelPath: model.backendRelPath,
    frontendRelPath: model.frontendRelPath,
    packageName: model.packageName,
    moduleName: model.moduleName,
    parentMenuId: model.parentMenuId,
    template: model.template,
  });
  if (res.code !== 200) {
    message.error(res.msg || '保存失败');
    return;
  }
  message.success(res.msg || '保存成功');
  profileModalOpen.value = false;
  await loadProfiles();
  if (typeof res.data === 'string') {
    selectedProfileId.value = res.data;
  }
};

const removeProfile = async () => {
  if (!selectedProfileId.value) {
    return;
  }
  const res = await deleteProjectProfile(selectedProfileId.value);
  if (res.code !== 200) {
    message.error(res.msg || '删除失败');
    return;
  }
  message.success(res.msg || '删除成功');
  selectedProfileId.value = undefined;
  await loadProfiles();
};

const handleChangeDatabase = (id: string) => {
  // 只需把已保存数据源 id 传给后端，连接信息一律不再经过前端
  model.databaseId = id;
  if (!model.ifCreateTable) {
    getTable();
  }
}

const optionsTable = ref<DBTable[]>([]);
const handleChangeIfCreateTable = () => {
  // 「创建表」= 目标库还没有这张表：表名手工填，字段全部在「数据库表配置」页签里加
  if (model.ifCreateTable) {
    optionsTable.value = [];
    model.tableComment = model.tableComment || '';
  } else {
    getTable();
  }
  emitTableChange();
}

const getTable = async () => {
  if (model.ifCreateTable) {
    return;
  }
  const databaseId = model.databaseId;
  if (!databaseId) {
    message.error('请选择数据库');
    return;
  }
  try {
    const resTable = await getListDBTable(databaseId);
    if (resTable && resTable.length > 0) {
      optionsTable.value = resTable || [];
      if (resTable && resTable.length === 1 && !model.ifCreateTable) {
        model.tableName = resTable[0].tableName;
        model.tableComment = resTable[0].tableComment;
        emitTableChange();
      }
    } else {
      // 库里没有任何表：自动切到"创建表"模式，字段由用户配置
      model.ifCreateTable = true;
      optionsTable.value = [];
    }
  } catch {
    message.error('获取表失败');
    optionsTable.value = [];
  }
}

const handleChangeTable = (tableName: string | undefined) => {
  if (tableName) {
    model.tableComment = optionsTable.value.find((item) => item.tableName === tableName)?.tableComment || '';
  }
  emitTableChange();
}

/** 通知外层按新的数据源/表名/建表模式重新加载主表字段配置 */
const emitTableChange = () => {
  if (!model.databaseId) {
    return;
  }
  emit('childData', {
    id: model.databaseId,
    tableName: model.tableName,
    ifCreateTable: !!model.ifCreateTable,
  });
}

/** 新建时给输出目录一个默认值：服务端建议目录（未限制时为当前用户主目录） */
const fillDefaultOutputDir = async () => {
  if (model.outputDir) {
    return;
  }
  const res = await getDirListing();
  const defaultPath = res?.defaultPath || res?.roots?.[0]?.path;
  if (defaultPath) {
    model.outputDir = defaultPath;
  }
}

const dirPickerOpen = ref(false);
const handleSelectDir = (path: string) => {
  model.outputDir = path;
}

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

/** 复用同一个目录选择器，标题按后端/前端区分 */
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
 * 返回 undefined 表示不在输出目录之下（不允许写入），空串表示就是输出目录本身。
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

defineExpose({
  init,
  // 父组件在「新增」时 nextTick 里调用 add()，之前漏暴露导致新增不重置表单
  // （会沿用上一条记录的 id，保存时变成修改），这里补齐
  add,
  submit,
  validate,
  edit,
});
</script>

<style lang="scss" scoped>
/* 全局 .modal-form-style 给表单项加了 10px 横向外边距，叠加上 width:100% 后
   最右侧一列的表单项会超出弹窗约 10px，把 body 撑出横向滚动条；这里收回横向边距 */
.modal-form-style :deep(.ant-form-item) {
  margin-left: 0;
  margin-right: 0;
}

/* 控件 + 悬停提示图标：说明文字默认隐藏，鼠标移到问号上才展开 */
.ctrl-with-tip {
  display: flex;
  align-items: center;
  width: 100%;

  /* 只拉伸输入类控件，开关保持自身尺寸 */
  >.ant-select,
  >.ant-input,
  >.ant-input-affix-wrapper {
    flex: 1;
    min-width: 0;
  }
}

.profile-group {
  display: flex;
  align-items: center;
  width: 100%;
}

/* 相对路径：输入框 + 目录选择按钮 + 提示图标 */
.path-group {
  display: flex;
  align-items: center;
  width: 100%;

  >.ant-input,
  >.ant-input-affix-wrapper {
    flex: 1;
    min-width: 0;
  }
}

.profile-select {
  flex: 1;
  min-width: 0;
}

.profile-tip {
  color: rgba(0, 0, 0, 0.45);
  font-size: 12px;
  line-height: 20px;
}

.db-select-group {
  display: flex;
  align-items: center;
  width: 100%;
}

.db-select {
  flex: 1;
  min-width: 0;
}

.output-dir-group {
  display: flex;
  width: 100%;
}

.output-dir-input {
  flex: 1;
  min-width: 0;
  font-family: Consolas, Monaco, monospace;
}
</style>
