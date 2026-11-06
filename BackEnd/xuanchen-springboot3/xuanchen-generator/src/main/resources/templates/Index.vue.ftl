<template>
    <!-- 列表页按项目统一规范：XCListPage（查询区 / 工具栏 / 表格 / 分页 / 弹窗插槽） -->
    <XCListPage :url="url" :columns="columns" :query-form-items="queryFormItems" :query-params="queryParams"
                :buttons="{ add: true, import: true, export: true, deleteBatch: true }"
                export-title="${tableComment?replace("'", "")?replace("\"", "")}"
<#if hasSub>
                selection="radio" @selection-change="handleSelectionChange" @search="handleSearch"
                @reset="handleReset"
</#if>
    >
<#if hasSub>
        <!-- 操作列：删除时同步清空下方子表明细的选中行（字典管理式主子表） -->
        <template #operation="{ record, handleEdit, handleDelete, loadData }">
            <a @click="handleEdit(record)">
                <EditOutlined /> 编辑
            </a>
            <a-divider type="vertical"></a-divider>
            <a-popconfirm title="确定删除吗？" placement="left"
                          @confirm="() => { mainId = ''; handleDelete(record.id, loadData); }">
                <a>
                    <DeleteOutlined /> 删除
                </a>
            </a-popconfirm>
        </template>

        <!-- 字典管理式主子表：主表列表单选一行，下方维护该行记录的子表明细 -->
        <template #footer>
            <SubList :main-id="mainId" />
        </template>
</#if>
        <!-- 弹窗区域：XCListPage 内部统一管理「新增 / 编辑」标题与弹窗引用 -->
        <template #modals="{ operationTitle, setOperationRef, loadData }">
            <Operation :operation-title="operationTitle" :ref="setOperationRef" @childOK="loadData"/>
        </template>
    </XCListPage>
</template>

<script lang="ts" setup>
import { <#if hasSub>ref, </#if>reactive } from 'vue';
import XCListPage from '@/components/xuanchen/XCListPage.vue';
<#assign hasDictQuery = false/>
<#list queryFields as f>
    <#if f.queryType == "Select" || f.queryType == "Radio" || f.queryType == "Checkbox" || f.queryType == "TreeSelect">
        <#assign hasDictQuery = true/>
    </#if>
</#list>
<#if hasDictQuery>
import { getAction } from '@/utils/httpAction';
</#if>
import Operation from './modal/Operation.vue';
<#if hasSub>
import SubList from './SubList.vue';
</#if>

/** url */
const url = reactive({
    list: '${urlBase}/list',
    delete: '${urlBase}/delete',
    deleteBatch: '${urlBase}/deleteBatch',
    exportExcel: '${urlBase}/exportExcel',
    importExcel: '${urlBase}/importExcel',
})

/** 查询参数（XCListPage 会与 XCQueryForm 共用同一份对象） */
const queryParams = reactive<any>({
<#list queryFields as f>
    ${f.property}: undefined,
</#list>
})

/** 查询表单项：控件类型按代码生成器字段配置渲染 */
const queryFormItems = reactive<any[]>([
<#list queryFields as f>
    {
        name: '${f.property}',
        label: '${f.comment?replace("'", "\\'")}',
    <#if f.queryType == "Switch">
        type: 'select',
        options: [{ label: '启用', value: 1 }, { label: '停用', value: 0 }],
        placeholder: '请选择${f.comment?replace("'", "\\'")}',
    <#elseif f.queryType == "Select" || f.queryType == "Radio" || f.queryType == "Checkbox" || f.queryType == "TreeSelect">
        type: 'select',
        options: [],
        fieldNames: { label: 'dictItemText', value: 'dictItemValue' },
        placeholder: '请选择${f.comment?replace("'", "\\'")}',
    <#elseif f.queryType == "DatePicker">
        type: 'datePicker',
        showTime: <#if f.javaType == "LocalDateTime">true<#else>false</#if>,
        valueFormat: '<#if f.javaType == "LocalDateTime">YYYY-MM-DD HH:mm:ss<#else>YYYY-MM-DD</#if>',
        placeholder: '请选择${f.comment?replace("'", "\\'")}',
    <#else>
        type: 'input',
        placeholder: '请输入${f.comment?replace("'", "\\'")}',
    </#if>
    },
</#list>
]);

/** 定义表格的列 */
const columns = [
<#list listFields as f>
    {
        title: '${f.comment?replace("'", "\\'")}',
        dataIndex: '${f.property}',
        align: 'center',
        width: 150
    },
</#list>
    {
        title: '操作',
        dataIndex: 'operation',
        align: 'center',
        width: 150
    },
];
<#if hasSub>

/** 当前选中的主表记录 id（驱动下方子表明细区） */
const mainId = ref('');

/** 单选变化：取选中行的主键给子表明细当外键 */
const handleSelectionChange = (keys: (string | number)[], rows: Record<string, unknown>[]) => {
    mainId.value = keys.length > 0 ? String(rows[0]?.id ?? '') : '';
};
/** 搜索 / 重置后选中依据失效，一并清空明细 */
const handleSearch = () => { mainId.value = ''; };
const handleReset = () => { mainId.value = ''; };
</#if>
<#if hasDictQuery>

/** 查询区字典选项（字典编码来自代码生成器字段配置） */
const loadDictOptions = () => {
<#list queryFields as f>
    <#if f.queryType == "Select" || f.queryType == "Radio" || f.queryType == "Checkbox" || f.queryType == "TreeSelect">
    getAction('/system/dict/select', { dictCode: '<#if f.dictCode?? && f.dictCode?has_content>${f.dictCode}<#else>TODO_${f.columnName}</#if>' }).then((res: any) => {
        if (res.code === 200) {
            const item = queryFormItems.find((i: any) => i.name === '${f.property}');
            if (item) {
                item.options = res.data || [];
            }
        }
    });
    </#if>
</#list>
};
loadDictOptions();
</#if>
</script>
