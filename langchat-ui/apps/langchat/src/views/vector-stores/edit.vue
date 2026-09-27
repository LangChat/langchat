<script setup lang="ts">
import type { AigcVectorStore } from '#/api/aigc/vector-store';

import { computed, ref, watch } from 'vue';

import { useVbenDrawer } from '@vben/common-ui';
import { Check } from '@vben/icons';
import { $t } from '@vben/locales';

import { useVbenForm, type VbenFormSchema } from '#/adapter/form';
import { vectorDimensionOptions } from '#/views/shared/aigc/options';

import {
  getVectorStoreProviderMeta,
  vectorStoreProviderOptions,
} from './vector-store-meta';

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

const selectedProvider = ref('PGVECTOR');
const providerMeta = computed(() =>
  getVectorStoreProviderMeta(selectedProvider.value),
);

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
    componentProps: {
      placeholder: providerMeta.value.tablePlaceholder,
    },
    fieldName: 'tableName',
    label: providerMeta.value.tableLabel,
    rules: providerMeta.value.requireTableName ? 'required' : undefined,
  },
  {
    component: 'Select',
    componentProps: {
      options: vectorDimensionOptions(),
    },
    fieldName: 'dimension',
    label: $t('vectorStores.form.dimension'),
    rules: 'selectRequired',
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

async function handleSelectProvider(provider: string) {
  const previousMeta = getVectorStoreProviderMeta(selectedProvider.value);
  const nextMeta = getVectorStoreProviderMeta(provider);
  const values = (await formApi.getValues()) as Partial<AigcVectorStore>;
  selectedProvider.value = nextMeta.value;
  if (!values.port || values.port === previousMeta.defaultPort) {
    await formApi.setFieldValue('port', nextMeta.defaultPort);
  }
}

async function handleSave() {
  const values = await formApi.validateAndSubmitForm();
  if (!values) {
    return;
  }
  emit('save', {
    ...(values as Record<string, any>),
    provider: selectedProvider.value,
  } as Partial<AigcVectorStore>);
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
    const incoming = props.modelValue ?? {};
    const meta = getVectorStoreProviderMeta(incoming.provider);
    selectedProvider.value = meta.value;
    await formApi.setValues(
      {
        databaseName: incoming.databaseName,
        dimension: incoming.dimension ?? 1024,
        host: incoming.host,
        name: incoming.name,
        password: incoming.password,
        port: incoming.port ?? meta.defaultPort,
        tableName: incoming.tableName,
        username: incoming.username,
      },
      false,
    );
  },
  { immediate: true },
);
</script>

<template>
  <Drawer>
    <div class="px-3 pb-2 pt-1">
      <div class="mb-2 text-xs font-medium text-foreground">
        {{ $t('vectorStores.form.providerTitle') }}
      </div>
      <div class="grid grid-cols-2 gap-3">
        <button
          v-for="option in vectorStoreProviderOptions()"
          :key="option.value"
          :aria-pressed="selectedProvider === option.value"
          :class="[
            selectedProvider === option.value
              ? 'border-primary bg-primary/5 shadow-sm'
              : 'border-border bg-background hover:border-primary/40 hover:bg-muted/40',
          ]"
          class="group relative flex min-h-[96px] items-start gap-3 rounded-xl border p-3.5 text-left transition-colors"
          type="button"
          @click="handleSelectProvider(option.value)"
        >
          <span
            class="flex size-11 shrink-0 items-center justify-center rounded-lg border border-border/70 bg-white p-1.5"
          >
            <img
              :alt="`${option.label} logo`"
              class="size-full object-contain"
              :src="option.icon"
            />
          </span>
          <span class="min-w-0 flex-1">
            <span class="block text-sm font-semibold text-foreground">
              {{ option.label }}
            </span>
            <span
              class="mt-1 block text-[11px] leading-4 text-muted-foreground"
            >
              {{ option.description }}
            </span>
            <span
              class="mt-1.5 block text-[10px] tabular-nums text-muted-foreground/80"
            >
              {{ $t('vectorStores.meta.defaultPort', { port: option.defaultPort }) }}
            </span>
          </span>
          <span
            v-if="selectedProvider === option.value"
            class="absolute right-3 top-3 flex size-5 items-center justify-center rounded-full bg-primary text-primary-foreground"
          >
            <Check class="size-3" />
          </span>
        </button>
      </div>
    </div>
    <Form />
  </Drawer>
</template>
