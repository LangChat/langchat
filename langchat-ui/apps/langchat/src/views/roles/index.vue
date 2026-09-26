<script setup lang="ts">
import type { AigcMenuTreeNode } from '#/api/auth/menu';
import type { AigcRole, AigcRoleMenu } from '#/api/auth/role';

import { ref } from 'vue';
import { Page } from '@vben/common-ui';
import { SquarePen, Trash2 } from '@vben/icons';
import { NButton, NInput, NTag } from 'naive-ui';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import { dialog, message } from '#/adapter/naive';
import { menuApi } from '#/api/auth/menu';
import { roleApi, roleMenuApi } from '#/api/auth/role';
import ManageCard from '#/views/shared/auth/manage-card.vue';
import { buildMenuNameMap } from '#/views/shared/auth/menu-tree';
import RoleEdit from './edit.vue';

interface RoleFormPayload extends Partial<AigcRole> {
  menuIds?: string[];
}

const menuTree = ref<AigcMenuTreeNode[]>([]);
const roleMenus = ref<AigcRoleMenu[]>([]);
const saving = ref(false);
const showEdit = ref(false);
const currentItem = ref<RoleFormPayload | null>(null);

// 搜索条件：draft 为输入框草稿值，applied 为已生效值（工具栏刷新时沿用生效值）
const draftKeyword = ref('');
const appliedKeyword = ref('');

const menuNameMap = () => buildMenuNameMap(menuTree.value);

// 系统内置角色（如超管）不允许编辑与删除
const BUILTIN_ROLE_CODES = new Set(['ADMIN']);

function isBuiltinRole(role: AigcRole) {
  return BUILTIN_ROLE_CODES.has(String(role.code || '').toUpperCase());
}

function resolveMenuIds(roleId?: string) {
  return roleMenus.value
    .filter((item) => item.roleId === roleId)
    .map((item) => item.menuId ?? '')
    .filter(Boolean);
}

function resolveMenuLabels(roleId?: string) {
  const nameMap = menuNameMap();
  return resolveMenuIds(roleId)
    .map((menuId) => nameMap[menuId] || menuId)
    .filter(Boolean);
}

function handleSearch() {
  appliedKeyword.value = draftKeyword.value.trim();
  gridApi.reload();
}

function handleReset() {
  draftKeyword.value = '';
  appliedKeyword.value = '';
  gridApi.reload();
}

async function queryRoles(params: {
  page?: { currentPage: number; pageSize: number };
}) {
  const [roles, menuList, relations] = await Promise.all([
    roleApi.list(),
    menuApi.listTree(),
    roleMenuApi.list(),
  ]);
  menuTree.value = menuList;
  roleMenus.value = relations;

  const keyword = appliedKeyword.value.trim().toLowerCase();
  const filtered = roles.filter(
    (item) =>
      !keyword ||
      [item.name, item.code, item.description]
        .map((value) => String(value ?? '').toLowerCase())
        .some((value) => value.includes(keyword)),
  );

  const currentPage = params.page?.currentPage ?? 1;
  const pageSize = params.page?.pageSize ?? 10;
  const start = (currentPage - 1) * pageSize;
  return {
    items: filtered.slice(start, start + pageSize),
    total: filtered.length,
  };
}

