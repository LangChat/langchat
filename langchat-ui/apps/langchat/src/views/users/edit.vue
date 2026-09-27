<script setup lang="ts">
import type { AigcUser } from '#/api/auth/user';

import { computed, watch } from 'vue';

import { useVbenDrawer } from '@vben/common-ui';
import { $t } from '@vben/locales';

import { useVbenForm, type VbenFormSchema } from '#/adapter/form';
import {
  userSexOptions,
  userStatusOptions,
} from '#/views/shared/auth/options';

interface UserFormModel extends Partial<AigcUser> {
  roleIds?: string[];
}

interface Props {
  modelValue?: null | UserFormModel;
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
  props.modelValue?.id ? $t('users.title.edit') : $t('users.title.create'),
);

const formSchema = computed<VbenFormSchema[]>(() => [
  {
    component: 'Input',
    fieldName: 'username',
    label: $t('users.form.username'),
    rules: 'required',
  },
  {
    component: 'Input',
    fieldName: 'realName',
    label: $t('users.form.realName'),
    rules: 'required',
  },
  {
    component: 'Input',
    componentProps: {
      placeholder: $t('users.form.passwordPlaceholder'),
      type: 'password',
    },
    fieldName: 'password',
    label: $t('users.form.password'),
  },
  {
    component: 'Select',
    componentProps: {
      options: userStatusOptions(),
    },
    fieldName: 'status',
    label: $t('users.form.status'),
    rules: 'selectRequired',
  },
  {
    component: 'Select',
    componentProps: {
      options: userSexOptions(),
    },
    fieldName: 'sex',
    label: $t('users.form.gender'),
  },
  {
    component: 'Input',
    fieldName: 'phone',
    label: $t('users.form.phone'),
  },
  {
    component: 'Input',
    fieldName: 'email',
    label: $t('users.form.email'),
  },
  {
    component: 'Input',
    fieldName: 'deptId',
    label: $t('users.form.deptId'),
  },
  {
    component: 'Input',
    fieldName: 'avatar',
    formItemClass: 'sm:cols-span-2',
    label: $t('users.form.avatar'),
  },
  {
    component: 'Select',
    componentProps: {
      clearable: true,
      filterable: true,
      maxTagCount: 4,
      multiple: true,
      options: props.roleOptions,
      placeholder: $t('users.form.rolesPlaceholder'),
    },
    fieldName: 'roleIds',
    formItemClass: 'sm:cols-span-2',
    label: $t('users.form.roles'),
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
        roleIds: [],
        sex: 'UNKNOWN',
        status: 1,
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
