<script setup lang="ts">
import type { AigcDatasource, TestConnectionPayload } from '#/api/aigc/datasource';

import { computed, ref, watch } from 'vue';
import { useVbenDrawer } from '@vben/common-ui';
import { Check, PlugZap } from '@vben/icons';

import { NButton } from 'naive-ui';

import { message } from '#/adapter/naive';
import { useVbenForm, type VbenFormSchema } from '#/adapter/form';
import {
  createDataSource,
  testDataSource,
  updateDataSource,
} from '#/api/aigc/datasource';

import {
  DATASOURCE_TYPE_OPTIONS,
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
  props.modelValue?.id ? '编辑数据源' : '新建数据源',
);
const selectedTypeMeta = computed(() =>
  getDatasourceTypeMeta(selectedDbType.value),
);

const formSchema = computed<VbenFormSchema[]>(() => [
  {
    component: 'Input',
    fieldName: 'name',
    formItemClass: 'sm:col-span-2',
    label: '数据源名称',
    rules: 'required',
  },
  {
    component: 'Input',
    componentProps: {
      placeholder: '例如 127.0.0.1',
    },
    fieldName: 'host',
    label: '主机地址',
    rules: 'required',
  },
  {
    component: 'InputNumber',
    componentProps: {
      min: 1,
    },
    fieldName: 'port',
    label: '端口',
    rules: 'required',
  },
  {
    component: 'Input',
    fieldName: 'databaseName',
    label: '数据库名',
    rules: 'required',
  },
  {
    component: 'Input',
    fieldName: 'username',
    label: '用户名',
    rules: 'required',
  },
  {
    component: 'Input',
    componentProps: {
      showPasswordOn: 'click',
      type: 'password',
    },
    fieldName: 'password',
    label: '密码',
    rules: 'required',
  },
  {
    component: 'Input',
    componentProps: {
      autosize: { maxRows: 3, minRows: 2 },
      type: 'textarea',
    },
    fieldName: 'remark',
    label: '备注',
    formItemClass: 'sm:col-span-2',
  },
  {
    component: 'Switch',
    fieldName: 'enabled',
    label: '是否启用',
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
    message.success('数据源已保存');
    emit('save', payload as Partial<AigcDatasource>);
  } catch (error) {
    message.error(`保存失败：${(error as Error)?.message || '未知错误'}`);
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
    message.warning('请先填写连接信息');
    return;
  }
  testing.value = true;
  try {
    await testDataSource(payload);
    message.success('连接成功');
  } catch (error) {
    message.error(`连接失败：${(error as Error)?.message || '未知错误'}`);
  } finally {
    testing.value = false;
  }
}

const [Drawer, drawerApi] = useVbenDrawer({
  class: 'w-[920px]',
  confirmText: '保存',
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
          数据库厂商
        </div>
        <div class="grid grid-cols-2 gap-2 md:grid-cols-1">
          <button
            v-for="option in DATASOURCE_TYPE_OPTIONS"
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
                默认端口 {{ option.port }}
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
            <div class="text-sm font-semibold text-foreground">连接配置</div>
            <div class="mt-0.5 text-[11px] text-muted-foreground">
              {{ selectedTypeMeta.label }} · 默认端口 {{ selectedTypeMeta.port }}
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
          测试连接
        </NButton>
        <div class="flex items-center gap-2">
          <NButton :disabled="saving" quaternary @click="drawerApi.onCancel">
            取消
          </NButton>
          <NButton
            :loading="saving"
            type="primary"
            @click="drawerApi.onConfirm"
          >
            保存
          </NButton>
        </div>
      </div>
    </template>
  </Drawer>
</template>
