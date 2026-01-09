<template>
  <a-row :gutter="20">
    <a-col :span="6">
      <a-card title="数据库配置">
        <a-form layout="inline" :model="formDataDB" :rules="formRulesDB" ref="formRefDB" autocomplete="off"
          class="modal-form-style">
          <a-form-item name="url" label="地址" :labelCol="labelCol" :wrapperCol="wrapperCol">
            <a-input placeholder="请输入数据库连接地址" v-model:value="formDataDB.url"></a-input>
          </a-form-item>
          <a-form-item name="databaseName" label="数据库名称" :labelCol="labelCol" :wrapperCol="wrapperCol">
            <a-input placeholder="请输入数据库名称" v-model:value="formDataDB.databaseName"></a-input>
          </a-form-item>
          <a-form-item name="username" label="用户名" :labelCol="labelCol" :wrapperCol="wrapperCol">
            <a-input placeholder="请输入数据库用户名" v-model:value="formDataDB.username"></a-input>
          </a-form-item>
          <a-form-item name="password" label="密码" :labelCol="labelCol" :wrapperCol="wrapperCol">
            <a-input placeholder="请输入数据库密码" v-model:value="formDataDB.password"></a-input>
          </a-form-item>
        </a-form>
        <a-form-item style="width: 100px;float: right;">
          <a-button type="primary" style="width: 100%;" @click="formSubmitDB">确认</a-button>
        </a-form-item>
      </a-card>
      <a-card title="生成代码配置" style="margin-top: 10px;">
        <a-form layout="inline" :model="formData" :rules="formRules" ref="formRef" autocomplete="off"
          class="modal-form-style">
          <a-form-item name="tableName" label="表名" :labelCol="labelCol" :wrapperCol="wrapperCol">
            <a-select v-model:value="formData.tableName" :options="optionsTable" show-search
              :filter-option="filterOptionTable" :fieldNames="{ label: 'tableComment', value: 'tableName' }"
              placeholder="请选择表名" allowClear @change="handleChangeTable"></a-select>
          </a-form-item>
          <a-form-item name="outputDir" label="输出目录" :labelCol="labelCol" :wrapperCol="wrapperCol">
            <a-input placeholder="请输入输出目录" v-model:value="formData.outputDir"></a-input>
          </a-form-item>
          <a-form-item name="moduleName" label="模块名" :labelCol="labelCol" :wrapperCol="wrapperCol">
            <a-input placeholder="请输入模块名" v-model:value="formData.moduleName"></a-input>
          </a-form-item>
          <a-form-item name="packageName" label="包名" :labelCol="labelCol" :wrapperCol="wrapperCol">
            <a-input placeholder="请输入包名" v-model:value="formData.packageName"></a-input>
          </a-form-item>
          <a-form-item style="width: 100px;float: right;">
            <a-button type="primary" style="width: 100%;" @click="formSubmit">生成</a-button>
          </a-form-item>
        </a-form>
      </a-card>
    </a-col>
    <a-col :span="18">
      <a-card title="表结构">
        <a-button type="primary" style="width: 100px;margin-bottom: 20px;float:right;"
          @click="tableSubmit">同步表结构</a-button>
        <a-table :dataSource="dataSource" :columns="columns" :pagination="false" bordered rowKey="id"
          size="small"></a-table>
      </a-card>
    </a-col>
  </a-row>
  <a-card>


  </a-card>
</template>

<script setup lang="ts">
import { getAction } from '@/utils/httpAction';
import { reactive, ref } from 'vue';
import { message } from 'ant-design-vue';

const labelCol = { span: 6 };
const wrapperCol = { span: 18 };

const url = {
  generator: '/codeGenerator/generator',
  getTableList: '/codeGenerator/getTableList',
  getTableInfo: '/codeGenerator/getTableInfo',
}

const columns = [
  { title: '字段名', dataIndex: 'columnName' },
  { title: '字段注释', dataIndex: 'columnComment' },
  { title: '字段类型', dataIndex: 'dataType' },
  { title: '字段最大长度', dataIndex: 'characterMaximumLength' },
  { title: '是否允许为NULL', dataIndex: 'isNullable' },
  { title: '默认值', dataIndex: 'columnDefault' },
  { title: '键类型', dataIndex: 'columnKey' },
  { title: '扩展信息', dataIndex: 'extra' },
]

const formRefDB = ref();
const formRef = ref();

const optionsTable = ref([]);
const dataSource = ref();
const filterOptionTable = (input: string, option: any) => {
  return option.tableComment.includes(input);
};

const formDataDB = reactive({
  url: 'localhost:3306',
  databaseName: 'xuanchen-dormitory',
  username: 'root',
  password: 'Admin123',
});
const formData = reactive({
  outputDir: 'D:/codeGenerator',
  moduleName: '',
  packageName: '',
  tableName: '',
});

const formRulesDB = reactive({
  url: [
    { required: true, message: '请输入数据库连接地址', trigger: 'blur' },
  ],
  databaseName: [
    { required: true, message: '请输入数据库名称', trigger: 'blur' },
  ],
  username: [
    { required: true, message: '请输入数据库用户名', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入数据库密码', trigger: 'blur' },
  ],
})

const formRules = reactive({
  outputDir: [
    { required: true, message: '请输入输出目录', trigger: 'blur' },
  ],
  moduleName: [
    { required: true, message: '请输入模块名', trigger: 'blur' },
  ],
  packageName: [
    { required: true, message: '请输入包名', trigger: 'blur' },
  ],
  tableName: [
    { required: true, message: '请选择表名', trigger: 'blur' },
  ],
})

const formSubmitDB = () => {
  formRefDB.value.validate().then(() => {
    getTableList();
  })
}

const formSubmit = () => {
  formRef.value.validate().then(() => {
    const mergedData = { ...formData, ...formDataDB };
    getAction(url.generator, mergedData).then((res: any) => {
      if (res && res.code === 200) {
        message.success(res.msg);
      } else {
        message.error(res.msg);
      }
    })
  })
}

const getTableList = () => {
  getAction(url.getTableList, formDataDB).then((res: any) => {
    if (res && res.code === 200) {
      optionsTable.value = res.data;
    } else {
      message.error(res.msg);
    }
  })
}
const handleChangeTable = () => {
  getTableInfo();
}
const getTableInfo = () => {
  const mergedData = { ...formData, ...formDataDB };
  getAction(url.getTableInfo, mergedData).then((res: any) => {
    if (res && res.code === 200) {
      dataSource.value = res.data;
    } else {
      message.error(res.msg);
    }
  })
}

const tableSubmit = () => {
  console.log(dataSource.value, '11111111111111');

}
</script>