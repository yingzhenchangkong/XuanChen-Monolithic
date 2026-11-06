<template>
  <XCModal v-model:open="visible" :title="operationTitle" :width="560" :show-fullscreen-btn="false" @ok="handleOk">
    <a-form layout="vertical" :model="model" :rules="rules" ref="rulesRef" autoComplete="off">
      <a-row :gutter="12">
        <a-col :span="12">
          <a-form-item name="dsName" label="数据源名称" required>
            <a-input v-model:value="model.dsName" placeholder="如 财务系统达梦库" :maxlength="50" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item name="dsType" label="数据源类型" required>
            <a-select v-model:value="model.dsType" placeholder="请选择类型">
              <a-select-option value="mysql">MySQL</a-select-option>
              <a-select-option value="mariadb">MariaDB</a-select-option>
              <a-select-option value="postgresql">PostgreSQL</a-select-option>
              <a-select-option value="oracle">Oracle</a-select-option>
              <a-select-option value="sqlserver">SQL Server</a-select-option>
              <a-select-option value="dm">达梦 DM</a-select-option>
              <a-select-option value="kingbase">人大金仓 Kingbase</a-select-option>
              <a-select-option value="h2">H2</a-select-option>
            </a-select>
          </a-form-item>
        </a-col>
      </a-row>
      <a-form-item name="jdbcUrl" label="JDBC 连接串" required>
        <a-input v-model:value="model.jdbcUrl" placeholder="如 jdbc:mysql://192.168.1.10:3306/dbname?useSSL=false&serverTimezone=Asia/Shanghai" :maxlength="500" />
      </a-form-item>
      <a-row :gutter="12">
        <a-col :span="12">
          <a-form-item label="用户名">
            <a-input v-model:value="model.username" placeholder="数据库用户名" :maxlength="100" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item :label="model.id ? '密码（留空保持原密码）' : '密码'">
            <a-input-password v-model:value="model.password" placeholder="数据库密码" />
          </a-form-item>
        </a-col>
      </a-row>
      <a-form-item label="状态">
        <a-switch v-model:checked="model.status" :checked-value="1" :un-checked-value="0" checked-children="启用" un-checked-children="停用" />
      </a-form-item>
      <a-alert type="info" show-icon class="mb-2">
        <template #message>
          支持 jdbc:mysql/mariadb/postgresql/oracle/sqlserver/dm/kingbase/h2 协议，其他协议将被拒绝。
          保存后可在报表、接口管理、数据大屏、代码生成器中直接引用此数据源。
        </template>
      </a-alert>
    </a-form>
  </XCModal>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue';
import type { Rule } from 'ant-design-vue/es/form';
import { message } from 'ant-design-vue';
import XCModal from '@/components/xuanchen/XCModal.vue';
import { saveSysDatasourceApi, testSysDatasourceConnectionApi, type SysDatasourceModel } from '../datasource.api';

defineProps({
  operationTitle: { type: String, default: '编辑' },
});

const emit = defineEmits(['childOK']);
const visible = ref(false);
const rulesRef = ref();

// ant-design-vue 的 rules 必须按字段名索引，传数组会导致校验规则无法命中表单项
const rules: Record<string, Rule[]> = {
  dsName: [{ required: true, message: '数据源名称不能为空', trigger: 'blur' }],
  dsType: [{ required: true, message: '数据源类型不能为空', trigger: 'change' }],
  jdbcUrl: [{ required: true, message: 'JDBC 连接串不能为空', trigger: 'blur' }],
};

const defaultModel: SysDatasourceModel = {
  id: undefined,
  dsName: '',
  dsType: undefined,
  jdbcUrl: '',
  username: '',
  password: '',
  status: 1,
};

const model = reactive<SysDatasourceModel>({ ...defaultModel });

const show = () => {
  Object.assign(model, defaultModel);
  visible.value = true;
};

const add = () => {
  Object.assign(model, defaultModel);
  visible.value = true;
};

const edit = (record: SysDatasourceModel) => {
  Object.assign(model, record);
  model.password = ''; // 编辑时不回显密码
  visible.value = true;
};

const handleOk = async () => {
  try {
    await rulesRef.value.validate();
  } catch {
    return;
  }
  // 先测试连接，失败不阻塞保存但给出提醒
  if (model.username && model.password && model.jdbcUrl) {
    try {
      const testRes = await testSysDatasourceConnectionApi({ ...model });
      if (testRes.code !== 200) {
        message.warning(`连接测试未通过：${testRes.msg}，仍可保存`);
      }
    } catch { /* 测试失败不阻塞保存 */ }
  }
  const res = await saveSysDatasourceApi({ ...model });
  if (res.code === 200) {
    message.success(res.data ?? '保存成功');
    visible.value = false;
    emit('childOK');
  } else {
    message.error(res.msg ?? '保存失败');
  }
};

defineExpose({ show, add, edit });
</script>
