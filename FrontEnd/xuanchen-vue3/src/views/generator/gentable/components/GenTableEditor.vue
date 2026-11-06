<template>
  <div class="gte-body">
    <!-- 表配置 -->
    <GenTableConfig @childData="recGenTableConfig" ref="refGenTableConfig" />

    <a-tabs v-model:activeKey="activeTab" type="editable-card" hide-add @edit="onTabEdit">
      <a-tab-pane key="main" tab="主表字段配置" :closable="false">
        <!-- 字段属性 + 页面配置合并为一张表：一行一个字段，不再分两个页签 -->
        <GenTableColumnConfig @childData="recMainColumns" ref="refMainColumnConfig" />
      </a-tab-pane>

      <!-- 子表页签：一张子表一个页签，可关闭删除，顺序即生成页面里的展示顺序 -->
      <a-tab-pane v-for="(sub, index) in subTables" :key="sub.uid" :tab="subTabTitle(sub, index)">
        <a-row :gutter="12">
          <a-col :span="8">
            <a-form-item label="子表名" :labelCol="{ span: 6 }" :wrapperCol="{ span: 18 }">
              <a-select v-model:value="sub.subTableName" :options="optionsSubTable" placeholder="请选择子表"
                :fieldNames="{ label: 'tableName', value: 'tableName' }" allowClear show-search
                @change="() => handleSubTableNameChange(sub)" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="外键列" :labelCol="{ span: 6 }" :wrapperCol="{ span: 18 }">
              <a-input v-model:value="sub.subFkName" placeholder="子表里指向主表的列，如 order_id" allowClear />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="子表标题" :labelCol="{ span: 6 }" :wrapperCol="{ span: 18 }">
              <a-input v-model:value="sub.subComment" placeholder="留空取表注释" allowClear />
            </a-form-item>
          </a-col>
        </a-row>
        <GenTableColumnConfig @childData="(data: any) => recSubColumns(sub, data)"
          :ref="(el: any) => setSubColumnRef(sub.uid, el)" />
      </a-tab-pane>
    </a-tabs>

    <a-button type="dashed" block style="margin-top: 12px;" @click="addSubTable">
      <template #icon>
        <PlusOutlined />
      </template>添加子表
    </a-button>
  </div>
</template>

<script setup lang="ts">
import { nextTick, reactive, ref } from 'vue';
import { message } from 'ant-design-vue';
import { PlusOutlined } from '@ant-design/icons-vue';

import GenTableConfig from './GenTableConfig.vue';
import GenTableColumnConfig from './GenTableColumnConfig.vue';

import {
  getListTableColumn, getListDBTable, getListDBTableColumn, getListTableColumnByTableId,
  getListSubTable, saveWithColumns, generator,
} from '../gentable.api';
import { tableColumn } from '../gentable.data';
import type { GenTable, GenTableColumn, DBTable } from '../gentable.types';

/**
 * 代码生成的配置主体（表配置 + 主表字段 + 子表页签）。
 * 两处复用：modal/Operation.vue（列表「编辑」弹窗）与 design/GenTableDesignPage.vue（「新增」全屏页）。
 * 组件只负责状态与校验保存，弹窗/页面的打开关闭由外层控制。
 */

/** GenTableConfig 通过 childData 回传的数据源 id 与表名（连接信息不再经过前端） */
interface GenTableConfigData {
  id: string;
  tableName?: string;
  /** true：目标库还没有该表，不能读库里的字段元数据，直接用手工配置/默认模板 */
  ifCreateTable?: boolean;
}

/** 子表编辑态：含本地唯一键（页签 key）与该子表的字段配置 */
interface SubTableForm {
  uid: string;
  id?: string;
  subTableName?: string;
  subFkName?: string;
  subComment?: string;
  columns: GenTableColumn[];
}

/** 保存结果：ok=false 时组件内部已提示过原因 */
export interface GenEditorSaveResult {
  ok: boolean;
}

const activeTab = ref('main');

