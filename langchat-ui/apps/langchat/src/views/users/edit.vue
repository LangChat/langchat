<script setup lang="ts">
import type { AigcUser } from '#/api/auth/user';

import { computed, watch } from 'vue';
import { useVbenDrawer } from '@vben/common-ui';

import { useVbenForm, type VbenFormSchema } from '#/adapter/form';
import {
  USER_SEX_OPTIONS,
  USER_STATUS_OPTIONS,
} from '#/views/shared/auth/options';

interface UserFormModel extends Partial<AigcUser> {
  roleIds?: string[];
}

interface Props {
  modelValue?: UserFormModel | null;
  roleOptions: Array<{ label: string; value: string }>;
  saving?: boolean;
  show: boolean;
}

const props = withDefaults(defineProps<Props>(), {
  modelValue: null,
  saving: false,
});

const emit = defineEmits<{
  cancel: [];
  save: [payload: UserFormModel];
  'update:show': [value: boolean];
}>();

const drawerTitle = computed(() =>
  props.modelValue?.id ? '编辑用户' : '新建用户',
);

const formSchema = computed<VbenFormSchema[]>(() => [
  {
    component: 'Input',
    fieldName: 'username',
    label: '用户名',
    rules: 'required',
  },
  {
    component: 'Input',
    fieldName: 'realName',
    label: '姓名',
    rules: 'required',
  },
  {
    component: 'Input',
    componentProps: {
      placeholder: '编辑时留空则保持原密码',
      type: 'password',
    },
    fieldName: 'password',
    label: '登录密码',
  },
  {
    component: 'Select',
    componentProps: {
      options: USER_STATUS_OPTIONS,
    },
    fieldName: 'status',
    label: '用户状态',
    rules: 'selectRequired',
  },
  {
    component: 'Select',
    componentProps: {
      options: USER_SEX_OPTIONS,
    },
    fieldName: 'sex',
    label: '性别',
  },
  {
    component: 'Input',
    fieldName: 'phone',
    label: '手机号',
  },
  {
    component: 'Input',
    fieldName: 'email',
    label: '邮箱',
  },
  {
    component: 'Input',
    fieldName: 'deptId',
    label: '部门 ID',
  },
  {
    component: 'Input',
    fieldName: 'avatar',
    formItemClass: 'sm:cols-span-2',
    label: '头像地址',
  },
  {
    component: 'Select',
    componentProps: {
      clearable: true,
      filterable: true,
      maxTagCount: 4,
      multiple: true,
      options: props.roleOptions,
      placeholder: '请选择角色',
    },
    fieldName: 'roleIds',
    formItemClass: 'sm:cols-span-2',
    label: '角色分配',
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
  emit('save', values as UserFormModel);
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
        roleIds: [],
        sex: 'UNKNOWN',
        status: 1,
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
