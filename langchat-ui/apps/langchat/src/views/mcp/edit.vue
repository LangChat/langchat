<script setup lang="ts">
import type { AigcMcp } from '#/api/aigc/mcp';

import { computed, watch } from 'vue';

import { useVbenDrawer } from '@vben/common-ui';

import { useVbenForm, type VbenFormSchema } from '#/adapter/form';
import {
  AIGC_COMMON_TAG_OPTIONS,
  MCP_TRANSPORT_OPTIONS,
} from '#/views/shared/aigc/options';
import { parseTagList, stringifyTagList } from '#/views/shared/aigc/tags';

interface Props {
  modelValue?: Partial<AigcMcp> | null;
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
  props.modelValue?.id ? '编辑 MCP 服务' : '新建 MCP 服务',
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
    label: '服务名称',
    rules: 'required',
  },
  {
    component: 'Input',
    fieldName: 'uuid',
    label: '唯一标识',
  },
  {
    component: 'Select',
    componentProps: {
      options: MCP_TRANSPORT_OPTIONS,
    },
    fieldName: 'transport',
    label: '协议类型',
  },
  {
    component: 'InputNumber',
    componentProps: {
      min: 1,
    },
    fieldName: 'timeout',
    label: '超时时间',
  },
  {
    component: 'Switch',
    fieldName: 'authorized',
    label: '是否授权',
  },
  {
    component: 'Input',
    fieldName: 'coverUrl',
    label: '封面地址',
  },
  {
    component: 'Input',
    fieldName: 'siteUrl',
    label: '站点地址',
    dependencies: {
      if: (values) => isNetworkTransport(String(values.transport ?? '')),
      triggerFields: ['transport'],
    },
  },
  {
    component: 'Input',
    fieldName: 'sseUrl',
    label: 'SSE 服务地址',
    dependencies: {
      if: (values) => isNetworkTransport(String(values.transport ?? '')),
      triggerFields: ['transport'],
    },
  },
  {
    component: 'Input',
    fieldName: 'dockerImage',
    label: 'Docker 镜像',
    dependencies: {
      if: (values) => isDockerTransport(String(values.transport ?? '')),
      triggerFields: ['transport'],
    },
  },
  {
    component: 'Input',
    fieldName: 'dockerHost',
    label: 'Docker 主机',
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
      options: AIGC_COMMON_TAG_OPTIONS,
      placeholder: '请选择标签',
    },
    fieldName: 'tags',
    formItemClass: 'sm:col-span-2',
    label: '标签',
  },
  {
    component: 'Input',
    componentProps: {
      autosize: { maxRows: 8, minRows: 4 },
      type: 'textarea',
    },
    fieldName: 'headers',
    formItemClass: 'sm:col-span-2',
    label: '请求头 JSON',
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
      placeholder: '请输入 Stdio 协议 JSON 配置',
    },
    fieldName: 'mcpJson',
    formItemClass: 'sm:col-span-2',
    label: 'MCP 配置 JSON',
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
    label: '描述',
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
        authorized: false,
        timeout: 60,
        transport: 'SSE',
        ...(props.modelValue ?? {}),
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
