<script setup lang="ts">
import type { AigcRole } from '#/api/auth/role';
import type { AigcUser, AigcUserRole } from '#/api/auth/user';

import { ref } from 'vue';
import { Page } from '@vben/common-ui';
import { SquarePen, Trash2 } from '@vben/icons';
import { NButton, NInput, NSelect, NTag } from 'naive-ui';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import { dialog, message } from '#/adapter/naive';
import { roleApi } from '#/api/auth/role';
import { userApi, userRoleApi } from '#/api/auth/user';
import ManageCard from '#/views/shared/auth/manage-card.vue';
import {
  findAuthOptionLabel,
  USER_SEX_OPTIONS,
  USER_STATUS_OPTIONS,
} from '#/views/shared/auth/options';
import UserEdit from './edit.vue';

interface UserFormPayload extends Partial<AigcUser> {
  roleIds?: string[];
}

const roles = ref<AigcRole[]>([]);
const userRoles = ref<AigcUserRole[]>([]);
const saving = ref(false);
const showEdit = ref(false);
const currentItem = ref<UserFormPayload | null>(null);

// 搜索条件：draft 为输入框草稿值，applied 为已生效值（工具栏刷新时沿用生效值）
const draftKeyword = ref('');
const draftStatus = ref<null | number>(null);
const appliedKeyword = ref('');
const appliedStatus = ref<null | number>(null);

const roleNameMap = () =>
  Object.fromEntries(
    roles.value.map((item) => [
      item.id ?? '',
      item.name || item.code || '未命名角色',
    ]),
  ) as Record<string, string>;

const roleOptions = () =>
  roles.value.map((item) => ({
    label: item.name || item.code || '未命名角色',
    value: item.id ?? '',
  }));

function resolveRoleIds(userId?: string) {
  return userRoles.value
    .filter((item) => item.userId === userId)
    .map((item) => item.roleId ?? '')
    .filter(Boolean);
}

function resolveRoleLabels(userId?: string) {
  const nameMap = roleNameMap();
  return resolveRoleIds(userId)
    .map((roleId) => nameMap[roleId] || roleId)
    .filter(Boolean);
}

function handleSearch() {
  appliedKeyword.value = draftKeyword.value.trim();
  appliedStatus.value = draftStatus.value;
  gridApi.reload();
}

function handleReset() {
  draftKeyword.value = '';
  draftStatus.value = null;
  appliedKeyword.value = '';
  appliedStatus.value = null;
  gridApi.reload();
}

async function queryUsers(params: {
  page?: { currentPage: number; pageSize: number };
}) {
  const [users, roleList, relations] = await Promise.all([
    userApi.list(),
    roleApi.list(),
    userRoleApi.list(),
  ]);
  roles.value = roleList;
  userRoles.value = relations;

  const keyword = appliedKeyword.value.trim().toLowerCase();
  const status = appliedStatus.value;
  const filtered = users.filter((item) => {
    const matchKeyword =
      !keyword ||
      [item.username, item.realName, item.phone, item.email]
        .map((value) => String(value ?? '').toLowerCase())
        .some((value) => value.includes(keyword));
    const matchStatus = status === null || item.status === status;
    return matchKeyword && matchStatus;
  });

  const currentPage = params.page?.currentPage ?? 1;
  const pageSize = params.page?.pageSize ?? 10;
  const start = (currentPage - 1) * pageSize;
  return {
    items: filtered.slice(start, start + pageSize),
    total: filtered.length,
  };
}

const [Grid, gridApi] = useVbenVxeGrid<AigcUser>({
  gridOptions: {
    columns: [
      { type: 'seq', title: '序号', width: 60 },
      { field: 'username', title: '用户名', minWidth: 140 },
      { field: 'realName', title: '姓名', minWidth: 140 },
      {
        field: 'status',
        title: '状态',
        width: 100,
        formatter: ({ cellValue }: { cellValue: number | string }) =>
          findAuthOptionLabel(USER_STATUS_OPTIONS, cellValue),
      },
      {
        field: 'sex',
        title: '性别',
        width: 100,
        formatter: ({ cellValue }: { cellValue: number | string }) =>
          findAuthOptionLabel(USER_SEX_OPTIONS, cellValue),
      },
      { field: 'phone', title: '手机号', minWidth: 140 },
      { field: 'email', title: '邮箱', minWidth: 180 },
      {
        field: 'roleIds',
        title: '角色',
        minWidth: 220,
        slots: { default: 'roleColumn' },
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
        query: queryUsers,
      },
    },
    toolbarConfig: {
      refresh: true,
      zoom: true,
    },
  },
  tableTitle: '用户管理',
});

