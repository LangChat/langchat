<script setup lang="ts">
import type { AigcMenu, AigcMenuTreeNode } from '#/api/auth/menu';

import { ref } from 'vue';
import { Page } from '@vben/common-ui';
import { SquarePen, Trash2 } from '@vben/icons';
import { NButton, NInput, NSelect, NTag } from 'naive-ui';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import { dialog, message } from '#/adapter/naive';
import { menuApi } from '#/api/auth/menu';
import {
  findAuthOptionLabel,
  MENU_TYPE_OPTIONS,
} from '#/views/shared/auth/options';
import { filterMenuTree } from '#/views/shared/auth/menu-tree';
import ManageCard from '#/views/shared/auth/manage-card.vue';
import MenuEdit from './edit.vue';

const saving = ref(false);
const showEdit = ref(false);
const currentItem = ref<Partial<AigcMenu> | null>(null);
const allMenus = ref<AigcMenuTreeNode[]>([]);

// 搜索条件：draft 为输入框草稿值，applied 为已生效值（工具栏刷新时沿用生效值）
const draftKeyword = ref('');
const draftType = ref<null | string>(null);
const appliedKeyword = ref('');
const appliedType = ref<null | string>(null);

function handleSearch() {
  appliedKeyword.value = draftKeyword.value.trim();
  appliedType.value = draftType.value;
  gridApi.reload();
}

function handleReset() {
  draftKeyword.value = '';
  draftType.value = null;
  appliedKeyword.value = '';
  appliedType.value = null;
  gridApi.reload();
}

async function queryMenus(_params: {
  page?: { currentPage: number; pageSize: number };
}) {
  const [filtered, tree] = await Promise.all([
    menuApi.listTree({
      keyword: appliedKeyword.value.trim() || undefined,
      type: appliedType.value || undefined,
    }),
    menuApi.listTree(),
  ]);
  allMenus.value = tree;
  return {
    items: filtered,
    total: filtered.length,
  };
}

const [Grid, gridApi] = useVbenVxeGrid<AigcMenuTreeNode>({
  gridOptions: {
    columns: [
      {
        field: 'name',
        treeNode: true,
        title: '菜单名称',
        minWidth: 220,
      },
      {
        field: 'type',
        title: '类型',
        width: 100,
        formatter: ({ cellValue }: { cellValue: number | string }) =>
          findAuthOptionLabel(MENU_TYPE_OPTIONS, cellValue),
      },
      { field: 'path', title: '路径', minWidth: 180 },
      { field: 'perms', title: '权限标识', minWidth: 180 },
      { field: 'component', title: '组件路径', minWidth: 180 },
      { field: 'orderNo', title: '排序', width: 80 },
      {
        field: 'isShow',
        title: '显示',
        width: 80,
        slots: { default: 'showColumn' },
      },
      {
        field: 'isKeepalive',
        title: '缓存',
        width: 80,
        slots: { default: 'keepaliveColumn' },
      },
      {
        field: 'isDisabled',
        title: '禁用',
        width: 80,
        slots: { default: 'disabledColumn' },
      },
      {
        field: 'updateTime',
        title: '更新时间',
        minWidth: 180,
        formatter: ({ cellValue }: { cellValue: number }) =>
          cellValue
            ? new Date(cellValue).toLocaleString('zh-CN', { hour12: false })
            : '--',
      },
      {
        field: 'actions',
        fixed: 'right',
        slots: { default: 'actionColumn' },
        title: '操作',
        width: 110,
      },
    ],
    height: 'auto',
    pagerConfig: {
      enabled: false,
    } as any,
    proxyConfig: {
      ajax: {
        query: queryMenus,
      },
    },
    rowConfig: {
      keyField: 'id',
    },
    toolbarConfig: {
      refresh: true,
      zoom: true,
    },
    treeConfig: {
      transform: false,
    },
  },
  tableTitle: '菜单管理',
});

