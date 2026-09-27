<script setup lang="ts">
import type { AigcDocs } from '#/api/aigc/docs';
import type { LabelOption } from '#/views/shared/aigc/options';

import { computed, watch } from 'vue';
import { useVbenDrawer } from '@vben/common-ui';
import { $t } from '@vben/locales';

import { useVbenForm, type VbenFormSchema } from '#/adapter/form';
import { docTypeOptions } from '#/views/shared/aigc/options';

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
  props.modelValue?.id ? $t('docs.title.edit') : $t('docs.title.create'),
);

const formSchema = computed<VbenFormSchema[]>(() => [
  {
    component: 'Input',
    fieldName: 'name',
    label: $t('docs.form.name'),
    rules: 'required',
  },
  {
    component: 'Select',
    componentProps: {
      clearable: true,
      options: props.knowledgeOptions,
    },
    fieldName: 'knowledgeId',
    label: $t('docs.form.knowledge'),
    rules: 'selectRequired',
  },
  {
    component: 'Select',
    componentProps: {
      options: docTypeOptions(),
    },
    fieldName: 'type',
    label: $t('docs.form.type'),
    rules: 'selectRequired',
  },
  {
    component: 'Switch',
    fieldName: 'enabled',
    label: $t('docs.form.enabled'),
  },
  {
    component: 'Input',
    fieldName: 'ext',
    label: $t('docs.form.ext'),
  },
  {
    component: 'InputNumber',
    componentProps: {
      min: 0,
    },
    fieldName: 'size',
    label: $t('docs.form.size'),
  },
  {
    component: 'Input',
    fieldName: 'ossId',
    label: $t('docs.form.resourceId'),
  },
  {
    component: 'Input',
    fieldName: 'parentId',
    label: $t('docs.form.parentId'),
  },
  {
    component: 'Input',
    componentProps: {
      autosize: { maxRows: 6, minRows: 3 },
      type: 'textarea',
    },
    fieldName: 'url',
    formItemClass: 'sm:cols-span-2',
    label: $t('docs.form.url'),
  },
  {
    component: 'Input',
    componentProps: {
      autosize: { maxRows: 6, minRows: 3 },
      type: 'textarea',
    },
    fieldName: 'pdfUrl',
    formItemClass: 'sm:cols-span-2',
    label: $t('docs.form.pdfUrl'),
  },
  {
    component: 'Input',
    componentProps: {
      autosize: { maxRows: 12, minRows: 6 },
      type: 'textarea',
    },
    fieldName: 'content',
    formItemClass: 'sm:cols-span-2',
    label: $t('docs.form.content'),
  },
  {
    component: 'Input',
    componentProps: {
      autosize: { maxRows: 8, minRows: 4 },
      type: 'textarea',
    },
    fieldName: 'ingestionConfig',
    formItemClass: 'sm:cols-span-2',
    label: $t('docs.form.ingestionConfig'),
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
  confirmText: $t('common.actions.save'),
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
  () => $t('common.actions.save'),
  (value) => {
    drawerApi.setState({ confirmText: value });
  },
  { immediate: true },
);

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
