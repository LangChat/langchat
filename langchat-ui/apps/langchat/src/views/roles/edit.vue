<script setup lang="ts">
import type { TreeOption } from 'naive-ui';

import type { AigcMenuTreeNode } from '#/api/auth/menu';
import type { AigcRole } from '#/api/auth/role';

import { computed, ref, watch } from 'vue';

import { useVbenDrawer } from '@vben/common-ui';
import { $t } from '@vben/locales';

import { NButton, NInput, NTree } from 'naive-ui';

import { useVbenForm, type VbenFormSchema } from '#/adapter/form';

interface RoleFormModel extends Partial<AigcRole> {
  menuIds?: string[];
}

interface Props {
  menuTree: AigcMenuTreeNode[];
  modelValue?: null | RoleFormModel;
  saving?: boolean;
  show: boolean;
}

const props = withDefaults(defineProps<Props>(), {
  modelValue: null,
  saving: false,
});

const emit = defineEmits<{
  cancel: [];
  save: [payload: RoleFormModel];
  'update:show': [value: boolean];
}>();

const menuKeyword = ref('');
const roleModel = ref<RoleFormModel>({ menuIds: [] });

const drawerTitle = computed(() =>
  props.modelValue?.id ? $t('roles.title.edit') : $t('roles.title.create'),
);

const checkedMenuIds = computed(() => roleModel.value.menuIds ?? []);
const treeData = computed(() => props.menuTree as unknown as TreeOption[]);

const formSchema = computed<VbenFormSchema[]>(() => [
  {
    component: 'Input',
    fieldName: 'name',
    label: $t('roles.form.name'),
    rules: 'required',
  },
  {
    component: 'Input',
    fieldName: 'code',
    label: $t('roles.form.code'),
    rules: 'required',
  },
  {
    component: 'Input',
    componentProps: {
      autosize: { maxRows: 6, minRows: 3 },
      placeholder: $t('roles.form.descriptionPlaceholder'),
      type: 'textarea',
    },
    fieldName: 'description',
    formItemClass: 'sm:cols-span-2',
    label: $t('roles.form.description'),
  },
]);

const [Form, formApi] = useVbenForm({
  layout: 'vertical',
  schema: formSchema.value,
  showDefaultActions: false,
  wrapperClass: 'grid-cols-1 px-3',
});

function emitClose(withCancel: boolean = true) {
  emit('update:show', false);
  if (withCancel) {
    emit('cancel');
  }
}

function toggleAllMenus() {
  roleModel.value.menuIds = collectMenuIds(props.menuTree);
}

function clearAllMenus() {
  roleModel.value.menuIds = [];
}

function handleCheckedKeysChange(keys: Array<number | string>) {
  roleModel.value.menuIds = keys.map(String);
}

function collectMenuIds(nodes: AigcMenuTreeNode[]): string[] {
  return nodes.flatMap((item) => [
    ...(item.id ? [item.id] : []),
    ...collectMenuIds(item.children ?? []),
  ]);
}

async function handleSave() {
  const values = await formApi.validateAndSubmitForm();
  if (!values) {
    return;
  }
  emit('save', {
    ...(values as RoleFormModel),
    menuIds: roleModel.value.menuIds,
  });
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

    menuKeyword.value = '';
    roleModel.value = {
      menuIds: [...new Set(props.modelValue?.menuIds)],
      ...props.modelValue,
    };
    drawerApi.open();

    await formApi.resetForm();
    await formApi.setValues(roleModel.value, false);
  },
  { immediate: true },
);
</script>

<template>
  <Drawer>
    <div class="space-y-4">
      <Form />

      <div>
        <div class="mb-2 text-sm font-medium text-foreground">
          {{ $t('roles.form.menuAuth') }}
        </div>
        <div class="space-y-3">
          <div
            class="flex flex-col gap-3 md:flex-row md:items-center md:justify-between"
          >
            <div class="w-full md:max-w-sm">
              <NInput
                v-model:value="menuKeyword"
                clearable
                :placeholder="$t('roles.form.menuSearchPlaceholder')"
              />
            </div>
            <div class="flex items-center gap-2">
              <NButton secondary size="small" @click="toggleAllMenus">
                {{ $t('roles.form.grantAll') }}
              </NButton>
              <NButton secondary size="small" @click="clearAllMenus">
                {{ $t('roles.form.clearAll') }}
              </NButton>
            </div>
          </div>

          <div class="rounded-xl border border-border bg-muted/30 p-2">
            <div class="max-h-[360px] overflow-y-auto pr-1">
              <NTree
                block-line
                cascade
                checkable
                children-field="children"
                :checked-keys="checkedMenuIds"
                default-expand-all
                key-field="id"
                label-field="name"
                :pattern="menuKeyword"
                :data="treeData"
                @update:checked-keys="handleCheckedKeysChange"
              />
            </div>
          </div>
        </div>
      </div>
    </div>
  </Drawer>
</template>
