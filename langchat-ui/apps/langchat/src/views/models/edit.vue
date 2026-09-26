<script lang="ts" setup>
import type { AigcModel } from '#/api/aigc/model';

import { computed, nextTick, ref, watch } from 'vue';
import { useVbenDrawer } from '@vben/common-ui';

import { type VbenFormSchema, useVbenForm } from '#/adapter/form';
import {
  getModelProviderOptions,
  getModelTypeConfigFields,
  type ModelTypeKey,
  getProviderBaseUrl,
  getProviderRecommendedModels,
  getProviderSupportedTypes,
  getProviderTypeOptions,
  normalizeModelType,
} from './model-meta';

interface Props {
  modelValue?: null | Partial<AigcModel>;
  saving?: boolean;
  show: boolean;
}

const props = withDefaults(defineProps<Props>(), {
  modelValue: null,
  saving: false,
});

const emit = defineEmits<{
  cancel: [];
  save: [payload: Partial<AigcModel>];
  'update:show': [value: boolean];
}>();

const drawerTitle = computed(() =>
  props.modelValue?.id ? '编辑模型' : '新建模型',
);

const ALL_CONFIG_FIELD_NAMES = [
  'maxToken',
  'temperature',
  'topP',
  'timeout',
  'dimension',
  'baseUrl',
  'apiKey',
] as const;
type ConfigFieldName = (typeof ALL_CONFIG_FIELD_NAMES)[number];
const configFieldNameSet = new Set<string>(ALL_CONFIG_FIELD_NAMES);

const CONNECTION_FIELD_NAMES = ['apiKey', 'baseUrl'] as const;

const configFieldSchemaMap: Record<ConfigFieldName, VbenFormSchema> = {
  apiKey: {
    component: 'Input',
    componentProps: {
      placeholder: '输入模型 API Key',
    },
    fieldName: 'apiKey',
    label: 'API Key',
  },
  baseUrl: {
    component: 'Input',
    componentProps: {
      placeholder: '例如：https://api.openai.com/v1',
    },
    fieldName: 'baseUrl',
    label: 'Base URL',
  },
  dimension: {
    component: 'Slider',
    componentProps: {
      max: 4096,
      min: 1,
      step: 1,
    },
    fieldName: 'dimension',
    label: 'dimension',
  },
  maxToken: {
    component: 'Slider',
    componentProps: {
      max: 32_768,
      min: 1,
      step: 1,
    },
    fieldName: 'maxToken',
    label: 'maxToken',
  },
  temperature: {
    component: 'Slider',
    componentProps: {
      max: 2,
      min: 0,
      step: 0.1,
    },
    fieldName: 'temperature',
    label: 'temperature',
  },
  timeout: {
    component: 'Slider',
    componentProps: {
      max: 600,
      min: 1,
      step: 1,
    },
    fieldName: 'timeout',
    label: 'timeout（秒）',
  },
  topP: {
    component: 'Slider',
    componentProps: {
      max: 1,
      min: 0,
      step: 0.1,
    },
    fieldName: 'topP',
    label: 'topP',
  },
};

const providerOptions = computed(() => getModelProviderOptions());
const currentProvider = ref<string>('OPENAI');
const currentType = ref<string>('TEXT2TEXT');
const hiddenTypeSet = new Set(['SPEECH2TEXT', 'TEXT2SPEECH', 'TEXT2VIDEO']);

function getAvailableTypes(provider?: string): ModelTypeKey[] {
  const supportedTypes = getProviderSupportedTypes(provider);
  const availableTypes = supportedTypes.filter(
    (type) => !hiddenTypeSet.has(type),
  );
  if (availableTypes.length > 0) {
    return availableTypes;
  }
  return ['TEXT2TEXT'];
}

const typeOptions = computed(() =>
  getProviderTypeOptions(currentProvider.value).filter(
    (option) => !hiddenTypeSet.has(String(option.value)),
  ),
);

const configFieldNames = computed(() =>
  getModelTypeConfigFields(currentType.value).filter(
    (field): field is ConfigFieldName => configFieldNameSet.has(field),
  ),
);

const connectionFieldSchema = computed<VbenFormSchema[]>(() => {
  const activeFieldNames = new Set(configFieldNames.value);
  return CONNECTION_FIELD_NAMES.filter((fieldName) =>
    activeFieldNames.has(fieldName),
  ).map((fieldName) => configFieldSchemaMap[fieldName]);
});

const tailConfigFieldSchema = computed<VbenFormSchema[]>(() => {
  const connectionFieldSet = new Set<string>(CONNECTION_FIELD_NAMES);
  return configFieldNames.value
    .filter((fieldName) => !connectionFieldSet.has(fieldName))
    .map((fieldName) => configFieldSchemaMap[fieldName]);
});

