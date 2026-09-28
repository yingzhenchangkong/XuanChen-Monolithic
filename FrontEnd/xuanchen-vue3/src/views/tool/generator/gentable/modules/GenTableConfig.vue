<template>
  <a-form :model="model" :rules="rules" ref="rulesRef" autoComplete="off" class="modal-form-style">
    <a-row>
      <a-col :span="6">
        <a-form-item name="databaseId" label="数据库" :labelCol="labelCol" :wrapperCol="wrapperCol">
          <a-select v-model:value="model.databaseId" :options="optionsDatabase" placeholder="请选择数据库"
            :fieldNames="{ label: 'connName', value: 'id' }" @change="handleChangeDatabase" allowClear></a-select>
        </a-form-item>
      </a-col>
      <a-col :span="6">
        <a-form-item name="ifCreateTable" label="是否创建表" :labelCol="labelCol" :wrapperCol="wrapperCol">
          <a-switch v-model:checked="ifCreateTable" checked-children="是" un-checked-children="否"
            @change="handleChangeIfCreateTable" />
        </a-form-item>
      </a-col>
      <a-col :span="6">
        <a-form-item name="tableName" label="表名" :labelCol="labelCol" :wrapperCol="wrapperCol">
          <a-input v-if="ifCreateTable" v-model:value="model.tableName" placeholder="字母/下划线开头" allowClear />
          <a-select v-else v-model:value="model.tableName" :options="optionsTable" placeholder="请选择表名"
            :fieldNames="{ label: 'tableName', value: 'tableName' }" @change="handleChangeTable" allowClear></a-select>
        </a-form-item>
      </a-col>
      <a-col :span="5">
        <a-form-item name="tableComment" label="表注释" :labelCol="labelCol" :wrapperCol="wrapperCol">
          <a-input v-model:value="model.tableComment" placeholder="请输入表注释" allowClear />
        </a-form-item>
      </a-col>
      <a-col :span="6">
        <a-form-item name="subTableName" label="子表名" :labelCol="labelCol" :wrapperCol="wrapperCol">
          <a-input v-model:value="model.subTableName" placeholder="请输入子表名" allowClear />
        </a-form-item>
      </a-col>
      <a-col :span="6">
        <a-form-item name="subTableFkName" label="子表外键名" :labelCol="labelCol" :wrapperCol="wrapperCol">
          <a-input v-model:value="model.subTableFkName" placeholder="请输入子表外键名" allowClear />
        </a-form-item>
      </a-col>
      <a-col :span="6">
        <a-form-item name="outputDir" label="输出目录" :labelCol="labelCol" :wrapperCol="wrapperCol">
          <a-input v-model:value="model.outputDir" placeholder="须位于服务器白名单根目录内，禁止 ../" allowClear />
        </a-form-item>
      </a-col>
      <a-col :span="5">
        <a-form-item name="packageName" label="包名" :labelCol="labelCol" :wrapperCol="wrapperCol">
          <a-input v-model:value="model.packageName" placeholder="如 user，可多段 a.b" allowClear />
        </a-form-item>
      </a-col>
      <a-col :span="6">
        <a-form-item name="moduleName" label="模块名" :labelCol="labelCol" :wrapperCol="wrapperCol">
          <a-input v-model:value="model.moduleName" placeholder="如 system，仅字母数字下划线" allowClear />
        </a-form-item>
      </a-col>
    </a-row>
  </a-form>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue';
import type { Rule } from 'ant-design-vue/es/form';
import { message } from 'ant-design-vue';
import { getGenDatabaseSelect } from '@/views/tool/generator/gendatabase/gendatabase.api';
import { saveOrUpdate, getListDBTable } from '../gentable.api';
import type { GenTable, GenTableColumn, DBTable } from '../gentable.types';
import type { SelectOption } from '@/types/api';

defineProps({
  operationTitle: {
    type: String,
    default: '编辑'
  }
});
const emit = defineEmits(['childData']);

const labelCol = { span: 6 };
const wrapperCol = { span: 18 };

