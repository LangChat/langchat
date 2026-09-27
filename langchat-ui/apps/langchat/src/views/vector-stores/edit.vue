<script setup lang="ts">
import type { AigcVectorStore } from '#/api/aigc/vector-store';

import { computed, watch } from 'vue';

import { useVbenDrawer } from '@vben/common-ui';
import { $t } from '@vben/locales';

import { useVbenForm, type VbenFormSchema } from '#/adapter/form';
import { VECTOR_PROVIDER_OPTIONS } from '#/views/shared/aigc/options';

interface Props {
  modelValue?: null | Partial<AigcVectorStore>;
  saving?: boolean;
  show: boolean;
}

const props = withDefaults(defineProps<Props>(), {
  modelValue: null,
  saving: false,
});

const emit = defineEmits<{
  cancel: [];
  save: [payload: Partial<AigcVectorStore>];
  'update:show': [value: boolean];
}>();

const drawerTitle = computed(() =>
  props.modelValue?.id ? $t('vectorStores.title.edit') : $t('vectorStores.title.create'),
);

const formSchema = computed<VbenFormSchema[]>(() => [
  {
    component: 'Input',
    fieldName: 'name',
    label: $t('vectorStores.form.name'),
    rules: 'required',
  },
  {
    component: 'Select',
    componentProps: {
      options: VECTOR_PROVIDER_OPTIONS,
    },
    fieldName: 'provider',
    label: $t('common.labels.provider'),
    rules: 'selectRequired',
  },
  {
    component: 'Input',
    fieldName: 'host',
    label: $t('vectorStores.form.host'),
    rules: 'required',
  },
  {
    component: 'InputNumber',
    componentProps: { min: 1 },
    fieldName: 'port',
    label: $t('common.labels.port'),
  },
  {
    component: 'Input',
    fieldName: 'databaseName',
    label: $t('vectorStores.form.database'),
  },
  {
    component: 'Input',
    fieldName: 'tableName',
    label: $t('vectorStores.form.table'),
  },
  {
    component: 'InputNumber',
    componentProps: { min: 1 },
    fieldName: 'dimension',
    label: $t('vectorStores.form.dimension'),
  },
  {
    component: 'Input',
    fieldName: 'username',
    label: $t('common.labels.username'),
  },
  {
    component: 'Input',
    componentProps: {
      showPasswordOn: 'click',
      type: 'password',
    },
    fieldName: 'password',
    label: $t('common.labels.password'),
  },
]);

const [Form, formApi] = useVbenForm({
  layout: 'vertical',
  schema: formSchema.value,
  showDefaultActions: false,
  wrapperClass: 'grid-cols-1 gap-x-4 px-3 sm:grid-cols-2',
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
  emit('save', values as Partial<AigcVectorStore>);
}

const [Drawer, drawerApi] = useVbenDrawer({
  class: 'w-[760px]',
  onCancel: () => emitClose(true),
  onConfirm: handleSave,
  onOpenChange: (isOpen) => {
    if (!isOpen) {
      emitClose(true);
    }
  },
});

watch(
  () => props.saving,
  (value) => {
    drawerApi.setState({ confirmLoading: value });
  },
  { immediate: true },
);

watch(
  () => $t('common.actions.save'),
  (value) => {
    drawerApi.setState({ confirmText: value });
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
        port: 5432,
        provider: 'PGVECTOR',
        ...props.modelValue,
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
