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
          <a-input v-if="ifCreateTable" v-model:value="model.tableName" placeholder="请输入表名" allowClear />
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
          <a-input v-model:value="model.outputDir" placeholder="请输入输出目录" allowClear />
        </a-form-item>
      </a-col>
      <a-col :span="5">
        <a-form-item name="packageName" label="包名" :labelCol="labelCol" :wrapperCol="wrapperCol">
          <a-input v-model:value="model.packageName" placeholder="请输入包名" allowClear />
        </a-form-item>
      </a-col>
      <a-col :span="6">
        <a-form-item name="moduleName" label="模块名" :labelCol="labelCol" :wrapperCol="wrapperCol">
          <a-input v-model:value="model.moduleName" placeholder="请输入模块名" allowClear />
        </a-form-item>
      </a-col>
    </a-row>
  </a-form>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue';
import type { Rule } from 'ant-design-vue/es/form';
import { message } from 'ant-design-vue';
import { getGenDatabaseSelect, getOneById } from '@/views/tool/generator/gendatabase/gendatabase.api';
import { saveOrUpdate, getTableList } from '../gentable.api';
import type { GenDatabase, GenTable } from '../gentable.types';

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

const rulesRef = ref();
const rules: Record<string, Rule[]> = {
  databaseId: [
    { required: true, message: '请输选择数据库', trigger: 'blur' },
  ],
  tableName: [
    { required: true, message: '请输入表名', trigger: 'blur' },
  ],
  tableComment: [
    { required: true, message: '请输入表注释', trigger: 'blur' },
  ],
  outputDir: [
    { required: true, message: '请输入输出目录', trigger: 'blur' },
  ],
  packageName: [
    { required: true, message: '请输入包名', trigger: 'blur' },
  ],
  moduleName: [
    { required: true, message: '请输入模块名', trigger: 'blur' },
  ],
}

const init = (data: any) => { };

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
const edit = (records: any) => {
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
  const res: any = await saveOrUpdate(model);
  message.success(res.msg);
  emit('childData', {});
}

const ifCreateTable = ref(false);

const optionsDatabase = ref([]);
const getDatabaseSelect = async () => {
  const res: any = await getGenDatabaseSelect();
  optionsDatabase.value = res.data;
  if (res.data && res.data.length === 1) {
    model.databaseId = res.data[0].id;
    getTable();
  }
}
getDatabaseSelect();

const modelDatabase = ref<GenDatabase>({
  id: '',
  connType: '',
  host: '',
  port: '',
  dbName: '',
  userName: '',
  password: '',
});

const handleChangeDatabase = async (id: string) => {
  const res: any = await getOneById(id);
  if (res.data) {
    model.databaseId = res.data.id;
    getTable();
  }
}

const optionsTable = ref<GenTable[]>([]);
const handleChangeIfCreateTable = () => {
  getTable();
}

const getTable = async () => {
  if (!model.databaseId) {
    message.error('请选择数据库');
    return;
  }
  try {
    const res: any = await getOneById(model.databaseId);
    modelDatabase.value.connType = res.data.connType;
    modelDatabase.value.host = res.data.host;
    modelDatabase.value.port = res.data.port;
    modelDatabase.value.dbName = res.data.dbName;
    modelDatabase.value.userName = res.data.userName;
    modelDatabase.value.password = res.data.password;
    const resTable: any = await getTableList(modelDatabase.value);
    if (resTable && resTable.length > 0) {
      optionsTable.value = resTable || [];
      if (resTable && resTable.length === 1 && !ifCreateTable.value) {
        model.tableName = resTable[0].tableName;
        model.tableComment = resTable[0].tableComment;
        emit('childData', { modelDatabase, tableName: resTable[0].tableName });
      }
    } else {
      ifCreateTable.value = true;
    }
  } catch (e) {
    message.error('获取表失败');
    optionsTable.value = [];
  }
}

const handleChangeTable = (tableName: string | undefined) => {
  if (tableName) {
    model.tableComment = optionsTable.value.find((item: GenTable) => item.tableName === tableName)?.tableComment || '';
  }
}

defineExpose({
  init,
});
</script>