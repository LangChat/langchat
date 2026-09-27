<script setup lang="ts">
import type { AigcDatasource, TestConnectionPayload } from '#/api/aigc/datasource';

import { computed, ref, watch } from 'vue';
import { useVbenDrawer } from '@vben/common-ui';
import { Check, PlugZap } from '@vben/icons';
import { $t } from '@vben/locales';

import { NButton } from 'naive-ui';

import { message } from '#/adapter/naive';
import { useVbenForm, type VbenFormSchema } from '#/adapter/form';
import {
  createDataSource,
  testDataSource,
  updateDataSource,
} from '#/api/aigc/datasource';

import {
  datasourceTypeOptions,
  getDatasourceTypeMeta,
} from './datasource-meta';

interface Props {
  modelValue?: Partial<AigcDatasource> | null;
  saving?: boolean;
  show: boolean;
}

const props = withDefaults(defineProps<Props>(), {
  modelValue: null,
  saving: false,
});

const emit = defineEmits<{
  cancel: [];
  save: [payload: Partial<AigcDatasource>];
  'update:show': [value: boolean];
}>();

const testing = ref(false);
const selectedDbType = ref('MYSQL');

const drawerTitle = computed(() =>
  props.modelValue?.id
    ? $t('datasource.edit.editTitle')
    : $t('datasource.edit.createTitle'),
);
const selectedTypeMeta = computed(() =>
  getDatasourceTypeMeta(selectedDbType.value),
);

const formSchema = computed<VbenFormSchema[]>(() => [
  {
    component: 'Input',
    fieldName: 'name',
    formItemClass: 'sm:col-span-2',
    label: $t('datasource.edit.name'),
    rules: 'required',
  },
  {
    component: 'Input',
    componentProps: {
      placeholder: $t('datasource.edit.hostPlaceholder'),
    },
    fieldName: 'host',
    label: $t('datasource.edit.host'),
    rules: 'required',
  },
  {
    component: 'InputNumber',
    componentProps: {
      min: 1,
    },
    fieldName: 'port',
    label: $t('common.labels.port'),
    rules: 'required',
  },
  {
    component: 'Input',
    fieldName: 'databaseName',
    label: $t('datasource.edit.database'),
    rules: 'required',
  },
  {
    component: 'Input',
    fieldName: 'username',
    label: $t('common.labels.username'),
    rules: 'required',
  },
  {
    component: 'Input',
    componentProps: {
      showPasswordOn: 'click',
      type: 'password',
    },
    fieldName: 'password',
    label: $t('common.labels.password'),
    rules: 'required',
  },
  {
    component: 'Input',
    componentProps: {
      autosize: { maxRows: 3, minRows: 2 },
      type: 'textarea',
    },
    fieldName: 'remark',
    label: $t('datasource.edit.remark'),
    formItemClass: 'sm:col-span-2',
  },
  {
    component: 'Switch',
    fieldName: 'enabled',
    label: $t('datasource.edit.enabled'),
  },
]);

const [Form, formApi] = useVbenForm({
  layout: 'vertical',
  schema: formSchema.value,
  showDefaultActions: false,
  wrapperClass: 'grid-cols-1 gap-x-4 p-0 sm:grid-cols-2',
});

watch(
  () => formSchema.value,
  () => {
    formApi.setState({ schema: formSchema.value });
  },
  { immediate: true },
);

function emitClose(withCancel: boolean = true) {
  emit('update:show', false);
  if (withCancel) {
    emit('cancel');
  }
}

async function handleSelectType(dbType: string) {
  const previousMeta = getDatasourceTypeMeta(selectedDbType.value);
  const nextMeta = getDatasourceTypeMeta(dbType);
  const values = (await formApi.getValues()) as Partial<AigcDatasource>;
  selectedDbType.value = nextMeta.value;
  if (!values.port || values.port === previousMeta.port) {
    await formApi.setFieldValue('port', nextMeta.port);
  }
}

async function handleSave() {
  const values = await formApi.validateAndSubmitForm();
  if (!values) {
    return;
  }
  const payload = {
    ...(values as Record<string, any>),
    dbType: selectedDbType.value,
  };
  try {
    if (props.modelValue?.id) {
      await updateDataSource(
        props.modelValue.id,
        payload as Partial<AigcDatasource>,
      );
    } else {
      await createDataSource(payload as Partial<AigcDatasource>);
    }
    message.success($t('datasource.messages.saved'));
    emit('save', payload as Partial<AigcDatasource>);
  } catch (error) {
    message.error(
      $t('common.messages.saveFailed', {
        message: (error as Error)?.message || $t('errors.unknown'),
      }),
    );
  }
}