const model = reactive<GenTable>({
  id: '',
  databaseId: undefined,
  tableName: undefined,
  tableComment: '',
  subTableName: '',
  subTableFkName: '',
  template: '',
  outputDir: '',
  packageName: '',
  moduleName: '',
});

// 与后端 GeneratorSafetyValidator 一致的标识符白名单（最终以后端校验为准）
const IDENTIFIER_PATTERN = /^[A-Za-z_][A-Za-z0-9_]*(\.[A-Za-z_][A-Za-z0-9_]*)*$/;
const TABLE_NAME_PATTERN = /^[A-Za-z_][A-Za-z0-9_]{0,63}$/;

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
        if (value && (value.includes('..') || /[\x00-\x1f<>:"|?*]/.test(value))) {
          return Promise.reject(new Error('输出目录禁止包含 ../ 或控制字符，且必须位于服务器白名单根目录内'));
        }
        return Promise.resolve();
      },
      trigger: 'blur',
    },
  ],
  packageName: [
    { required: true, message: '请输入包名', trigger: 'blur' },
    { pattern: IDENTIFIER_PATTERN, message: '包名仅允许字母、数字、下划线，多段以英文句点分隔', trigger: 'blur' },
  ],
  moduleName: [
    { required: true, message: '请输入模块名', trigger: 'blur' },
    { pattern: IDENTIFIER_PATTERN, message: '模块名仅允许字母、数字、下划线，多段以英文句点分隔', trigger: 'blur' },
  ],
}

const init = (data: GenTableColumn[]) => { };

//打开弹窗
const add = () => {
  if (rulesRef.value) {
    rulesRef.value.resetFields();
  }
  model.id = '';
  model.tableName = undefined;
  model.tableComment = '';
  model.subTableName = '';
  model.subTableFkName = '';
  model.template = '';
  model.outputDir = '';
  model.packageName = '';
  model.moduleName = '';
}
const edit = (records: GenTable) => {
  if (rulesRef.value) {
    rulesRef.value.resetFields();
  }
  model.id = records.id;
  model.tableName = records.tableName;
  model.tableComment = records.tableComment;
  model.subTableName = records.subTableName;
  model.subTableFkName = records.subTableFkName;
  model.template = records.template;
  model.outputDir = records.outputDir;
  model.packageName = records.packageName;
  model.moduleName = records.moduleName;
}

const submit = async () => {
  await rulesRef.value.validate();
  try {
    const res = await saveOrUpdate(model);
    if (res.code !== 200) {
      message.error(res.msg);
      return;
    }
    message.success(res.msg);
    emit('childData', {});
  } catch {
    // 网络/HTTP 错误已由响应拦截器统一提示
  }
}

const ifCreateTable = ref(false);

const optionsDatabase = ref<SelectOption[]>([]);
const getDatabaseSelect = async () => {
  const res = await getGenDatabaseSelect();
  optionsDatabase.value = res.data ?? [];
  if (res.data && res.data.length === 1) {
    const dbId = res.data[0].id;
    if (typeof dbId === 'string') {
      model.databaseId = dbId;
    }
    getTable();
  }
}
getDatabaseSelect();

const handleChangeDatabase = (id: string) => {
  // 只需把已保存数据源 id 传给后端，连接信息一律不再经过前端
  model.databaseId = id;
  getTable();
}

const optionsTable = ref<DBTable[]>([]);
const handleChangeIfCreateTable = () => {
  getTable();
}

const getTable = async () => {
  const databaseId = model.databaseId;
  if (!databaseId) {
    message.error('请选择数据库');
    return;
  }
  try {
    const resTable = await getListDBTable(databaseId);
    if (resTable && resTable.length > 0) {
      optionsTable.value = resTable || [];
      if (resTable && resTable.length === 1 && !ifCreateTable.value) {
        model.tableName = resTable[0].tableName;
        model.tableComment = resTable[0].tableComment;
        emit('childData', { id: databaseId, tableName: resTable[0].tableName });
      }
    } else {
      ifCreateTable.value = true;
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
}

defineExpose({
  init,
});
</script>