function openCreate() {
  currentItem.value = null;
  showEdit.value = true;
}

function openEdit(item: AigcUser) {
  currentItem.value = {
    ...item,
    roleIds: resolveRoleIds(item.id),
  };
  showEdit.value = true;
}

async function handleDelete(item: AigcUser) {
  if (!item.id) {
    return;
  }
  const userId = item.id;
  dialog.warning({
    closable: false,
    content: `删除后不可恢复，确认删除用户「${item.realName || item.username || '未命名用户'}」吗？`,
    negativeText: '取消',
    positiveText: '确认删除',
    title: '删除用户',
    onPositiveClick: async () => {
      await userApi.remove(userId);
      message.success('用户已删除');
      await gridApi.reload();
    },
  });
}

async function handleSave(payload: UserFormPayload) {
  saving.value = true;
  try {
    const nextRoleIds = [...new Set(payload.roleIds ?? [])];
    const userPayload: Partial<AigcUser> = { ...payload };
    delete (userPayload as UserFormPayload).roleIds;
    if (!userPayload.password) {
      delete userPayload.password;
    }

    let targetUserId = currentItem.value?.id ?? '';
    if (currentItem.value?.id) {
      await userApi.update(currentItem.value.id, userPayload);
      message.success('用户已更新');
    } else {
      await userApi.create(userPayload);
      const users = await userApi.list();
      targetUserId =
        users.find((item) => item.username === userPayload.username)?.id ?? '';
      message.success('用户已创建');
    }

    if (targetUserId) {
      await syncUserRoles(targetUserId, nextRoleIds);
    }
    showEdit.value = false;
    await gridApi.reload();
  } finally {
    saving.value = false;
  }
}

async function syncUserRoles(userId: string, nextRoleIds: string[]) {
  const currentRoleIds = userRoles.value
    .filter((item) => item.userId === userId)
    .map((item) => item.roleId ?? '')
    .filter(Boolean);

  const addRoleIds = nextRoleIds.filter(
    (roleId) => !currentRoleIds.includes(roleId),
  );
  const removeRoleIds = currentRoleIds.filter(
    (roleId) => !nextRoleIds.includes(roleId),
  );

  await Promise.all([
    ...addRoleIds.map((roleId) =>
      userRoleApi.create({
        roleId,
        userId,
      }),
    ),
    ...removeRoleIds.map((roleId) =>
      userRoleApi.remove({
        roleId,
        userId,
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
            placeholder="用户名 / 姓名 / 手机号 / 邮箱"
            style="width: 240px"
            @keyup.enter="handleSearch"
          />
        </div>
        <div class="flex items-center gap-2">
          <span class="shrink-0 text-sm text-muted-foreground">状态</span>
          <NSelect
            v-model:value="draftStatus"
            :options="USER_STATUS_OPTIONS"
            clearable
            placeholder="全部状态"
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
          <NButton type="primary" @click="openCreate"> 新建用户 </NButton>
        </template>

        <template #roleColumn="{ row }">
          <div class="flex flex-wrap gap-1 py-1">
            <NTag
              v-for="role in resolveRoleLabels(row.id)"
              :key="role"
              :bordered="false"
              round
              size="small"
              type="primary"
            >
              {{ role }}
            </NTag>
            <span
              v-if="resolveRoleLabels(row.id).length === 0"
              class="text-xs text-muted-foreground"
            >
              未分配角色
            </span>
          </div>
        </template>

        <template #actionColumn="{ row }">
          <div class="flex items-center justify-center gap-1">
            <NButton
              v-tippy="'编辑用户'"
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
              v-tippy="'删除用户'"
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

    <UserEdit
      v-model:show="showEdit"
      :model-value="currentItem"
      :role-options="roleOptions()"
      :saving="saving"
      @save="handleSave"
    />
  </Page>
</template>
