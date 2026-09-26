<script setup lang="ts">
import type { AigcDocs } from '#/api/aigc/docs';
import type { LabelOption } from '#/views/shared/aigc/options';

import { computed, watch } from 'vue';
import { useVbenDrawer } from '@vben/common-ui';

import { useVbenForm, type VbenFormSchema } from '#/adapter/form';
import { DOC_TYPE_OPTIONS } from '#/views/shared/aigc/options';

interface Props {
  knowledgeOptions: LabelOption[];
  modelValue?: Partial<AigcDocs> | null;
  saving?: boolean;
  show: boolean;
}

const props = withDefaults(defineProps<Props>(), {
  modelValue: null,
  saving: false,
});

const emit = defineEmits<{
  cancel: [];
  save: [payload: Partial<AigcDocs>];
  'update:show': [value: boolean];
}>();

const drawerTitle = computed(() =>
  props.modelValue?.id ? '编辑文档' : '新建文档',
);

const formSchema = computed<VbenFormSchema[]>(() => [
  {
    component: 'Input',
    fieldName: 'name',
    label: '文档名称',
    rules: 'required',
  },
  {
    component: 'Select',
    componentProps: {
      clearable: true,
      options: props.knowledgeOptions,
    },
    fieldName: 'knowledgeId',
    label: '所属知识库',
    rules: 'selectRequired',
  },
  {
    component: 'Select',
    componentProps: {
      options: DOC_TYPE_OPTIONS,
    },
    fieldName: 'type',
    label: '文档类型',
    rules: 'selectRequired',
  },
  {
    component: 'Switch',
    fieldName: 'enabled',
    label: '是否启用',
  },
  {
    component: 'Input',
    fieldName: 'ext',
    label: '文件后缀',
  },
  {
    component: 'InputNumber',
    componentProps: {
      min: 0,
    },
    fieldName: 'size',
    label: '文件大小',
  },
  {
    component: 'Input',
    fieldName: 'ossId',
    label: '资源文件 ID',
  },
  {
    component: 'Input',
    fieldName: 'parentId',
    label: '父节点 ID',
  },
  {
    component: 'Input',
    componentProps: {
      autosize: { maxRows: 6, minRows: 3 },
      type: 'textarea',
    },
    fieldName: 'url',
    formItemClass: 'sm:cols-span-2',
    label: '文件地址',
  },
  {
    component: 'Input',
    componentProps: {
      autosize: { maxRows: 6, minRows: 3 },
      type: 'textarea',
    },
    fieldName: 'pdfUrl',
    formItemClass: 'sm:cols-span-2',
    label: 'PDF 地址',
  },
  {
    component: 'Input',
    componentProps: {
      autosize: { maxRows: 12, minRows: 6 },
      type: 'textarea',
    },
    fieldName: 'content',
    formItemClass: 'sm:cols-span-2',
    label: '文档内容',
  },
  {
    component: 'Input',
    componentProps: {
      autosize: { maxRows: 8, minRows: 4 },
      type: 'textarea',
    },
    fieldName: 'ingestionConfig',
    formItemClass: 'sm:cols-span-2',
    label: '向量化配置 JSON',
  },
]);

const [Form, formApi] = useVbenForm({
  layout: 'vertical',
  schema: formSchema.value,
  showDefaultActions: false,
  wrapperClass: 'grid-cols-1 px-3',
});

watch(
  () => formSchema.value,
  (schema) => {
    formApi.setState({ schema });
  },
  { immediate: true },
);

function emitClose(withCancel: boolean = true) {
  emit('update:show', false);
  if (withCancel) {
    emit('cancel');
  }
}

async function handleSave() {
  const values = await formApi.validateAndSubmitForm();
  if (!values) {
    return;
  }
  emit('save', values as Partial<AigcDocs>);
}

const [Drawer, drawerApi] = useVbenDrawer({
  class: 'w-[860px]',
  confirmText: '保存',
  onCancel: () => emitClose(true),
  onConfirm: handleSave,
  onOpenChange: (isOpen) => {
    if (!isOpen) {
      emitClose(true);
    }
  },
  title: drawerTitle.value,
});

watch(
  () => props.saving,
  (value) => {
    drawerApi.setState({ confirmLoading: value });
  },
  { immediate: true },
);

watch(
  () => drawerTitle.value,
  (value) => {
    drawerApi.setState({ title: value });
  },
  { immediate: true },
);

watch(
  () => [props.show, props.modelValue],
  async () => {
    if (!props.show) {
      drawerApi.close();
      return;
    }
    drawerApi.open();

    await formApi.resetForm();
    await formApi.setValues(
      {
        embedStatus: 'pending',
        enabled: true,
        type: 'TEXT',
        ...(props.modelValue ?? {}),
      },
      false,
    );
  },
  { immediate: true },
);
</script>

<template>
  <Drawer>
    <Form />
  </Drawer>
</template>
