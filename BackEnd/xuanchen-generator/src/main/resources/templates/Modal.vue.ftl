<template>
    <a-modal v-model:open="visible" :title="operationTitle" :width="500" @ok="handleOk" ok-text="确认"
             cancel-text="取消">
        <a-form layout="inline" :model="model" :rules="rules" ref="rulesRef" autoComplete="off"
                class="modal-form-style">
            <a-form-item label="选择" :labelCol="labelCol" :wrapperCol="wrapperCol">
                <a-select v-model:value="model.select" style="width: 100%;" :options="optionsSelect"
                          :fieldNames="{ label: 'dictItemText', value: 'dictItemValue' }" placeholder="请选择"
                          allowClear>
                </a-select>
            </a-form-item>
            <a-form-item name="input" label="文字" :labelCol="labelCol" :wrapperCol="wrapperCol">
                <a-input v-model:value="model.input" placeholder="请输入文字" allowClear/>
            </a-form-item>
            <a-form-item label="数字" :labelCol="labelCol" :wrapperCol="wrapperCol">
                <a-input-number v-model:value="model.inputNumber" placeholder="请输入数字" style="width: 100%;"
                                allowClear/>
            </a-form-item>
            <a-form-item label="时间" :labelCol="labelCol" :wrapperCol="wrapperCol">
                <a-date-picker v-model:value="model.datetime" value-format="YYYY-MM-DD" placeholder="请选择时间"
                               style="width: 100%;" allowClear/>
            </a-form-item>
            <a-form-item label="多行文字" :labelCol="labelCol" :wrapperCol="wrapperCol">
                <a-textarea v-model:value="model.text" placeholder="请输入多行文字" allow-clear/>
            </a-form-item>
            <a-form-item label="状态" :labelCol="labelCol" :wrapperCol="wrapperCol">
                <a-switch v-model:checked="model.status" checked-children="启用" un-checked-children="停用"/>
            </a-form-item>
            <a-form-item label="排序码" :labelCol="labelCol" :wrapperCol="wrapperCol">
                <a-input-number v-model:value="model.orderNo" placeholder="请输入排序码" allowClear
                                style="width: 100%"/>
            </a-form-item>
        </a-form>
    </a-modal>
</template>

<script setup lang="ts">
    import {reactive, ref} from 'vue';
    import type {Rule} from 'ant-design-vue/es/form';
    import {getAction, httpAction} from '@/utils/httpAction';
    import {message} from 'ant-design-vue';

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
        add: '/add',
        edit: '/edit',
        validate: '/validate',
        optionsSelect: '/optionsSelect',
    }
    const visible = ref(false);

    const model = reactive({
        id: '',
        select: undefined,
        input: '',
        inputNumber: '',
        datetime: '',
        text: '',
        status: true,
        orderNo: '',
    })

    const optionsSelect = ref([])
    const getOptionsSelect = () => {
        getAction(url.optionsSelect, {}).then((res: any) => {
            if (res.code === 200) {
                optionsSelect.value = res.data;
            }
        })
    }
    getOptionsSelect()

    const validate = async (rule: Rule, value: string) => {
        const res: any = await getAction(url.validate,
            {id: model.id, input: value});
        if (res.code === 500) {
            return Promise.reject(res.msg);
        } else {
            return Promise.resolve();
        }
    }

    const rulesRef = ref()
    const rules: Record<string, Rule[]> = {
        input: [
            {required: true, message: '请输入文字', trigger: 'blur'},
            {required: true, validator: validate, trigger: 'blur'}
        ],
    }

    //打开弹窗
    const add = () => {
        visible.value = true;
        if (rulesRef.value) {
            rulesRef.value.resetFields();
        }
        model.id = '';
        model.status = true;
        model.orderNo = '';
    }
    const edit = (records: any) => {
        visible.value = true;
        if (rulesRef.value) {
            rulesRef.value.resetFields();
        }
        model.id = records.id;
        model.status = records.status;
        model.orderNo = records.orderNo;
    }

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
        edit
    })
</script>