function openCreate() {
  currentItem.value = null;
  showEdit.value = true;
}

function openEdit(item: AigcMenu) {
  currentItem.value = item;
  showEdit.value = true;
}

async function handleDelete(item: AigcMenu) {
  if (!item.id) {
    return;
  }
  const menuId = item.id;
  dialog.warning({
    closable: false,
    content: `删除后不可恢复，确认删除菜单「${item.name || '未命名菜单'}」吗？`,
    negativeText: '取消',
    positiveText: '确认删除',
    title: '删除菜单',
    onPositiveClick: async () => {
      await menuApi.remove(menuId);
      message.success('菜单已删除');
      await gridApi.reload();
    },
  });
}

async function handleSave(payload: Partial<AigcMenu>) {
  saving.value = true;
  try {
    if (currentItem.value?.id) {
      await menuApi.update(currentItem.value.id, payload);
      message.success('菜单已更新');
    } else {
      await menuApi.create(payload);
      message.success('菜单已创建');
    }
    showEdit.value = false;
    await gridApi.reload();
  } finally {
    saving.value = false;
  }
}
</script>

<template>
  <Page>
    <ManageCard>
      <template #search>
        <div class="flex items-center gap-2">
          <span class="shrink-0 text-sm text-muted-foreground">关键词</span>
          <NInput
            v-model:value="draftKeyword"
            clearable
            placeholder="菜单名称 / 路径 / 权限标识 / 组件路径"
            style="width: 240px"
            @keyup.enter="handleSearch"
          />
        </div>
        <div class="flex items-center gap-2">
          <span class="shrink-0 text-sm text-muted-foreground">菜单类型</span>
          <NSelect
            v-model:value="draftType"
            :options="MENU_TYPE_OPTIONS"
            clearable
            placeholder="全部类型"
            style="width: 180px"
          />
        </div>
        <div class="ml-auto flex items-center gap-2">
          <NButton @click="handleReset">重置</NButton>
          <NButton type="primary" @click="handleSearch">搜索</NButton>
        </div>
      </template>

      <Grid class="min-h-0 flex-1" grid-class="px-4 pb-4 pt-3">
        <template #toolbar-tools>
          <NButton type="primary" @click="openCreate"> 新建菜单 </NButton>
        </template>

        <template #showColumn="{ row }">
          <NTag
            :bordered="false"
            :type="row.isShow === false ? 'warning' : 'success'"
            round
            size="small"
          >
            {{ row.isShow === false ? '隐藏' : '显示' }}
          </NTag>
        </template>

        <template #keepaliveColumn="{ row }">
          <NTag
            :bordered="false"
            :type="row.isKeepalive ? 'primary' : 'default'"
            round
            size="small"
          >
            {{ row.isKeepalive ? '是' : '否' }}
          </NTag>
        </template>

        <template #disabledColumn="{ row }">
          <NTag
            :bordered="false"
            :type="row.isDisabled ? 'error' : 'success'"
            round
            size="small"
          >
            {{ row.isDisabled ? '是' : '否' }}
          </NTag>
        </template>

        <template #actionColumn="{ row }">
          <div class="flex items-center justify-center gap-1">
            <NButton
              v-tippy="'编辑菜单'"
              circle
              quaternary
              size="small"
              type="primary"
              @click="openEdit(row)"
            >
              <template #icon>
                <SquarePen class="size-4" />
              </template>
            </NButton>
            <NButton
              v-tippy="'删除菜单'"
              circle
              quaternary
              size="small"
              type="error"
              @click="handleDelete(row)"
            >
              <template #icon>
                <Trash2 class="size-4" />
              </template>
            </NButton>
          </div>
        </template>
      </Grid>
    </ManageCard>

    <MenuEdit
      v-model:show="showEdit"
      :menu-tree="filterMenuTree(allMenus, currentItem?.id)"
      :model-value="currentItem"
      :saving="saving"
      @save="handleSave"
    />
  </Page>
</template>
