<script setup lang="ts">
import type { AigcMcp } from '#/api/aigc/mcp';

import { computed, watch } from 'vue';

import { useVbenDrawer } from '@vben/common-ui';
import { $t } from '@vben/locales';

import { useVbenForm, type VbenFormSchema } from '#/adapter/form';
import {
  aigcCommonTagOptions,
  MCP_TRANSPORT_OPTIONS,
} from '#/views/shared/aigc/options';
import { parseTagList, stringifyTagList } from '#/views/shared/aigc/tags';

interface Props {
  modelValue?: null | Partial<AigcMcp>;
  saving?: boolean;
  show: boolean;
}

const props = withDefaults(defineProps<Props>(), {
  modelValue: null,
  saving: false,
});

const emit = defineEmits<{
  cancel: [];
  save: [payload: Partial<AigcMcp>];
  'update:show': [value: boolean];
}>();

const drawerTitle = computed(() =>
  props.modelValue?.id ? $t('mcp.title.edit') : $t('mcp.title.create'),
);

function isNetworkTransport(transport?: string) {
  return transport === 'HTTP' || transport === 'SSE';
}

function isDockerTransport(transport?: string) {
  return transport === 'DOCKER';
}

function isStdioTransport(transport?: string) {
  return transport === 'STDIO';
}

const formSchema = computed<VbenFormSchema[]>(() => [
  {
    component: 'Input',
    fieldName: 'name',
    label: $t('mcp.form.name'),
    rules: 'required',
  },
  {
    component: 'Input',
    fieldName: 'uuid',
    label: $t('mcp.form.uuid'),
  },
  {
    component: 'Select',
    componentProps: {
      options: MCP_TRANSPORT_OPTIONS,
    },
    fieldName: 'transport',
    label: $t('mcp.form.transport'),
  },
  {
    component: 'InputNumber',
    componentProps: {
      min: 1,
    },
    fieldName: 'timeout',
    label: $t('mcp.form.timeout'),
  },
  {
    component: 'Switch',
    fieldName: 'authorized',
    label: $t('mcp.form.authorized'),
  },
  {
    component: 'Input',
    fieldName: 'coverUrl',
    label: $t('mcp.form.coverUrl'),
  },
  {
    component: 'Input',
    fieldName: 'siteUrl',
    label: $t('mcp.form.siteUrl'),
    dependencies: {
      if: (values) => isNetworkTransport(String(values.transport ?? '')),
      triggerFields: ['transport'],
    },
  },
  {
    component: 'Input',
    fieldName: 'sseUrl',
    label: $t('mcp.form.sseUrl'),
    dependencies: {
      if: (values) => isNetworkTransport(String(values.transport ?? '')),
      triggerFields: ['transport'],
    },
  },
  {
    component: 'Input',
    fieldName: 'dockerImage',
    label: $t('mcp.form.dockerImage'),
    dependencies: {
      if: (values) => isDockerTransport(String(values.transport ?? '')),
      triggerFields: ['transport'],
    },
  },
  {
    component: 'Input',
    fieldName: 'dockerHost',
    label: $t('mcp.form.dockerHost'),
    dependencies: {
      if: (values) => isDockerTransport(String(values.transport ?? '')),
      triggerFields: ['transport'],
    },
  },
  {
    component: 'Select',
    componentProps: {
      clearable: true,
      filterable: true,
      multiple: true,
      options: aigcCommonTagOptions(),
      placeholder: $t('common.placeholder.selectTags'),
    },
    fieldName: 'tags',
    formItemClass: 'sm:col-span-2',
    label: $t('common.labels.tags'),
  },
  {
    component: 'Input',
    componentProps: {
      autosize: { maxRows: 8, minRows: 4 },
      type: 'textarea',
    },
    fieldName: 'headers',
    formItemClass: 'sm:col-span-2',
    label: $t('mcp.form.requestHeaders'),
    dependencies: {
      if: (values) => isNetworkTransport(String(values.transport ?? '')),
      triggerFields: ['transport'],
    },
  },
  {
    component: 'CodeEditor',
    componentProps: {
      height: 240,
      language: 'json',
      placeholder: $t('mcp.form.stdioPlaceholder'),
    },
    fieldName: 'mcpJson',
    formItemClass: 'sm:col-span-2',
    label: $t('mcp.form.stdioConfig'),
    dependencies: {
      if: (values) => isStdioTransport(String(values.transport ?? '')),
      triggerFields: ['transport'],
    },
  },
  {
    component: 'Input',
    componentProps: {
      autosize: { maxRows: 8, minRows: 4 },
      type: 'textarea',
    },
    fieldName: 'description',
    formItemClass: 'sm:col-span-2',
    label: $t('mcp.form.description'),
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
  const payload = values as Record<string, any>;
  emit('save', {
    ...payload,
    tags: stringifyTagList(payload.tags),
  } as Partial<AigcMcp>);
}

const [Drawer, drawerApi] = useVbenDrawer({
  class: 'w-[820px]',
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
        authorized: false,
        timeout: 60,
        transport: 'SSE',
        ...props.modelValue,
        tags: parseTagList(props.modelValue?.tags),
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
