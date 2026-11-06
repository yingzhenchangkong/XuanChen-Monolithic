<#-- 控件渲染宏：按字段配置的编辑控件类型分派。
     字典类下拉统一走 XCSelect（组件内按 dictCode 自动加载选项，不再手工 getAction 拉字典）；
     图片上传走 XCUploadImage（v-model:file-list 为字符串数组，单值字段用 computed 桥接，桥名由 bridgeName 传入）。 -->
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
<#list editFields as f>
    <#if f.editType == "Select" || f.editType == "Radio" || f.editType == "Checkbox" || f.editType == "TreeSelect"><#assign hasDictField = true/></#if>
</#list>
<#assign hasUpload = false/>
<#list editFields as f>
    <#if f.editType == "Upload"><#assign hasUpload = true/></#if>
</#list>
<template>
    <!-- 弹窗统一项目规范组件 XCModal（v-model:open / title / width / ok 事件） -->
    <XCModal v-model:open="visible" :title="operationTitle" :width="520"
             ok-text="确认" cancel-text="取消" @ok="handleOk">
        <!--表单区域（编辑字段按代码生成器字段配置渲染）-->
        <a-form layout="inline" :model="model" :rules="rules" ref="rulesRef" autoComplete="off"
                class="modal-form-style">
            <a-form-item name="id" label="主键" :labelCol="labelCol" :wrapperCol="wrapperCol" v-show="false">
                <a-input v-model:value="model.id" />
            </a-form-item>
<#list editFields as f>
            <a-form-item name="${f.property}" label="${f.comment}" :labelCol="labelCol" :wrapperCol="wrapperCol">
            <@renderControl "model" f "${f.property}Files"/>
            </a-form-item>
</#list>
        </a-form>
    </XCModal>
</template>

<script setup lang="ts">
    import {<#if hasUpload>computed, </#if>reactive, ref} from 'vue';
    import type {Rule} from 'ant-design-vue/es/form';
    import {httpAction} from '@/utils/httpAction';
    import {message} from 'ant-design-vue';
    import XCModal from '@/components/xuanchen/XCModal.vue';
<#if hasDictField>
    import XCSelect from '@/components/xuanchen/XCSelect.vue';
</#if>
<#if hasUpload>
    import XCUploadImage from '@/components/xuanchen/XCUploadImage.vue';
</#if>

    defineProps({
        operationTitle: {
            type: String,
            default: '编辑'
        },
    })

    const labelCol = {span: 4};
    const wrapperCol = {span: 18};

    const emit = defineEmits(['childOK']);
    const url = {
        add: '${urlBase}/add',
        edit: '${urlBase}/edit',
    }
    const visible = ref(false);

    const model = reactive({
        id: '',
<#list editFields as f>
        ${f.property}: <#if f.editType == "Switch"><#if f.javaType == "Boolean">false<#else>0</#if><#else>''</#if>,
</#list>
    })
<#list editFields as f>
    <#if f.editType == "Upload">

    /** ${f.comment}：单值字段 ↔ XCUploadImage 字符串数组桥接 */
    const ${f.property}Files = computed({
        get: () => (model.${f.property} ? [model.${f.property}] : []),
        set: (val: string[]) => { model.${f.property} = val.length ? val[0] : ''; },
    });
    </#if>
</#list>

    const rulesRef = ref()
    const rules: Record<string, Rule[]> = {
<#list editFields as f>
    <#if f.isRequired>
        ${f.property}: [
            {required: true, message: '<#if f.editType == "Input" || f.editType == "InputNumber">请输入<#else>请选择</#if>${f.comment}', trigger: '<#if f.editType == "Input" || f.editType == "InputNumber">blur<#else>change</#if>'},
        ],
    </#if>
</#list>
    }
    //打开弹窗（新增）
    const add = () => {
        visible.value = true;
        if (rulesRef.value) {
            rulesRef.value.resetFields();
        }
        model.id = '';
<#list editFields as f>
        model.${f.property} = <#if f.editType == "Switch"><#if f.javaType == "Boolean">false<#else>0</#if><#else>''</#if>;
</#list>
    }
    //打开弹窗（编辑，记录回显按字段配置展开）
    const edit = (records: any) => {
        visible.value = true;
        if (rulesRef.value) {
            rulesRef.value.resetFields();
        }
        model.id = records.id;
<#list editFields as f>
        model.${f.property} = records.${f.property} ?? <#if f.editType == "Switch"><#if f.javaType == "Boolean">false<#else>0</#if><#else>''</#if>;
</#list>
    }

    const handleOk = async () => {
        try {
            await rulesRef.value.validate();
        } catch {
            message.error('输入有误，请重新输入');
            return;
        }
        let httpurl = '';
        let method = '';
        if (model.id === '') {
            httpurl = url.add;
            method = 'post';
        } else {
            httpurl = url.edit;
            method = 'put';
        }
        let res: any;
        try {
            res = await httpAction(httpurl, model, method);
        } catch {
            // 网络层 / HTTP 错误已由 axios 拦截器统一提示
            return;
        }
        if (res.code !== 200) {
            message.error(res.msg || '保存失败');
            return;
        }
        message.success(res.msg);
        visible.value = false;
        emit('childOK');
    };

    //子组件方法默认为私有
    defineExpose({
        add,
        edit
    })
</script>
