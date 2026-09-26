<script setup lang="ts">
import type { AigcMenuTreeNode } from '#/api/auth/menu';
import type { AigcRole } from '#/api/auth/role';
import type { TreeOption } from 'naive-ui';

import { computed, ref, watch } from 'vue';
import { useVbenDrawer } from '@vben/common-ui';
import { NButton, NInput, NTree } from 'naive-ui';

import { useVbenForm, type VbenFormSchema } from '#/adapter/form';

interface RoleFormModel extends Partial<AigcRole> {
  menuIds?: string[];
}

interface Props {
  menuTree: AigcMenuTreeNode[];
  modelValue?: RoleFormModel | null;
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
  props.modelValue?.id ? '编辑角色' : '新建角色',
);

const checkedMenuIds = computed(() => roleModel.value.menuIds ?? []);
const treeData = computed(() => props.menuTree as unknown as TreeOption[]);

const formSchema: VbenFormSchema[] = [
  {
    component: 'Input',
    fieldName: 'name',
    label: '角色名称',
    rules: 'required',
  },
  {
    component: 'Input',
    fieldName: 'code',
    label: '角色编码',
    rules: 'required',
  },
  {
    component: 'Input',
    componentProps: {
      autosize: { maxRows: 6, minRows: 3 },
      placeholder: '请输入角色描述',
      type: 'textarea',
    },
    fieldName: 'description',
    formItemClass: 'sm:cols-span-2',
    label: '角色描述',
  },
];

const [Form, formApi] = useVbenForm({
  layout: 'vertical',
  schema: formSchema,
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
  roleModel.value.menuIds = keys.map((item) => String(item));
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

    menuKeyword.value = '';
    roleModel.value = {
      menuIds: [...new Set(props.modelValue?.menuIds ?? [])],
      ...(props.modelValue ?? {}),
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
        <div class="mb-2 text-sm font-medium text-foreground">菜单授权</div>
        <div class="space-y-3">
          <div
            class="flex flex-col gap-3 md:flex-row md:items-center md:justify-between"
          >
            <div class="w-full md:max-w-sm">
              <NInput
                v-model:value="menuKeyword"
                clearable
                placeholder="搜索菜单名称"
              />
            </div>
            <div class="flex items-center gap-2">
              <NButton secondary size="small" @click="toggleAllMenus">
                全部授权
              </NButton>
              <NButton secondary size="small" @click="clearAllMenus">
                清空授权
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
