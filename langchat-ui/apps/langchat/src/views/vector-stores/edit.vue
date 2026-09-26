<script setup lang="ts">
import type { AigcVectorStore } from '#/api/aigc/vector-store';

import { computed, watch } from 'vue';
import { useVbenDrawer } from '@vben/common-ui';

import { useVbenForm, type VbenFormSchema } from '#/adapter/form';
import { VECTOR_PROVIDER_OPTIONS } from '#/views/shared/aigc/options';

interface Props {
  modelValue?: Partial<AigcVectorStore> | null;
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
  props.modelValue?.id ? '编辑向量库' : '新建向量库',
);

const formSchema: VbenFormSchema[] = [
  {
    component: 'Input',
    fieldName: 'name',
    label: '向量库名称',
    rules: 'required',
  },
  {
    component: 'Select',
    componentProps: {
      options: VECTOR_PROVIDER_OPTIONS,
    },
    fieldName: 'provider',
    label: '供应商',
    rules: 'selectRequired',
  },
  {
    component: 'Input',
    fieldName: 'host',
    label: '主机地址',
    rules: 'required',
  },
  {
    component: 'InputNumber',
    componentProps: { min: 1 },
    fieldName: 'port',
    label: '端口',
  },
  {
    component: 'Input',
    fieldName: 'databaseName',
    label: '数据库名',
  },
  {
    component: 'Input',
    fieldName: 'tableName',
    label: '表名/集合名',
  },
  {
    component: 'InputNumber',
    componentProps: { min: 1 },
    fieldName: 'dimension',
    label: '向量维度',
  },
  {
    component: 'Input',
    fieldName: 'username',
    label: '用户名',
  },
  {
    component: 'Input',
    componentProps: {
      showPasswordOn: 'click',
      type: 'password',
    },
    fieldName: 'password',
    label: '密码',
  },
];

const [Form, formApi] = useVbenForm({
  layout: 'vertical',
  schema: formSchema,
  showDefaultActions: false,
  wrapperClass: 'grid-cols-1 gap-x-4 px-3 sm:grid-cols-2',
});

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
        port: 5432,
        provider: 'PGVECTOR',
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
