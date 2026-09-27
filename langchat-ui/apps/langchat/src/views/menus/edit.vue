<script setup lang="ts">
import type { AigcMenu, AigcMenuTreeNode } from '#/api/auth/menu';

import { computed, watch } from 'vue';

import { useVbenDrawer } from '@vben/common-ui';
import { $t } from '@vben/locales';

import { useVbenForm, type VbenFormSchema } from '#/adapter/form';
import { menuTypeOptions } from '#/views/shared/auth/options';

interface Props {
  menuTree: AigcMenuTreeNode[];
  modelValue?: null | Partial<AigcMenu>;
  saving?: boolean;
  show: boolean;
}

const props = withDefaults(defineProps<Props>(), {
  modelValue: null,
  saving: false,
});

const emit = defineEmits<{
  cancel: [];
  save: [payload: Partial<AigcMenu>];
  'update:show': [value: boolean];
}>();

const drawerTitle = computed(() =>
  props.modelValue?.id ? $t('menus.title.edit') : $t('menus.title.create'),
);

const formSchema = computed<VbenFormSchema[]>(() => [
  {
    component: 'Input',
    fieldName: 'name',
    label: $t('menus.form.name'),
    rules: 'required',
  },
  {
    component: 'Select',
    componentProps: {
      options: menuTypeOptions(),
    },
    fieldName: 'type',
    label: $t('menus.form.type'),
    rules: 'selectRequired',
  },
  {
    component: 'TreeSelect',
    componentProps: {
      childrenField: 'children',
      clearable: true,
      filterable: true,
      keyField: 'id',
      labelField: 'name',
      options: props.menuTree,
      placeholder: $t('menus.form.parentPlaceholder'),
    },
    fieldName: 'parentId',
    label: $t('menus.form.parent'),
  },
  {
    component: 'InputNumber',
    componentProps: {
      min: 0,
    },
    fieldName: 'orderNo',
    label: $t('menus.form.orderNo'),
  },
  {
    component: 'Input',
    fieldName: 'path',
    label: $t('menus.form.path'),
  },
  {
    component: 'Input',
    fieldName: 'perms',
    label: $t('menus.form.perms'),
  },
  {
    component: 'Input',
    fieldName: 'icon',
    label: $t('menus.form.icon'),
  },
  {
    component: 'Input',
    fieldName: 'component',
    label: $t('menus.form.component'),
  },
  {
    component: 'Switch',
    fieldName: 'isShow',
    label: $t('menus.form.isShow'),
  },
  {
    component: 'Switch',
    fieldName: 'isKeepalive',
    label: $t('menus.form.isKeepalive'),
  },
  {
    component: 'Switch',
    fieldName: 'isExt',
    label: $t('menus.form.isExternal'),
  },
  {
    component: 'Switch',
    fieldName: 'isDisabled',
    label: $t('menus.form.isDisabled'),
  },
]);

const [Form, formApi] = useVbenForm({
  layout: 'vertical',
  schema: formSchema.value,
  showDefaultActions: false,
  wrapperClass: 'grid-cols-1 px-3',
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
  emit('save', values as Partial<AigcMenu>);
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
    await formApi.setValues(
      {
        isDisabled: false,
        isExt: false,
        isKeepalive: true,
        isShow: true,
        orderNo: 1,
        type: 'MENU',
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