const presetModelOptions = computed(() =>
  getProviderRecommendedModels(currentProvider.value, currentType.value).map(
    (item) => ({
      label: item,
      value: item,
    }),
  ),
);

function resolveTypeForProvider(provider: string, type?: string) {
  const supportedTypes = getAvailableTypes(provider);
  const normalizedType = normalizeModelType(type);
  return supportedTypes.includes(normalizedType)
    ? normalizedType
    : supportedTypes[0] || normalizedType;
}

const isSyncingProviderType = ref(false);
const isPrefilling = ref(false);

async function handleFormValuesChange(values: Record<string, any>) {
  if (isSyncingProviderType.value || isPrefilling.value) {
    return;
  }

  const provider = String(values.provider || '');
  const selectedType = normalizeModelType(values.type || currentType.value);
  const resolvedType = resolveTypeForProvider(provider, selectedType);
  const providerChanged = provider !== currentProvider.value;
  const typeChanged = resolvedType !== currentType.value;

  currentProvider.value = provider;
  currentType.value = resolvedType;

  // 供应商变化时，Base URL 自动填充该供应商的默认地址（仍可手动修改）
  if (providerChanged) {
    const defaultBaseUrl = getProviderBaseUrl(provider);
    if (defaultBaseUrl && defaultBaseUrl !== values.baseUrl) {
      isSyncingProviderType.value = true;
      try {
        await formApi.setValues({ baseUrl: defaultBaseUrl }, false);
      } finally {
        isSyncingProviderType.value = false;
      }
    }
  }

  if (!providerChanged && !typeChanged) {
    return;
  }

  if (resolvedType === values.type) {
    return;
  }

  isSyncingProviderType.value = true;
  try {
    await formApi.setValues({ type: resolvedType }, false);
  } finally {
    isSyncingProviderType.value = false;
  }
}

const formSchema = computed<VbenFormSchema[]>(() => [
  {
    component: 'Input',
    fieldName: 'name',
    label: '模型别名',
    rules: 'required',
  },
  {
    component: 'Select',
    componentProps: {
      options: providerOptions.value,
    },
    fieldName: 'provider',
    label: '供应商',
    rules: 'selectRequired',
  },
  {
    component: 'Select',
    componentProps: {
      options: typeOptions.value,
    },
    fieldName: 'type',
    label: '模型类型',
    rules: 'selectRequired',
  },
  {
    component: 'Input',
    componentProps: {
      placeholder: presetModelOptions.value.length
        ? `例如：${presetModelOptions.value
            .slice(0, 3)
            .map((item) => item.label)
            .join(' / ')}`
        : '输入模型名称',
    },
    fieldName: 'model',
    label: '模型名称',
    rules: 'required',
  },
  ...connectionFieldSchema.value,
  ...tailConfigFieldSchema.value,
]);

const [Form, formApi] = useVbenForm({
  handleValuesChange: handleFormValuesChange,
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
  const payload = { ...(values as Record<string, any>) };
  const activeConfigFieldSet = new Set(configFieldNames.value);
  for (const fieldName of ALL_CONFIG_FIELD_NAMES) {
    if (!activeConfigFieldSet.has(fieldName)) {
      delete payload[fieldName];
    }
  }
  emit('save', payload as Partial<AigcModel>);
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

    const incoming = props.modelValue ?? {};
    const incomingProvider = String(incoming.provider || 'OPENAI');
    const incomingType = resolveTypeForProvider(
      incomingProvider,
      String(incoming.type || 'TEXT2TEXT'),
    );
    currentProvider.value = incomingProvider;
    currentType.value = incomingType;

    // 赋值期间屏蔽供应商/类型联动，避免 resetForm 触发的空值事件把预填内容冲掉
    isPrefilling.value = true;
    try {
      await nextTick();
      await formApi.resetForm();
      await formApi.setValues(
        {
          apiKey: '',
          billingType: 'TOKEN',
          dimension: 1536,
          maxToken: 4096,
          model: '',
          name: '',
          temperature: 0.7,
          timeout: 60,
          topP: 1,
          ...incoming,
          // Base URL 默认填充当前供应商的内置地址；编辑时若已有值则保留
          baseUrl:
            String(incoming.baseUrl || '') ||
            getProviderBaseUrl(incomingProvider),
          provider: incomingProvider,
          type: incomingType,
        },
        false,
      );
      await nextTick();
      // 兜底：schema 联动刷新后重新确保供应商与类型处于选中状态
      await formApi.setValues(
        {
          provider: incomingProvider,
          type: incomingType,
        },
        false,
      );
    } finally {
      isPrefilling.value = false;
    }
  },
  { immediate: true },
);
</script>

<template>
  <Drawer>
    <Form />
  </Drawer>
</template>