async function handleTest() {
  const values = (await formApi.getValues()) as Partial<AigcDatasource>;
  const payload: TestConnectionPayload = {
    databaseName: String(values.databaseName || ''),
    dbType: selectedDbType.value,
    host: String(values.host || ''),
    password: String(values.password || ''),
    port: values.port,
    username: String(values.username || ''),
  };
  if (!payload.host || !payload.databaseName) {
    message.warning($t('datasource.edit.connectionInfoRequired'));
    return;
  }
  testing.value = true;
  try {
    await testDataSource(payload);
    message.success($t('datasource.messages.connectionSuccess'));
  } catch (error) {
    message.error(
      $t('datasource.messages.connectionFailed', {
        message: (error as Error)?.message || $t('errors.unknown'),
      }),
    );
  } finally {
    testing.value = false;
  }
}

const [Drawer, drawerApi] = useVbenDrawer({
  class: 'w-[920px]',
  contentClass: 'p-0',
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
  () => $t('common.actions.save'),
  (value) => {
    drawerApi.setState({ confirmText: value });
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
    const typeMeta = getDatasourceTypeMeta(incoming.dbType);
    selectedDbType.value = typeMeta.value;
    await formApi.setValues(
      {
        databaseName: incoming.databaseName,
        enabled: incoming.enabled ?? true,
        host: incoming.host,
        name: incoming.name,
        password: incoming.password,
        port: incoming.port ?? typeMeta.port,
        remark: incoming.remark,
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
    <div class="grid min-h-full md:grid-cols-[250px_minmax(0,1fr)]">
      <aside class="border-b border-border bg-muted/20 p-4 md:border-b-0 md:border-r">
        <div class="mb-3 text-xs font-semibold text-foreground">
          {{ $t('datasource.edit.vendorTitle') }}
        </div>
        <div class="grid grid-cols-2 gap-2 md:grid-cols-1">
          <button
            v-for="option in datasourceTypeOptions()"
            :key="option.value"
            :aria-pressed="selectedDbType === option.value"
            :class="[
              selectedDbType === option.value
                ? 'border-primary bg-primary/5 shadow-sm'
                : 'border-border bg-background hover:border-primary/40 hover:bg-muted/40',
            ]"
            class="group relative flex min-h-[74px] items-center gap-3 rounded-lg border p-3 text-left transition-colors"
            type="button"
            @click="handleSelectType(option.value)"
          >
            <span
              class="flex size-10 shrink-0 items-center justify-center rounded-lg border border-border/70 bg-white p-1.5"
            >
              <img
                :alt="`${option.label} logo`"
                class="size-full object-contain"
                :src="option.icon"
              />
            </span>
            <span class="min-w-0 flex-1">
              <span class="block text-xs font-semibold text-foreground">
                {{ option.label }}
              </span>
              <span class="mt-0.5 block truncate text-[10px] text-muted-foreground">
                {{ $t('datasource.edit.defaultPort', { port: option.port }) }}
              </span>
            </span>
            <span
              v-if="selectedDbType === option.value"
              class="flex size-5 shrink-0 items-center justify-center rounded-full bg-primary text-primary-foreground"
            >
              <Check class="size-3" />
            </span>
          </button>
        </div>
      </aside>

      <section class="min-w-0 p-5">
        <div class="mb-5 flex items-center gap-3 border-b border-border pb-4">
          <span
            class="flex size-10 items-center justify-center rounded-lg border border-border bg-white p-1.5"
          >
            <img
              :alt="`${selectedTypeMeta.label} logo`"
              class="size-full object-contain"
              :src="selectedTypeMeta.icon"
            />
          </span>
          <div class="min-w-0">
            <div class="text-sm font-semibold text-foreground">{{ $t('datasource.edit.connectionTitle') }}</div>
            <div class="mt-0.5 text-[11px] text-muted-foreground">
              {{ $t('datasource.edit.connectionSummary', { type: selectedTypeMeta.label, port: selectedTypeMeta.port }) }}
            </div>
          </div>
        </div>
        <Form />
      </section>
    </div>
    <template #footer>
      <div class="flex w-full items-center justify-between gap-3">
        <NButton :loading="testing" secondary @click="handleTest">
          <PlugZap class="mr-1.5 size-4" />
          {{ $t('datasource.edit.testConnection') }}
        </NButton>
        <div class="flex items-center gap-2">
          <NButton :disabled="saving" quaternary @click="drawerApi.onCancel">
            {{ $t('common.actions.cancel') }}
          </NButton>
          <NButton
            :loading="saving"
            type="primary"
            @click="drawerApi.onConfirm"
          >
            {{ $t('common.actions.save') }}
          </NButton>
        </div>
      </div>
    </template>
  </Drawer>
</template>
