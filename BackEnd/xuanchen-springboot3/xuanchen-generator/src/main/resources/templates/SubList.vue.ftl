<#-- 控件渲染宏：与 Modal.vue.ftl 保持同一套控件分派（XCSelect 字典下拉 / XCUploadImage 图片上传） -->
<#macro renderControl modelExpr f bridgeName>
                    <#switch f.editType>
                        <#case "InputNumber">
                    <a-input-number v-model:value="${modelExpr}.${f.property}" placeholder="请输入${f.comment}"
                                    style="width: 100%;" allowClear/>
                        <#break>
                        <#case "Select">
                        <#case "Radio">
                        <#case "Checkbox">
                        <#case "TreeSelect">
                    <XCSelect v-model="${modelExpr}.${f.property}" apiUrl="/system/dict/select"
                              :params="{ dictCode: '<#if f.dictCode?? && f.dictCode?has_content>${f.dictCode}<#else>TODO_${f.columnName}</#if>' }"
                              :fieldNames="{ label: 'dictItemText', value: 'dictItemValue' }"
                              placeholder="请选择${f.comment}"/>
                        <#break>
                        <#case "Switch">
                    <a-switch v-model:checked="${modelExpr}.${f.property}" checked-children="启用" un-checked-children="停用"<#if f.javaType != "Boolean"> :checked-value="1" :un-checked-value="0"</#if>/>
                        <#break>
                        <#case "DatePicker">
                    <a-date-picker v-model:value="${modelExpr}.${f.property}" value-format="<#if f.javaType == "LocalDateTime">YYYY-MM-DD HH:mm:ss<#else>YYYY-MM-DD</#if>"<#if f.javaType == "LocalDateTime"> show-time</#if>
                                   placeholder="请选择${f.comment}" style="width: 100%;" allowClear/>
                        <#break>
                        <#case "TimePicker">
                    <a-time-picker v-model:value="${modelExpr}.${f.property}" placeholder="请选择${f.comment}"
                                   style="width: 100%;" allowClear/>
                        <#break>
                        <#case "Upload">
                    <XCUploadImage v-model:file-list="${bridgeName}" :max-count="1"/>
                        <#break>
                        <#default>
                    <a-input v-model:value="${modelExpr}.${f.property}" placeholder="请输入${f.comment}" allowClear/>
                    </#switch>
</#macro>
<#assign hasDictField = false/>
<#assign hasUpload = false/>
<#list subTables as st>
    <#list st.editFields as sf>
        <#if sf.editType == "Select" || sf.editType == "Radio" || sf.editType == "Checkbox" || sf.editType == "TreeSelect"><#assign hasDictField = true/></#if>
        <#if sf.editType == "Upload"><#assign hasUpload = true/></#if>
    </#list>
</#list>
<template>
    <!-- 字典管理式子表明细：主表列表页单选一行后，在这里维护该行记录的子表数据 -->
    <a-tabs v-model:activeKey="activeKey">
<#list subTables as st>
        <a-tab-pane key="sub${st.index}" tab="${st.tableComment}">
            <div class="btn-style">
                <a-button type="primary" :disabled="!mainId" @click="handleAdd(${st.index})">
                    <template #icon>
                        <PlusOutlined />
                    </template>新增
                </a-button>
                <span v-if="!mainId" class="sublist-tip">请先在上方列表中选择一条记录</span>
            </div>
            <a-table :dataSource="states[${st.index}].data" :columns="subColumns[${st.index}]" rowKey="id"
                     size="small" :pagination="false" bordered :loading="states[${st.index}].loading">
                <template #bodyCell="{ column, record }">
                    <template v-if="column.dataIndex === 'operation'">
                        <a @click="handleEdit(${st.index}, record)">
                            <EditOutlined /> 编辑
                        </a>
                        <a-divider type="vertical"></a-divider>
                        <a-popconfirm title="确定删除吗？" placement="left"
                                      @confirm="() => handleDelete(${st.index}, record.id)">
                            <a>
                                <DeleteOutlined /> 删除
                            </a>
                        </a-popconfirm>
                    </template>
                </template>
            </a-table>

            <!-- ${st.tableComment} 新增 / 编辑弹窗（小弹窗，按项目规范不显示全屏按钮） -->
            <XCModal v-model:open="states[${st.index}].visible" :title="states[${st.index}].title"
                     :show-fullscreen-btn="false" ok-text="确认" cancel-text="取消" @ok="handleOk(${st.index})">
                <a-form layout="inline" :model="states[${st.index}].model" :rules="subRules[${st.index}]"
                        :ref="(el: any) => setRuleRef(${st.index}, el)" autoComplete="off" class="modal-form-style">
<#list st.editFields as sf>
    <#if sf.property != st.fkProperty>
                    <a-form-item name="${sf.property}" label="${sf.comment}" :labelCol="labelCol" :wrapperCol="wrapperCol">
                    <@renderControl "states[" + st.index + "].model" sf "sub${st.index}_${sf.property}Files"/>
                    </a-form-item>
    </#if>
</#list>
                </a-form>
            </XCModal>
        </a-tab-pane>
</#list>
    </a-tabs>
</template>

