<script setup lang="ts">
import type { AigcMenu, AigcMenuTreeNode } from '#/api/auth/menu';

import { computed, watch } from 'vue';
import { useVbenDrawer } from '@vben/common-ui';

import { useVbenForm, type VbenFormSchema } from '#/adapter/form';
import { MENU_TYPE_OPTIONS } from '#/views/shared/auth/options';

interface Props {
  menuTree: AigcMenuTreeNode[];
  modelValue?: Partial<AigcMenu> | null;
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
  props.modelValue?.id ? '编辑菜单' : '新建菜单',
);

const formSchema = computed<VbenFormSchema[]>(() => [
  {
    component: 'Input',
    fieldName: 'name',
    label: '菜单名称',
    rules: 'required',
  },
  {
    component: 'Select',
    componentProps: {
      options: MENU_TYPE_OPTIONS,
    },
    fieldName: 'type',
    label: '菜单类型',
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
      placeholder: '请选择上级菜单',
    },
    fieldName: 'parentId',
    label: '上级菜单',
  },
  {
    component: 'InputNumber',
    componentProps: {
      min: 0,
    },
    fieldName: 'orderNo',
    label: '排序号',
  },
  {
    component: 'Input',
    fieldName: 'path',
    label: '路由路径',
  },
  {
    component: 'Input',
    fieldName: 'perms',
    label: '权限标识',
  },
  {
    component: 'Input',
    fieldName: 'icon',
    label: '图标',
  },
  {
    component: 'Input',
    fieldName: 'component',
    label: '组件路径',
  },
  {
    component: 'Switch',
    fieldName: 'isShow',
    label: '是否显示',
  },
  {
    component: 'Switch',
    fieldName: 'isKeepalive',
    label: '是否缓存',
  },
  {
    component: 'Switch',
    fieldName: 'isExt',
    label: '是否外链',
  },
  {
    component: 'Switch',
    fieldName: 'isDisabled',
    label: '是否禁用',
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
        isDisabled: false,
        isExt: false,
        isKeepalive: true,
        isShow: true,
        orderNo: 1,
        type: 'MENU',
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
