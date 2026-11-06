<template>
  <a-card>
    <a-tabs defaultActiveKey="1">
      <a-tab-pane tab="基本信息" key="1">
        <div class="btn-style">
          <a-form layout="inline" :model="model" :rules="rules" ref="rulesRef" autoComplete="off"
            class="modal-form-style">
            <a-form-item name="parentDeptCode" label="上级部门名称" :labelCol="labelCol" :wrapperCol="wrapperCol">
              <a-tree-select v-model:value="model.parentDeptCode" show-search style="width: 100%"
                :dropdown-style="{ maxHeight: '400px', overflow: 'auto' }" placeholder="请选择上级部门" allow-clear
                :tree-data="treeData" tree-node-filter-prop="label" @change="createDeptCode"
                :field-names="{ children: 'children', label: 'title', value: 'key', }">
              </a-tree-select>
            </a-form-item>
            <a-form-item name="deptCode" label="部门编码" :labelCol="labelCol" :wrapperCol="wrapperCol">
              <a-input v-model:value="model.deptCode" placeholder="请输入部门编码" allowClear :disabled="true" />
            </a-form-item>
            <a-form-item name="deptName" label="部门名称" :labelCol="labelCol" :wrapperCol="wrapperCol">
              <a-input v-model:value="model.deptName" placeholder="请输入部门名称" allowClear />
            </a-form-item>
            <a-form-item name="leaderUser" label="部门负责人" :labelCol="labelCol" :wrapperCol="wrapperCol">
              <a-select v-model:value="model.leaderUser" placeholder="请选择负责人（可空）" allowClear show-search
                option-filter-prop="label" :options="userOptions" />
            </a-form-item>
            <a-form-item label="状态" :labelCol="labelCol" :wrapperCol="wrapperCol">
              <a-switch v-model:checked="model.status" :checked-value="1" :un-checked-value="0" checked-children="启用" un-checked-children="停用" />
            </a-form-item>
            <a-form-item label="排序码" :labelCol="labelCol" :wrapperCol="wrapperCol">
              <a-input-number v-model:value="model.orderNo" placeholder="请输入排序码" allowClear style="width: 100%" />
            </a-form-item>
            <div style="width: 100%;">
              <a-button type="primary" @click="handleSave" style="float: right;">
                <template #icon>
                  <SaveOutlined />
                </template>保存
              </a-button>
              <a-button @click="handleReset" style="float: right;">
                <template #icon>
                  <ReloadOutlined />
                </template>重置
              </a-button>
            </div>
          </a-form>
        </div>
      </a-tab-pane>
      <a-tab-pane tab="部门人员" key="2">
        <DeptUser :deptCode="model.deptCode" />
      </a-tab-pane>
    </a-tabs>
  </a-card>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue';
import type { Rule } from 'ant-design-vue/es/form';
import { message } from 'ant-design-vue';
import DeptUser from './DeptUser.vue';

import { getDeptTreeApi, getSelectedDeptApi, createDeptCodeApi, saveOrUpdate } from '../dept.api';
import type { DeptTranData, DeptModel, DeptRecord } from '../dept.types';
import { getUserOptionsApi } from '@/views/businessprocess/businessprocess.api';
import type { BpmUserOption } from '@/views/businessprocess/businessprocess.types';

const model = reactive<DeptModel>({
  id: '',
  deptCode: '',
  parentDeptCode: undefined as string | undefined,
  deptName: '',
  leaderUser: undefined as string | undefined,
  status: 1,
  orderNo: undefined,
})

/** 负责人下拉（启用账号；负责人供审批流"部门负责人/直属上级"规则使用） */
const userOptions = ref<Array<{ label: string; value: string }>>([]);
const loadUserOptions = async () => {
  try {
    const list: BpmUserOption[] = await getUserOptionsApi();
    userOptions.value = (list ?? []).map((u) => ({ label: `${u.nickName}（${u.userName}）`, value: u.userName }));
  } catch {
    // 拦截器已统一提示
  }
};
loadUserOptions();

const labelCol = { span: 4 };
const wrapperCol = { span: 18 };
const treeData = ref<DeptRecord[]>([]);
const emit = defineEmits(['childData']);

const rulesRef = ref()
const rules: Record<string, Rule[]> = {
  deptName: [
    { required: true, message: '请输入部门名称', trigger: 'blur' },
  ],
}

const handleSave = async () => {
  await rulesRef.value.validate();
  try {
    const res = await saveOrUpdate(model);
    if (res.code !== 200) {
      message.error(res.msg);
      return;
    }
    message.success(res.msg);
    emit('childData', { selectedKey: model.deptCode, ifAddChild: false });
  } catch {
    // 网络/HTTP 错误已由响应拦截器统一提示
  }
};

const handleReset = () => {
  model.id = '';
  model.deptCode = '';
  model.parentDeptCode = undefined as string | undefined;
  model.deptName = '';
  model.leaderUser = undefined as string | undefined;
  model.status = 1;
  model.orderNo = undefined;
  ;
}

const getDeptTree = async () => {
  treeData.value = await getDeptTreeApi();
}
getDeptTree();

const getSelectedDept = async (deptCode: string) => {
  const res = await getSelectedDeptApi(deptCode);
  model.id = res.id;
  model.deptCode = res.deptCode;
  model.deptName = res.deptName;
  model.leaderUser = res.leaderUser || undefined;
  model.status = res.status;
  model.orderNo = res.orderNo;
}

const createDeptCode = async (value: string) => {
  model.deptCode = await createDeptCodeApi(value);
}

const getParentCode = (tree: DeptRecord[], targetCode: string, parentCode: string = ''): string => {
  for (const node of tree) {
    if (node.key === targetCode) {
      return parentCode;
    }
    if (node.children && node.children.length > 0) {
      const result = getParentCode(node.children, targetCode, node.key);
      if (result !== '') {
        return result;
      }
    }
  }
  return '';
};

const recParentData = async (data: DeptTranData) => {
  if (!data.selectedKey) {
    model.id = '';
    model.parentDeptCode = undefined;
    model.deptName = '';
    model.leaderUser = undefined as string | undefined;
    model.status = 1;
    model.orderNo = undefined;
    await createDeptCode('');
    await getDeptTree();
  } else {
    if (data.ifAddChild) {
      await createDeptCode(data.selectedKey);
      model.id = '';
      model.parentDeptCode = data.selectedKey;
      model.deptName = '';
      model.leaderUser = undefined as string | undefined;
      model.status = 1;
      model.orderNo = undefined;
    } else {
      await getDeptTree();
      model.parentDeptCode = getParentCode(treeData.value, data.selectedKey);
      await getSelectedDept(data.selectedKey);
    }
  }
}

defineExpose({
  recParentData
})
</script>