/** 主表字段 */
const mainColumns = reactive<GenTableColumn[]>([]);
/** 子表列表（一个主表可挂多张子表） */
const subTables = reactive<SubTableForm[]>([]);
/** 子表字段表格实例：按 uid 存，增删页签不会串位 */
const subColumnRefs = new Map<string, any>();
/** 当前主表的数据源，子表名下拉与字段元数据都靠它 */
const currentDatabaseId = ref('');
const optionsSubTable = ref<DBTable[]>([]);

const refGenTableConfig = ref();
const refMainColumnConfig = ref();

const newUid = () => `sub_${Date.now()}_${Math.random().toString(36).slice(2, 7)}`;

/** 页签标题：优先子表标题，其次物理表名 */
const subTabTitle = (sub: SubTableForm, index: number) => sub.subComment || sub.subTableName || `子表 ${index + 1}`;

const setSubColumnRef = (uid: string, el: any) => {
  if (el) {
    subColumnRefs.set(uid, el);
  } else {
    subColumnRefs.delete(uid);
  }
};

/** 把某张子表的字段推给它的配置表格 */
const syncSub = (sub: SubTableForm) => {
  nextTick(() => {
    subColumnRefs.get(sub.uid)?.init(sub.columns);
  });
};

/** 重置（新增） */
const add = () => {
  activeTab.value = 'main';
  mainColumns.splice(0, mainColumns.length);
  subTables.splice(0, subTables.length);
  currentDatabaseId.value = '';
  optionsSubTable.value = [];
  // 必须重置表配置表单，否则新增会沿用上一条记录（含 id，保存会变成修改）
  nextTick(() => {
    refGenTableConfig.value?.add();
    // 字段清空后默认展示一行空数据（表格组件此时才随弹窗渲染完成，这里补一次 init）
    syncAll();
  });
};

/** 回显（编辑）：主表字段 + 各子表字段 */
const edit = async (record: GenTable) => {
  activeTab.value = 'main';
  currentDatabaseId.value = record.databaseId || '';
  refGenTableConfig.value?.edit(record);

  const saved = record.id ? await getListTableColumnByTableId(record.id) : [];
  const savedMain = saved.filter((item) => !item.subId && (item.columnScope || 'main') === 'main');
  if (savedMain.length) {
    mainColumns.splice(0, mainColumns.length, ...savedMain);
  } else if (record.ifCreateTable) {
    // 建表模式且还没保存过字段：给一份默认字段模板（snake_case，与真实列名一致）
    mainColumns.splice(0, mainColumns.length, ...cloneTemplate());
  } else {
    mainColumns.splice(0, mainColumns.length);
  }

  const subs = record.id ? await getListSubTable(record.id) : [];
  subTables.splice(0, subTables.length, ...subs.map((item) => ({
    uid: newUid(),
    id: item.id,
    subTableName: item.subTableName,
    subFkName: item.subFkName,
    subComment: item.subComment,
    columns: (item.columns || []).map((column) => ({ ...column })),
  })));
  loadSubTableOptions();
  syncAll();

  // 子表从未配置过字段：按物理表元数据起步，避免空表格让人以为没关联上
  for (const sub of subTables) {
    if (!sub.columns.length && sub.subTableName && currentDatabaseId.value) {
      await loadSubColumns(sub, sub.subTableName);
    }
  }
};

/** 整行都没填的空数据行（表格默认就带一行）：保存时直接忽略，不参与校验与提交 */
const isBlankColumn = (item: GenTableColumn) =>
  !String(item.columnName || '').trim()
  && !String(item.columnComment || '').trim()
  && !String(item.columnDefault || '').trim()
  && !item.columnIsPk;