const [Grid, gridApi] = useVbenVxeGrid<AigcRole>({
  gridOptions: {
    columns: [
      { type: 'seq', title: '序号', width: 60 },
      { field: 'name', title: '角色名称', minWidth: 160 },
      { field: 'code', title: '角色编码', minWidth: 140 },
      { field: 'description', title: '描述', minWidth: 220 },
      {
        field: 'menuIds',
        title: '菜单授权',
        minWidth: 260,
        slots: { default: 'menuColumn' },
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
      pageSize: 10,
      pageSizes: [10, 20, 50],
    },
    proxyConfig: {
      ajax: {
        query: queryRoles,
      },
    },
    toolbarConfig: {
      refresh: true,
      zoom: true,
    },
  },
  tableTitle: '角色管理',
});

function openCreate() {
  currentItem.value = null;
  showEdit.value = true;
}

function openEdit(item: AigcRole) {
  currentItem.value = {
    ...item,
    menuIds: resolveMenuIds(item.id),
  };
  showEdit.value = true;
}

async function handleDelete(item: AigcRole) {
  if (!item.id) {
    return;
  }
  const roleId = item.id;
  dialog.warning({
    closable: false,
    content: `删除后不可恢复，确认删除角色「${item.name || item.code || '未命名角色'}」吗？`,
    negativeText: '取消',
    positiveText: '确认删除',
    title: '删除角色',
    onPositiveClick: async () => {
      await roleApi.remove(roleId);
      message.success('角色已删除');
      await gridApi.reload();
    },
  });
}

async function handleSave(payload: RoleFormPayload) {
  saving.value = true;
  try {
    const nextMenuIds = [...new Set(payload.menuIds ?? [])];
    const rolePayload: Partial<AigcRole> = { ...payload };
    delete (rolePayload as RoleFormPayload).menuIds;

    let targetRoleId = currentItem.value?.id ?? '';
    if (currentItem.value?.id) {
      await roleApi.update(currentItem.value.id, rolePayload);
      message.success('角色已更新');
    } else {
      await roleApi.create(rolePayload);
      const roles = await roleApi.list();
      targetRoleId =
        roles.find((item) => item.code === rolePayload.code)?.id ?? '';
      message.success('角色已创建');
    }

    if (targetRoleId) {
      await syncRoleMenus(targetRoleId, nextMenuIds);
    }
    showEdit.value = false;
    await gridApi.reload();
  } finally {
    saving.value = false;
  }
}

async function syncRoleMenus(roleId: string, nextMenuIds: string[]) {
  const currentMenuIds = roleMenus.value
    .filter((item) => item.roleId === roleId)
    .map((item) => item.menuId ?? '')
    .filter(Boolean);

  const addMenuIds = nextMenuIds.filter(
    (menuId) => !currentMenuIds.includes(menuId),
  );
  const removeMenuIds = currentMenuIds.filter(
    (menuId) => !nextMenuIds.includes(menuId),
  );

  await Promise.all([
    ...addMenuIds.map((menuId) =>
      roleMenuApi.create({
        menuId,
        roleId,
      }),
    ),
    ...removeMenuIds.map((menuId) =>
      roleMenuApi.remove({
        menuId,
        roleId,
      }),
    ),
  ]);
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
            placeholder="角色名称 / 编码 / 描述"
            style="width: 240px"
            @keyup.enter="handleSearch"
          />
        </div>
        <div class="ml-auto flex items-center gap-2">
          <NButton @click="handleReset">重置</NButton>
          <NButton type="primary" @click="handleSearch">搜索</NButton>
        </div>
      </template>

      <Grid class="min-h-0 flex-1" grid-class="px-4 pb-4 pt-3">
        <template #toolbar-tools>
          <NButton type="primary" @click="openCreate"> 新建角色 </NButton>
        </template>

        <template #menuColumn="{ row }">
          <div class="flex flex-wrap gap-1 py-1">
            <NTag
              v-for="menu in resolveMenuLabels(row.id).slice(0, 6)"
              :key="menu"
              :bordered="false"
              round
              size="small"
              type="info"
            >
              {{ menu }}
            </NTag>
            <span
              v-if="resolveMenuLabels(row.id).length === 0"
              class="text-xs text-muted-foreground"
            >
              未授权菜单
            </span>
            <span
              v-else-if="resolveMenuLabels(row.id).length > 6"
              class="text-xs text-muted-foreground"
            >
              另有 {{ resolveMenuLabels(row.id).length - 6 }} 项
            </span>
          </div>
        </template>

        <template #actionColumn="{ row }">
          <div class="flex items-center justify-center gap-1">
            <NButton
              v-tippy="isBuiltinRole(row) ? '系统内置角色，不可编辑' : '编辑角色'"
              :disabled="isBuiltinRole(row)"
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
              v-tippy="isBuiltinRole(row) ? '系统内置角色，不可删除' : '删除角色'"
              :disabled="isBuiltinRole(row)"
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

    <RoleEdit
      v-model:show="showEdit"
      :menu-tree="menuTree"
      :model-value="currentItem"
      :saving="saving"
      @save="handleSave"
    />
  </Page>
</template>
