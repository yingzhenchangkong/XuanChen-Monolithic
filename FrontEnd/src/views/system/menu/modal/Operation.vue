<template>
  <a-modal v-model:open="visible" :title="modalTitle" :width="500" @ok="handleOk" ok-text="确认" cancel-text="取消">
    <a-form layout="inline" :model="model" :rules="rules" ref="rulesRef" autoComplete="off" class="modal-form-style">
      <a-form-item label="上级菜单" :labelCol="labelCol" :wrapperCol="wrapperCol">
        <a-tree-select v-model:value="model.parentId" show-search style="width: 100%"
          :dropdown-style="{ maxHeight: '400px', overflow: 'auto' }" placeholder="请选择上级菜单" allow-clear
          :tree-data="treeData" :field-names="{ children: 'children', label: 'title', value: 'id' }"
          tree-node-filter-prop="title">
        </a-tree-select>
      </a-form-item>
      <a-form-item label="路由标题" :labelCol="labelCol" :wrapperCol="wrapperCol">
        <a-input v-model:value="model.title" placeholder="请输入路由标题" allowClear />
      </a-form-item>
      <a-form-item name="name" label="路由名称" :labelCol="labelCol" :wrapperCol="wrapperCol">
        <a-input v-model:value="model.name" placeholder="请输入路由名称" allowClear />
      </a-form-item>
      <a-form-item label="菜单图标" :labelCol="labelCol" :wrapperCol="wrapperCol">
        <icon-picker v-model:icon="model.icon">
          <template #iconSelect>
            <a-input v-model:value="model.icon" placeholder="请选择菜单图标" />
          </template>
        </icon-picker>
      </a-form-item>
      <a-form-item name="path" label="路由地址" :labelCol="labelCol" :wrapperCol="wrapperCol">
        <a-input v-model:value="model.path" placeholder="请输入路由地址" allowClear />
      </a-form-item>
      <a-form-item label="组件路径" :labelCol="labelCol" :wrapperCol="wrapperCol">
        <a-input v-model:value="model.component" placeholder="请输入组件路径" allowClear />
      </a-form-item>
      <a-form-item label="状态" :labelCol="labelCol" :wrapperCol="wrapperCol">
        <a-switch v-model:checked="model.status" checked-children="启用" un-checked-children="停用" />
      </a-form-item>
      <a-form-item label="排序码" :labelCol="labelCol" :wrapperCol="wrapperCol">
        <a-input-number v-model:value="model.orderNo" placeholder="请输入排序码" allowClear style="width: 100%" />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue';
import { getAction, httpAction } from '@/utils/httpAction';
import { message } from 'ant-design-vue';
import type { TreeSelectProps } from 'ant-design-vue';
import iconPicker from '@/components/iconpicker/IconPicker.vue';
import type { Rule } from 'ant-design-vue/es/form';

defineProps({
  modalTitle: {
    type: String,
    default: '编辑'
  }
})

const labelCol = { span: 5 };
const wrapperCol = { span: 17 };

const emit = defineEmits(['childOK']);
const url = {
  add: '/system/menu/add',
  edit: '/system/menu/edit',
  selectMenu: '/system/menu/list',
  validateName: '/system/menu/validateName',
  validatePath: '/system/menu/validatePath',
}
const visible = ref(false);

const model = reactive({
  id: '',
  parentId: undefined,
  name: '',
  title: '',
  path: '',
  component: '',
  perms: '',
  icon: '',
  menuType: undefined,
  status: true,
  orderNo: ''
})

const validateName = async (rule: Rule, value: string) => {
  const res: any = await getAction(url.validateName, { id: model.id, name: value });
  if (res.code === 500) {
    return Promise.reject(res.msg);
  } else {
    return Promise.resolve();
  }
}
const validatePath = async (rule: Rule, value: string) => {
  const res: any = await getAction(url.validatePath, { id: model.id, path: value });
  if (res.code === 500) {
    return Promise.reject(res.msg);
  } else {
    return Promise.resolve();
  }
}

const rulesRef = ref()
const rules: Record<string, Rule[]> = {
  name: [
    { required: true, message: '请输入路由名称', trigger: 'blur' },
    { required: true, validator: validateName, trigger: 'blur' }
  ],
  path: [
    { required: true, message: '请输入路由地址', trigger: 'blur' },
    { required: true, validator: validatePath, trigger: 'blur' }
  ],
}

//打开弹窗
const add = () => {
  model.id = '';
  model.parentId = undefined;
  model.name = '';
  model.title = '';
  model.path = '';
  model.component = '';
  model.perms = '';
  model.icon = '';
  model.menuType = undefined;
  model.status = true;
  model.orderNo = '';

  visible.value = true;
}
const addSub = (parentId: any) => {
  model.id = '';
  model.parentId = parentId;
  model.name = '';
  model.title = '';
  model.path = '';
  model.component = '';
  model.perms = '';
  model.icon = '';
  model.menuType = undefined;
  model.status = true;
  model.orderNo = '';

  visible.value = true;
}
const edit = (records: any) => {
  model.id = records.id;
  model.parentId = records.parentId;
  model.name = records.name;
  model.title = records.meta.title;
  model.path = records.path;
  model.component = records.component;
  model.perms = records.perms;
  model.icon = records.meta.icon;
  model.menuType = records.menuType;
  model.status = records.status;
  model.orderNo = records.orderNo;

  visible.value = true;
}

const treeData = ref<TreeSelectProps['treeData']>([]);

const getTreeData = async () => {
  const res = await getAction(url.selectMenu, {});
  treeData.value = res.data.records;
}
getTreeData()
const handleOk = () => {
  rulesRef.value.validate().then(() => {
    let httpurl = '';
    let method = '';
    if (model.id === '') {
      httpurl += url.add;
      method = 'post';
    } else {
      httpurl += url.edit;
      method = 'put';
    }
    httpAction(httpurl, model, method).then((res: any) => {
      message.success(res.msg);
      emit('childOK');
    })
    visible.value = false;
  }).catch(() => {
    message.error('输入有误，请重新输入');
  })
};

//子组件方法默认为私有
defineExpose({
  add,
  addSub,
  edit
})
</script>