/** 保存：表配置 + 主表字段 + 全部子表（含字段）一次性保存（后端同一事务） */
const save = async (): Promise<GenEditorSaveResult> => {
  // 0. 字段名不能为空（建表模式下会直接变成列名，空值只会在生成时报错）；整行空数据不算错
  const blankMain = mainColumns.findIndex(
    (item) => !isBlankColumn(item) && !String(item.columnName || '').trim(),
  );
  if (blankMain >= 0) {
    activeTab.value = 'main';
    message.warning(`主表字段第 ${blankMain + 1} 行的字段名不能为空`);
    return { ok: false };
  }
  const mainPayload = mainColumns.filter((item) => !isBlankColumn(item));
  // 1. 子表配置校验
  for (let index = 0; index < subTables.length; index++) {
    const sub = subTables[index];
    if (!sub.subTableName) {
      activeTab.value = sub.uid;
      message.warning(`第 ${index + 1} 张子表还没选择子表名`);
      return { ok: false };
    }
    if (!sub.subFkName) {
      activeTab.value = sub.uid;
      message.warning(`第 ${index + 1} 张子表还没填外键列`);
      return { ok: false };
    }
    const blankSub = sub.columns.findIndex(
      (item) => !isBlankColumn(item) && !String(item.columnName || '').trim(),
    );
    if (blankSub >= 0) {
      activeTab.value = sub.uid;
      message.warning(`子表「${sub.subTableName}」第 ${blankSub + 1} 行的字段名不能为空`);
      return { ok: false };
    }
  }
  // 2. 校验表配置（不保存）
  const table = await refGenTableConfig.value?.validate();
  if (!table) {
    return { ok: false }; // 校验未通过，保持当前界面
  }
  // 单表模板不生成子表：已配置子表时提前拦住，避免保存成功却生成不出子表
  if (table.template !== 'one2many' && subTables.length > 0) {
    activeTab.value = subTables[0].uid;
    message.warning('已配置子表，请将「生成模板」切换为「一对多」，否则子表不会生成');
    return { ok: false };
  }
  // 3. 建表模式下没有字段就无法建表，提前拦住
  if (table.ifCreateTable && mainPayload.length === 0) {
    activeTab.value = 'main';
    message.warning('已选择「是否创建表 = 是」，请先在「主表字段配置」添加字段（至少一个主键）');
    return { ok: false };
  }
  // 4. 一次请求完成「表配置 + 主表字段 + 子表」保存
  const isAdd = !table.id;
  const subPayload = subTables.map((sub) => ({
    subTableName: sub.subTableName,
    subFkName: sub.subFkName,
    subComment: sub.subComment,
    columns: sub.columns.filter((item) => !isBlankColumn(item)),
  }));
  const res = await saveWithColumns(table, mainPayload, subPayload);
  if (res.code !== 200) {
    message.error(res.msg || '保存失败');
    return { ok: false };
  }
  if (isAdd && typeof res.data === 'string' && res.data) {
    // 新增后直接生成：产物写入「输出目录」，不用再回列表点一次「生成」
    await autoGenerate(res.data, table.outputDir);
    return { ok: true };
  }
  message.success(res.msg || '修改成功');
  return { ok: true };
};

/**
 * 新增保存后自动生成代码。
 * 生成失败不影响已保存的配置，只给出提示让用户手动重试。
 */
const autoGenerate = async (id: string, outputDir?: string) => {
  const hide = message.loading('表配置已保存，正在生成代码…', 0);
  try {
    const gen = await generator(id);
    hide();
    if (gen.code !== 200) {
      message.warning(`已保存，但代码生成失败：${gen.msg || '未知错误'}，请稍后点「生成」重试`, 6);
      return;
    }
    const count = Array.isArray(gen.data) ? gen.data.length : 0;
    message.success(`添加成功，已生成 ${count} 个文件${outputDir ? ` 到 ${outputDir}` : ''}`, 6);
  } catch {
    hide();
    message.warning('表配置已保存，但自动生成失败，请手动点击「生成」重试', 6);
  }
};

// ---------------------------------------------------------------- 子表

/** 新增一张子表并切到它的页签 */
const addSubTable = () => {
  const sub: SubTableForm = { uid: newUid(), subComment: '', subFkName: '', columns: [] };
  subTables.push(sub);
  loadSubTableOptions();
  activeTab.value = sub.uid;
  if (!optionsSubTable.value.length) {
    message.info('请先在上方选好「数据库」，子表名下拉才有候选');
  }
};