<script setup lang="ts">
    import {<#if hasUpload>computed, </#if>reactive, ref, watch} from 'vue';
    import type {Rule} from 'ant-design-vue/es/form';
    import {getAction, httpAction, deleteAction} from '@/utils/httpAction';
    import {message} from 'ant-design-vue';
    import XCModal from '@/components/xuanchen/XCModal.vue';
<#if hasDictField>
    import XCSelect from '@/components/xuanchen/XCSelect.vue';
</#if>
<#if hasUpload>
    import XCUploadImage from '@/components/xuanchen/XCUploadImage.vue';
</#if>

    const props = defineProps({
        /** 主表当前选中记录的主键（子表外键取值来源）；为空时明细区只读展示提示 */
        mainId: {
            type: String,
            default: ''
        },
    })

    const labelCol = {span: 4};
    const wrapperCol = {span: 18};

    const activeKey = ref('sub0');
    const ruleRefs: any[] = [];
    const setRuleRef = (index: number, el: any) => { ruleRefs[index] = el; };

    /** 每张子表一份独立状态（列表 / 弹窗 / 表单模型），避免多子表互相覆盖 */
    const states = reactive([
<#list subTables as st>
        {
            data: [] as any[],
            loading: false,
            visible: false,
            title: '',
            model: {
                id: '',
<#list st.editFields as sf>
    <#if sf.property != st.fkProperty>
                ${sf.property}: <#if sf.editType == "Switch"><#if sf.javaType == "Boolean">false<#else>0</#if><#else>''</#if>,
    </#if>
</#list>
            } as any,
        },
</#list>
    ]);
<#list subTables as st>
    <#list st.editFields as sf>
        <#if sf.editType == "Upload">

    /** 子表${st.index} ${sf.comment}：单值字段 ↔ XCUploadImage 字符串数组桥接 */
    const sub${st.index}_${sf.property}Files = computed({
        get: () => (states[${st.index}].model.${sf.property} ? [states[${st.index}].model.${sf.property}] : []),
        set: (val: string[]) => { states[${st.index}].model.${sf.property} = val.length ? val[0] : ''; },
    });
        </#if>
    </#list>
</#list>

    /** 子表接口与外键属性名，下标与 states 一一对应 */
    const subUrls = [
<#list subTables as st>
        {
            list: '${st.urlBase}/listByMainId',
            add: '${st.urlBase}/add',
            edit: '${st.urlBase}/edit',
            del: '${st.urlBase}/delete',
            fk: '${st.fkProperty}',
        },
</#list>
    ];

    const subRules = [
<#list subTables as st>
        {
<#list st.editFields as sf>
    <#if sf.property != st.fkProperty && sf.isRequired>
            ${sf.property}: [
                {required: true, message: '<#if sf.editType == "Input" || sf.editType == "InputNumber">请输入<#else>请选择</#if>${sf.comment}', trigger: '<#if sf.editType == "Input" || sf.editType == "InputNumber">blur<#else>change</#if>'},
            ],
    </#if>
</#list>
        },
</#list>
    ];

    const subColumns = [
<#list subTables as st>
        [
            {
                title: '#',
                dataIndex: '',
                key: 'rowIndex',
                align: 'center',
                width: 60,
                customRender: function (text: any) {
                    return parseInt(text.index) + 1;
                }
            },
<#list st.listFields as sf>
    <#if sf.property != st.fkProperty>
            {
                title: '${sf.comment?replace("'", "\\'")}',
                dataIndex: '${sf.property}',
                align: 'center',
                width: 120
            },
    </#if>
</#list>
            {
                title: '操作',
                dataIndex: 'operation',
                align: 'center',
                width: 120
            },
        ],
</#list>
    ];

    /** 加载某张子表：按主表主键过滤 */
    const loadSub = (index: number) => {
        const state = states[index];
        if (!props.mainId) {
            state.data = [];
            return;
        }
        state.loading = true;
        getAction(subUrls[index].list, {mainId: props.mainId}).then((res: any) => {
            if (res.code === 200) {
                state.data = res.data || [];
            }
        }).finally(() => {
            state.loading = false;
        });
    }
    const loadAll = () => {
        subUrls.forEach((_item: any, index: number) => loadSub(index));
    }
    /** 主表选中行变化即刷新明细；初次挂载也拉一次（父页面可能带选中进入） */
    watch(() => props.mainId, () => loadAll());
    loadAll();

    const resetModel = (index: number) => {
        const m = states[index].model;
        m.id = '';
<#list subTables as st>
        if (index === ${st.index}) {
<#list st.editFields as sf>
    <#if sf.property != st.fkProperty>
            m.${sf.property} = <#if sf.editType == "Switch"><#if sf.javaType == "Boolean">false<#else>0</#if><#else>''</#if>;
    </#if>
</#list>
        }
</#list>
    }

    const handleAdd = (index: number) => {
        resetModel(index);
        states[index].title = '添加';
        states[index].visible = true;
    }

    const handleEdit = (index: number, record: any) => {
        resetModel(index);
        Object.assign(states[index].model, record);
        states[index].title = '编辑';
        states[index].visible = true;
    }

    const handleOk = async (index: number) => {
        const state = states[index];
        try {
            if (ruleRefs[index]) {
                await ruleRefs[index].validate();
            }
        } catch {
            message.error('输入有误，请重新输入');
            return;
        }
        const payload = {...state.model, [subUrls[index].fk]: props.mainId};
        const httpurl = state.model.id ? subUrls[index].edit : subUrls[index].add;
        const method = state.model.id ? 'put' : 'post';
        let res: any;
        try {
            res = await httpAction(httpurl, payload, method);
        } catch {
            // 网络层 / HTTP 错误已由 axios 拦截器统一提示
            return;
        }
        if (res.code !== 200) {
            message.error(res.msg || '保存失败');
            return;
        }
        message.success(res.msg);
        state.visible = false;
        loadSub(index);
    }

    const handleDelete = (index: number, id: string) => {
        deleteAction(subUrls[index].del, {id}).then((res: any) => {
            if (res.code === 200) {
                message.success(res.msg);
                loadSub(index);
            } else {
                message.error(res.msg);
            }
        })
    }
</script>

<style scoped>
.sublist-tip {
    color: #8c8c8c;
    font-size: 12px;
    margin-left: 8px;
}
</style>
