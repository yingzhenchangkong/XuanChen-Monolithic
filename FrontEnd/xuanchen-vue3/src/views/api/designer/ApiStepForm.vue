<template>
  <!-- SQL 查询步骤 -->
  <template v-if="step.type === 'sql'">
    <a-form-item label="数据源" class="step-field">
      <a-select v-model:value="step.datasource" placeholder="留空=主库" allowClear>
        <a-select-option value="" selected>主库</a-select-option>
        <a-select-option v-for="ds in datasourceList" :key="ds.id" :value="ds.id">
          {{ ds.dsName }} <span style="color:#999;font-size:12px">({{ ds.dsType }})</span>
        </a-select-option>
      </a-select>
    </a-form-item>
    <a-form-item label="SQL（仅允许 SELECT，:参数名 预编译占位）" class="step-field">
      <a-textarea v-model:value="step.sqlText" :rows="3"
        placeholder="SELECT id, user_name FROM md_person WHERE status = :st" />
    </a-form-item>
    <a-form-item :label="`参数（JSON 对象，值支持 ${exprHint} 取上下文）`" class="step-field">
      <a-textarea v-model:value="step.paramText" :rows="2" placeholder='{ "st": "{{params.status}}" }' />
    </a-form-item>
  </template>

  <!-- HTTP 拉取步骤 -->
  <template v-if="step.type === 'http'">
    <a-row :gutter="12">
      <a-col :span="16">
        <a-form-item label="URL" class="step-field">
          <a-input v-model:value="step.url" placeholder="https://api.example.com/data?city=xx" />
        </a-form-item>
      </a-col>
      <a-col :span="8">
        <a-form-item label="请求方法" class="step-field">
          <a-select v-model:value="step.method">
            <a-select-option value="GET">GET</a-select-option>
            <a-select-option value="POST">POST</a-select-option>
          </a-select>
        </a-form-item>
      </a-col>
    </a-row>
    <a-form-item label="请求头（JSON 对象，可空）" class="step-field">
      <a-textarea v-model:value="step.headerText" :rows="2" placeholder='{ "Authorization": "Bearer xxx" }' />
    </a-form-item>
    <a-form-item v-if="step.method === 'GET'" :label="`Query 参数（JSON 对象，可空，值支持 ${exprHint}）`" class="step-field">
      <a-textarea v-model:value="step.paramText" :rows="2" placeholder='{ "city": "北京" }' />
    </a-form-item>
    <a-form-item v-if="step.method === 'POST'" label="请求体（JSON 对象，可空）" class="step-field">
      <a-textarea v-model:value="step.bodyText" :rows="2" placeholder='{ "page": 1 }' />
    </a-form-item>
  </template>

  <!-- 字段映射步骤 -->
  <template v-if="step.type === 'map'">
    <a-form-item label="来源步骤名" class="step-field">
      <a-input v-model:value="step.mapSource" placeholder="如 query" />
    </a-form-item>
    <a-form-item label="字段映射（JSON 数组 [{from,to}]，from 支持多级路径）" class="step-field">
      <a-textarea v-model:value="step.fieldsText" :rows="2" placeholder='[{ "from": "user_name", "to": "userName" }]' />
    </a-form-item>
    <a-form-item label="保留来源全部字段">
      <a-switch v-model:checked="step.keepSource" size="small" />
    </a-form-item>
  </template>

  <!-- 采集落库步骤 -->
  <template v-if="step.type === 'save'">
    <a-form-item label="来源步骤名（其结果将逐条 JSON 落库到采集数据表）" class="step-field">
      <a-input v-model:value="step.saveSource" placeholder="如 query" />
    </a-form-item>
  </template>
</template>

<script setup lang="ts">
import type { ApiDatasourceRecord } from '../designer.api';

/** 步骤表单（按类型渲染配置项；直接编辑传入的草稿对象） */
defineProps<{ step: {
  type: 'sql' | 'http' | 'map' | 'save';
  name: string;
  /** sql 步骤的数据源：空=主库；其他=外部数据源 id */
  datasource: string;
  sqlText: string;
  paramText: string;
  url: string;
  method: string;
  headerText: string;
  bodyText: string;
  mapSource: string;
  keepSource: boolean;
  fieldsText: string;
  saveSource: string;
}; datasourceList: ApiDatasourceRecord[] }>();

/** 模板表达式提示文案（脚本常量，避免模板内嵌 {{ }} 提前闭合插值） */
const exprHint = '{{步骤名.路径}}';
</script>

<style scoped>
.step-field {
  margin-bottom: 8px;
}
</style>
