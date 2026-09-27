<script lang="ts" setup>
import type { AigcModel, AigcModelConfig } from '#/api/aigc/model';

import { computed, nextTick, ref, watch } from 'vue';
import { useVbenDrawer } from '@vben/common-ui';
import { $t } from '@vben/locales';

import { type VbenFormSchema, useVbenForm } from '#/adapter/form';
import { vectorDimensionOptions } from '#/views/shared/aigc/options';
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
  props.modelValue?.id
    ? $t('models.form.editTitle')
    : $t('models.form.createTitle'),
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
const DEFAULT_EMBEDDING_DIMENSION = 1024;

function positiveInteger(value: unknown): number | undefined {
  const numberValue = Number(value);
  return Number.isInteger(numberValue) && numberValue > 0
    ? numberValue
    : undefined;
}

function normalizeConfig(config: unknown): AigcModelConfig {
  if (!config || typeof config !== 'object' || Array.isArray(config)) {
    return {};
  }
  return { ...(config as AigcModelConfig) };
}

function getConfigFieldSchemaMap(): Record<ConfigFieldName, VbenFormSchema> {
  return {
    apiKey: {
      component: 'VbenInputPassword',
      componentProps: {
        placeholder: $t('models.form.apiKeyPlaceholder'),
      },
      fieldName: 'apiKey',
      label: 'API Key',
    },
    baseUrl: {
      component: 'Input',
      componentProps: {
        placeholder: $t('models.form.baseUrlPlaceholder'),
      },
      fieldName: 'baseUrl',
      label: 'Base URL',
    },
    dimension: {
      component: 'Select',
      componentProps: {
        options: vectorDimensionOptions(),
        placeholder: $t('models.form.dimensionPlaceholder'),
      },
      fieldName: 'dimension',
      label: $t('models.form.dimension'),
      rules: 'selectRequired',
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
      label: $t('models.form.timeout'),
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
}

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
  const configFieldSchemaMap = getConfigFieldSchemaMap();
  const activeFieldNames = new Set(configFieldNames.value);
  return CONNECTION_FIELD_NAMES.filter((fieldName) =>
    activeFieldNames.has(fieldName),
  ).map((fieldName) => configFieldSchemaMap[fieldName]);
});

const tailConfigFieldSchema = computed<VbenFormSchema[]>(() => {
  const configFieldSchemaMap = getConfigFieldSchemaMap();
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

  const nextValues: Record<string, any> = {};

  // 供应商变化时，Base URL 自动填充该供应商的默认地址（仍可手动修改）
  if (providerChanged) {
    const defaultBaseUrl = getProviderBaseUrl(provider);
    if (defaultBaseUrl && defaultBaseUrl !== values.baseUrl) {
      nextValues.baseUrl = defaultBaseUrl;
    }
  }

  if (resolvedType !== values.type) {
    nextValues.type = resolvedType;
  }

  if (providerChanged || typeChanged) {
    const selectedModel = String(values.model || '');
    const availableModels = getProviderRecommendedModels(
      provider,
      resolvedType,
    );
    if (!selectedModel || !availableModels.includes(selectedModel)) {
      nextValues.model = availableModels[0] || '';
    }
    if (resolvedType === 'EMBEDDINGS') {
      nextValues.dimension = DEFAULT_EMBEDDING_DIMENSION;
    }
  }

  if (Object.keys(nextValues).length === 0) {
    return;
  }

  isSyncingProviderType.value = true;
  try {
    await formApi.setValues(nextValues, false);
  } finally {
    isSyncingProviderType.value = false;
  }
}

const formSchema = computed<VbenFormSchema[]>(() => [
  {
    component: 'Input',
    fieldName: 'name',
    label: $t('models.form.alias'),
    rules: 'required',
  },
  {
    component: 'Select',
    componentProps: {
      options: providerOptions.value,
    },
    fieldName: 'provider',
    label: $t('models.form.provider'),
    rules: 'selectRequired',
  },
  {
    component: 'Select',
    componentProps: {
      options: typeOptions.value,
    },
    fieldName: 'type',
    label: $t('models.form.type'),
    rules: 'selectRequired',
  },
  {
    component: 'Select',
    componentProps: {
      clearable: true,
      filterable: true,
      options: presetModelOptions.value,
      placeholder: $t('models.form.modelSelectPlaceholder'),
      tag: true,
    },
    fieldName: 'model',
    label: $t('models.form.name'),
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
  const configJson = normalizeConfig(props.modelValue?.configJson);
  delete configJson.dimension;
  if (currentType.value === 'EMBEDDINGS') {
    configJson.dimension = positiveInteger(payload.dimension);
  }
  delete payload.dimension;
  payload.configJson = configJson;
  emit('save', payload as Partial<AigcModel>);
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

    const incoming = props.modelValue ?? {};
    const incomingConfig = normalizeConfig(incoming.configJson);
    const incomingFields = { ...incoming };
    delete incomingFields.configJson;
    const incomingProvider = String(incoming.provider || 'OPENAI');
    const incomingType = resolveTypeForProvider(
      incomingProvider,
      String(incoming.type || 'TEXT2TEXT'),
    );
    const defaultModel =
      getProviderRecommendedModels(incomingProvider, incomingType)[0] || '';
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
          dimension:
            positiveInteger(incomingConfig.dimension) ??
            DEFAULT_EMBEDDING_DIMENSION,
          maxToken: 4096,
          name: '',
          temperature: 0.7,
          timeout: 60,
          topP: 1,
          ...incomingFields,
          // 新建时默认选中当前供应商和类型的首个预置模型；编辑时保留原值
          model: String(incoming.model || '') || defaultModel,
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
