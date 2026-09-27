<script setup lang="ts">
import type { AigcKnowledge } from '#/api/aigc/knowledge';
import type { LabelOption } from '#/views/shared/aigc/options';

import { computed, markRaw, watch } from 'vue';
import { useVbenDrawer } from '@vben/common-ui';
import { $t } from '@vben/locales';

import { useVbenForm, type VbenFormSchema } from '#/adapter/form';
import LcIcon from '#/components/LcIcon/index.vue';
import { aigcCommonTagOptions } from '#/views/shared/aigc/options';
import { parseTagList, stringifyTagList } from '#/views/shared/aigc/tags';

interface Props {
  modelOptions: LabelOption[];
  modelValue?: Partial<AigcKnowledge> | null;
  saving?: boolean;
  show: boolean;
  vectorStoreOptions: LabelOption[];
}

const props = withDefaults(defineProps<Props>(), {
  modelValue: null,
  saving: false,
});
const LcIconEditor = markRaw(LcIcon);

const emit = defineEmits<{
  cancel: [];
  save: [payload: Partial<AigcKnowledge>];
  'update:show': [value: boolean];
}>();

const drawerTitle = computed(() =>
  props.modelValue?.id
    ? $t('knowledge.title.edit')
    : $t('knowledge.title.create'),
);

const formSchema = computed<VbenFormSchema[]>(() => [
  {
    component: 'Input',
    fieldName: 'name',
    label: $t('knowledge.form.name'),
    rules: 'required',
  },
  {
    component: LcIconEditor,
    componentProps: {
      editable: true,
      fallbackIcon: 'lucide:database',
      size: 64,
    },
    fieldName: 'coverUrl',
    label: $t('knowledge.form.icon'),
    modelPropName: 'modelValue',
  },
  {
    component: 'Select',
    componentProps: {
      clearable: true,
      options: props.vectorStoreOptions,
    },
    fieldName: 'vectorStoreId',
    label: $t('knowledge.form.vectorStore'),
    rules: 'selectRequired',
  },
  {
    component: 'Select',
    componentProps: {
      clearable: true,
      options: props.modelOptions,
    },
    fieldName: 'vectorModelId',
    label: $t('knowledge.form.vectorModel'),
    rules: 'selectRequired',
  },
  {
    component: 'InputNumber',
    componentProps: {
      min: 1,
    },
    fieldName: 'maxResults',
    label: $t('knowledge.form.maxResults'),
  },
  {
    component: 'InputNumber',
    componentProps: {
      min: 0,
      step: 0.05,
    },
    fieldName: 'minScore',
    label: $t('knowledge.form.minScore'),
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
      placeholder: $t('knowledge.form.descriptionPlaceholder'),
      type: 'textarea',
    },
    fieldName: 'description',
    formItemClass: 'sm:col-span-2',
    label: $t('knowledge.form.description'),
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
    coverUrl: String(payload.coverUrl || '').trim(),
    tags: stringifyTagList(payload.tags),
  } as Partial<AigcKnowledge>);
}

const [Drawer, drawerApi] = useVbenDrawer({
  class: 'w-[860px]',
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
        coverUrl: '',
        maxResults: 5,
        minScore: 0.5,
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
