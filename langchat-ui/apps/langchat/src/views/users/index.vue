<script setup lang="ts">
import type { VxeGridPropTypes } from '#/adapter/vxe-table';
import type { AigcRole } from '#/api/auth/role';
import type { AigcUser, AigcUserRole } from '#/api/auth/user';

import { computed, ref, watch } from 'vue';

import { Page } from '@vben/common-ui';
import { SquarePen, Trash2 } from '@vben/icons';
import { $t } from '@vben/locales';

import { NButton, NInput, NSelect, NTag } from 'naive-ui';

import { dialog, message } from '#/adapter/naive';
import { useVbenVxeGrid } from '#/adapter/vxe-table';
import { roleApi } from '#/api/auth/role';
import { userApi, userRoleApi } from '#/api/auth/user';
import { formatRelativeTime } from '#/views/shared/aigc/time';
import ManageCard from '#/views/shared/auth/manage-card.vue';
import {
  findAuthOptionLabel,
  userSexOptions,
  userStatusOptions,
} from '#/views/shared/auth/options';

import UserEdit from './edit.vue';

interface UserFormPayload extends Partial<AigcUser> {
  roleIds?: string[];
}

const roles = ref<AigcRole[]>([]);
const userRoles = ref<AigcUserRole[]>([]);
const saving = ref(false);
const showEdit = ref(false);
const currentItem = ref<null | UserFormPayload>(null);

// 搜索条件：draft 为输入框草稿值，applied 为已生效值（工具栏刷新时沿用生效值）
const draftKeyword = ref('');
const draftStatus = ref<null | number>(null);
const appliedKeyword = ref('');
const appliedStatus = ref<null | number>(null);

const roleNameMap = () =>
  Object.fromEntries(
    roles.value.map((item) => [
      item.id ?? '',
      item.name || item.code || $t('roles.card.unnamed'),
    ]),
  ) as Record<string, string>;

const roleOptions = () =>
  roles.value.map((item) => ({
    label: item.name || item.code || $t('roles.card.unnamed'),
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

const gridColumns = computed<VxeGridPropTypes.Columns<AigcUser>>(() => [
  { type: 'seq', title: $t('users.columns.seq'), width: 60 },
  {
    field: 'username',
    title: $t('users.columns.username'),
    minWidth: 140,
  },
  {
    field: 'realName',
    title: $t('users.columns.realName'),
    minWidth: 140,
  },
  {
    field: 'status',
    title: $t('users.columns.status'),
    width: 100,
    formatter: ({ cellValue }: { cellValue: number | string }) =>
      findAuthOptionLabel(userStatusOptions(), cellValue),
  },
  {
    field: 'sex',
    title: $t('users.columns.gender'),
    width: 100,
    formatter: ({ cellValue }: { cellValue: number | string }) =>
      findAuthOptionLabel(userSexOptions(), cellValue),
  },
  {
    field: 'phone',
    title: $t('users.columns.phone'),
    minWidth: 140,
  },
  {
    field: 'email',
    title: $t('users.columns.email'),
    minWidth: 180,
  },
  {
    field: 'roleIds',
    title: $t('users.columns.roles'),
    minWidth: 220,
    slots: { default: 'roleColumn' },
  },
  {
    field: 'updateTime',
    title: $t('users.columns.updateTime'),
    minWidth: 180,
    formatter: ({ cellValue }: { cellValue: number }) =>
      cellValue ? formatRelativeTime(cellValue) : '--',
  },
  {
    field: 'actions',
    fixed: 'right',
    slots: { default: 'actionColumn' },
    title: $t('common.labels.actions'),
    width: 110,
  },
]);

const [Grid, gridApi] = useVbenVxeGrid<AigcUser>({
  gridOptions: {
    columns: gridColumns.value,
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
  tableTitle: $t('users.list.tableTitle'),
});

watch(
  gridColumns,
  (columns) => {
    gridApi.setGridOptions({ columns });
  },
  { immediate: true },
);

watch(
  () => $t('users.list.tableTitle'),
  (value) => {
    gridApi.setState({ tableTitle: value });
  },
  { immediate: true },
);

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
    content: $t('common.messages.deleteConfirmContent', {
      name: item.realName || item.username || $t('users.card.unnamed'),
    }),
    negativeText: $t('common.actions.cancel'),
    positiveText: $t('common.actions.confirmDelete'),
    title: $t('users.messages.deleteTitle'),
    onPositiveClick: async () => {
      await userApi.remove(userId);
      message.success($t('users.messages.deleted'));
      await gridApi.reload();
    },
  });
}

async function handleSave(payload: UserFormPayload) {
  saving.value = true;
  try {
    const nextRoleIds = [...new Set(payload.roleIds)];
    const userPayload: Partial<AigcUser> = { ...payload };
    delete (userPayload as UserFormPayload).roleIds;
    if (!userPayload.password) {
      delete userPayload.password;
    }

    let targetUserId = currentItem.value?.id ?? '';
    if (currentItem.value?.id) {
      await userApi.update(currentItem.value.id, userPayload);
      message.success($t('users.messages.updated'));
    } else {
      await userApi.create(userPayload);
      const users = await userApi.list();
      targetUserId =
        users.find((item) => item.username === userPayload.username)?.id ?? '';
      message.success($t('users.messages.created'));
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
          <span class="shrink-0 text-sm text-muted-foreground">{{
            $t('common.labels.keyword')
          }}</span>
          <NInput
            v-model:value="draftKeyword"
            clearable
            :placeholder="$t('users.list.keywordPlaceholder')"
            style="width: 240px"
            @keyup.enter="handleSearch"
          />
        </div>
        <div class="flex items-center gap-2">
          <span class="shrink-0 text-sm text-muted-foreground">{{
            $t('common.labels.status')
          }}</span>
          <NSelect
            v-model:value="draftStatus"
            :options="userStatusOptions()"
            clearable
            :placeholder="$t('users.list.statusPlaceholder')"
            style="width: 180px"
          />
        </div>
        <div class="ml-auto flex items-center gap-2">
          <NButton @click="handleReset">{{ $t('common.actions.reset') }}</NButton>
          <NButton type="primary" @click="handleSearch">
{{
            $t('common.actions.search')
          }}
</NButton>
        </div>
      </template>

      <Grid class="min-h-0 flex-1" grid-class="px-4 pb-4 pt-3">
        <template #toolbar-tools>
          <NButton type="primary" @click="openCreate">
            {{ $t('users.actions.create') }}
          </NButton>
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
              {{ $t('users.list.notAssignedRoles') }}
            </span>
          </div>
        </template>

        <template #actionColumn="{ row }">
          <div class="flex items-center justify-center gap-1">
            <NButton
              v-tippy="$t('users.list.editUser')"
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
              v-tippy="$t('users.list.deleteUser')"
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
