<template>
    <a-card>
        <!-- 查询区域 -->
        <div class="query-style btn-style">
            <a-form layout="inline" :model="queryParams" @keyup.enter="loadData">
                <a-form-item label="下拉框">
                    <a-select v-model:value="queryParams.select" style="width: 150px" :options="optionsSelect"
                              :fieldNames="{ label: 'dictItemText', value: 'dictItemValue' }" placeholder="请选择下拉框" allowClear></a-select>
                </a-form-item>
                <a-form-item label="输入框">
                    <a-input v-model:value="queryParams.input" placeholder="请输入输入框" allowClear />
                </a-form-item>
                <a-button type="primary" @click="loadData">
                    <template #icon>
                        <SearchOutlined />
                    </template>查询
                </a-button>
                <a-button @click="handleReset">
                    <template #icon>
                        <ReloadOutlined />
                    </template>重置
                </a-button>
            </a-form>
        </div>
        <!--操作按钮区域-->
        <div class="btn-style">
            <a-button type="primary" @click="handleAdd">
                <template #icon>
                    <PlusOutlined />
                </template>新增
            </a-button>
            <a-upload name="file" :customRequest="handleImport" :showUploadList="false">
                <a-button>
                    <template #icon>
                        <UploadOutlined />
                    </template>导入
                </a-button>
            </a-upload>
            <a-button @click="handleExport('导出内容')">
                <template #icon>
                    <DownloadOutlined />
                </template>导出
            </a-button>
            <template v-if="state.selectedRowKeys.length > 0">
                <a-popconfirm title="确定删除吗？" @confirm="handledeleteBatch">
                    <a-button>
                        <template #icon>
                            <DeleteOutlined />
                        </template>批量删除
                    </a-button>
                </a-popconfirm>
                <a-button @click="handleCancelSelect">
                    <template #icon>
                        <UndoOutlined />
                    </template>取消选择
                </a-button>
                <a-tag color="processing" style="float: right;">
                    <template #icon>
                        <CheckSquareOutlined />
                    </template>
                    {{ `已选择 ${state.selectedRowKeys.length} 条` }}
                </a-tag>
            </template>
        </div>
        <!--表格区域-->
        <a-table :dataSource="dataSource" :columns="columns" :pagination="ipagination" :loading="loading"
                 :row-selection="{ selectedRowKeys: state.selectedRowKeys, onChange: onSelectChange }" bordered rowKey="id"
                 size="small" @change="handleTableChange">
            <template #bodyCell="{ column, text, record }">
                <template v-if="column.dataIndex === 'operation'">
                    <a @click="handleEdit(record)">
                        <EditOutlined />编辑
                    </a>
                    <a-divider type="vertical"></a-divider>
                    <a-popconfirm title="确定删除吗？" @confirm="() => handleDelete(record.id)" placement="left">
                        <a>
                            <DeleteOutlined />删除
                        </a>
                    </a-popconfirm>
                </template>
                <template v-else-if="column.dataIndex === 'status'">
                    <a-tag :color="record.status === true ? 'green' : 'volcano'">
                        {{ record.status === true ? '启用' : '停用' }}
                    </a-tag>
                </template>
            </template>
        </a-table>
        <!--弹窗区域-->
        <Operation ref="refOperation" :operationTitle="operationTitle" @childOK="loadData" />
    </a-card>
</template>

<script lang="ts" setup>
    import { useList } from '@/hooks/useList'
    import { reactive, ref } from 'vue';
    import Operation from './modal/Operation.vue';
    import { getAction } from '@/utils/httpAction';

    const optionsSelect = ref([]);

    /** url */
    const url = reactive({
        list: '/list',
        delete: '/delete',
        deleteBatch: '/deleteBatch',
        exportExcel: '/exportExcel',
        importExcel: '/importExcel',
        optionsSelect: '/optionsSelect',
    })
    /** 查询参数 */
    const queryParams = reactive({
        select: undefined,
        input: '',
    })
    /** 定义表格的列 */
    const columns = [
        {
            title: '#',
            dataIndex: '',
            key: 'rowIndex',
            align: 'center',
            width: 60,
            customRender:
                function (text: any, record: any, index: any, column: any) {
                    return parseInt(text.index) + 1;
                }
        },
        {
            title: '下拉框',
            dataIndex: 'select',
            align: 'center',
            width: 150
        },
        {
            title: '状态',
            dataIndex: 'status',
            align: 'center',
            width: 80
        },
        {
            title: '排序码',
            dataIndex: 'orderNo',
            align: 'center',
            width: 80
        },
        {
            title: '操作',
            dataIndex: 'operation',
            align: 'center',
            width: 150
        },
    ];

    const getOptionsSelect = () => {
        getAction(url.optionsSelect, { }).then((res: any) => {
            if (res.code == 200) {
                optionsSelect.value = res.data
            }
        })
    }
    getOptionsSelect();

    /** 重置 */
    const handleReset = () => {
        queryParams.input = '';
        queryParams.select = undefined;
        loadData()
    }

    const {
        loadData,
        operationTitle, refOperation,
        handleAdd, handleEdit, handleDelete, handledeleteBatch, handleImport, handleExport,
        dataSource, loading, ipagination, handleTableChange, state, onSelectChange, handleCancelSelect
    } = useList({ url, queryParams })
    loadData()
</script>