/** 关闭页签即删除该子表 */
const onTabEdit = (targetKey: any, action: 'add' | 'remove') => {
  if (action !== 'remove') {
    return;
  }
  const index = subTables.findIndex((item) => item.uid === targetKey);
  if (index < 0) {
    return;
  }
  subTables.splice(index, 1);
  if (activeTab.value === targetKey) {
    activeTab.value = 'main';
  }
};

/** 子表名候选：当前数据源里的全部物理表 */
const loadSubTableOptions = async () => {
  if (!currentDatabaseId.value) {
    return;
  }
  try {
    optionsSubTable.value = await getListDBTable(currentDatabaseId.value) || [];
  } catch {
    optionsSubTable.value = [];
  }
};

/**
 * 子表名变更：按新表重新读字段元数据。
 * 已有字段配置会被覆盖，所以只在用户主动改表名时触发。
 */
const handleSubTableNameChange = async (sub: SubTableForm) => {
  sub.columns = [];
  if (sub.subTableName) {
    await loadSubColumns(sub, sub.subTableName);
  }
  syncSub(sub);
};

/** 按物理表元数据填充某张子表的字段 */
const loadSubColumns = async (sub: SubTableForm, tableName: string) => {
  if (!currentDatabaseId.value || !tableName) {
    return;
  }
  try {
    const meta = await getListDBTableColumn(currentDatabaseId.value, tableName);
    sub.columns = meta.map((item) => ({ ...item, columnScope: 'sub' }));
  } catch {
    sub.columns = [];
  }
  syncSub(sub);
};

/** 子表字段表格编辑 → 回写该子表的字段配置 */
const recSubColumns = (sub: SubTableForm, data: GenTableColumn[]) => {
  sub.columns = [...data];
};

// ---------------------------------------------------------------- 主表字段加载

/** 表配置变更（数据源 / 表名 / 建表模式）后重新加载主表字段；子表有自己的页签，不受影响 */
const recGenTableConfig = async (data: GenTableConfigData) => {
  currentDatabaseId.value = data.id || '';
  loadSubTableOptions();
  await loadMainColumns(data);
  syncAll();
};

/** 主表字段加载：已保存配置 → 物理表元数据 → 默认模板 */
const loadMainColumns = async (data: GenTableConfigData) => {
  if (!data.id) {
    return;
  }
  if (data.ifCreateTable) {
    // 建表模式：目标库还没有这张表，读元数据必然为空，直接用已保存配置或默认模板起步
    if (!data.tableName) {
      mainColumns.splice(0, mainColumns.length, ...cloneTemplate());
      return;
    }
    const saved = (await getListTableColumn(data.id, data.tableName))
      .filter((item) => !item.subId && (item.columnScope || 'main') === 'main');
    mainColumns.splice(0, mainColumns.length, ...(saved.length ? saved : cloneTemplate()));
    return;
  }
  if (!data.tableName) {
    return;
  }
  const saved = (await getListTableColumn(data.id, data.tableName))
    .filter((item) => !item.subId && (item.columnScope || 'main') === 'main');
  if (saved.length) {
    mainColumns.splice(0, mainColumns.length, ...saved);
    return;
  }
  const meta = await getListDBTableColumn(data.id, data.tableName);
  mainColumns.splice(0, mainColumns.length, ...(meta.length ? meta : cloneTemplate()));
};

/** 主表字段表格编辑 → 回写主表字段（一行同时含库表属性与页面配置） */
const recMainColumns = (data: GenTableColumn[]) => {
  mainColumns.splice(0, mainColumns.length, ...data);
};

/** 把当前主表字段推给两个表格 */
const syncAll = () => {
  nextTick(() => {
    refMainColumnConfig.value?.init(mainColumns);
    subTables.forEach((sub) => syncSub(sub));
  });
};

/** 默认字段模板要深拷贝：直接塞原数组会让多行共用同一对象 */
const cloneTemplate = (): GenTableColumn[] => tableColumn.map((item) => ({ ...item, columnScope: 'main' }));

defineExpose({
  add,
  edit,
  save,
});
</script>

<style scoped>
.gte-body {
  display: flex;
  flex-direction: column;
}
</style>
