<template>
  <a-card>
    <!--操作按钮区域-->
    <div class="btn-style">
      <a-button type="primary" @click="handleAdd">
        <template #icon>
          <PlusOutlined />
        </template>新增
      </a-button>
      <a-button @click="handleAddChild">
        <template #icon>
          <PlusOutlined />
        </template>添加下级
      </a-button>
    </div>
    <a-tree v-model:expandedKeys="expandedKeys" v-model:selectedKeys="selectedKeys" :tree-data="treeData"
      :show-line="true">
      <template #title="{ key: treeKey, title, dataRef }">
        <a-dropdown :trigger="['contextmenu']">
          <span>{{ title }}</span>
          <template #overlay>
            <a-menu>
              <a-menu-item key="1" @click="handleAddChildDepend(dataRef)">添加下级</a-menu-item>
              <a-menu-item key="2">
                <a-popconfirm title="确定删除吗？" @confirm="() => handleDelete(treeKey)" placement="right">
                  <a style="color: red">删除</a>
                </a-popconfirm>
              </a-menu-item>
            </a-menu>
          </template>
        </a-dropdown>
      </template>
    </a-tree>
  </a-card>
</template>
<script setup lang="ts">
import { ref, watch } from 'vue';
import { message } from 'ant-design-vue';

import { getDeptTreeApi, deleteDeptApi } from '../dept.api';
import type { DeptTranData } from '../dept.types';

const treeData = ref();

const expandedKeys = ref<string[]>([]);
const selectedKeys = ref<string[]>([]);
const emit = defineEmits(['childData']);

const getDeptTree = async () => {
  treeData.value = await getDeptTreeApi();
  // 获取所有节点的key并设置给expandedKeys
  const getAllKeys = (nodes: any[]): string[] => {
    return nodes.reduce((keys: string[], node) => {
      keys.push(node.key);
      if (node.children) {
        keys.push(...getAllKeys(node.children));
      }
      return keys;
    }, []);
  };
  expandedKeys.value = getAllKeys(treeData.value);
}
getDeptTree();

const handleAdd = () => {
  emit('childData', { selectedKey: '', ifAddChild: false });
}

function handleAddChild() {
  if (selectedKeys.value[0]) {
    emit('childData', { selectedKey: selectedKeys.value[0], ifAddChild: true });
  } else {
    message.warning('请选择部门！');
  }
}

function handleAddChildDepend(dataRef: any) {
  emit('childData', { selectedKey: dataRef.key, ifAddChild: true });
}

async function handleDelete(treeKey: string) {
  const res: any = await deleteDeptApi(treeKey);
  if (res.code === 200) {
    message.success(res.msg)
    await getDeptTree();
  } else {
    message.warning(res.msg)
  }
}

watch(selectedKeys, () => {
  if (selectedKeys.value[0]) {
    emit('childData', { selectedKey: selectedKeys.value[0], ifAddChild: false });
  }
});

const recParentData = (data: DeptTranData) => {
  selectedKeys.value = [data.selectedKey];
  getDeptTree();
}

defineExpose({
  recParentData
})
</script>