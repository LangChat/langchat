<script lang="ts" setup>
import type { UploadFileInfo } from 'naive-ui';

import type { AigcSkill } from '#/api/aigc/skill';

import { computed, onMounted, ref, watch } from 'vue';

import { Page, useVbenModal } from '@vben/common-ui';
import { FolderUp, PackageOpen } from '@vben/icons';
import { $t } from '@vben/locales';

import {
  NAlert,
  NPagination,
  NRadioButton,
  NRadioGroup,
  NUpload,
  NUploadDragger,
} from 'naive-ui';

import { dialog, message } from '#/adapter/naive';
import { skillApi } from '#/api/aigc/skill';
import LcActionCard from '#/components/LcActionCard/index.vue';
import LcListCard from '#/components/LcListCard/index.vue';
import { aigcCommonTagOptions } from '#/views/shared/aigc/options';
import { parseTagList, stringifyTagList } from '#/views/shared/aigc/tags';

import SkillCard from './card.vue';
import SkillEdit from './edit.vue';

const loading = ref(false);
const showEdit = ref(false);
const uploadMode = ref<'folder' | 'zip'>('zip');
const zipFileList = ref<UploadFileInfo[]>([]);
const folderFileList = ref<UploadFileInfo[]>([]);
const uploadTags = ref<string[]>([]);
const keyword = ref('');
const selectedTag = ref('ALL');
const currentPage = ref(1);
const pageSize = ref(9);
const items = ref<AigcSkill[]>([]);
const currentSkill = ref<null | Partial<AigcSkill>>(null);

const tagOptions = computed(() =>
  aigcCommonTagOptions().map((item) => ({
    label: item.label,
    value: String(item.value),
  })),
);

const tagFilterOptions = computed(() => [
  { label: $t('common.labels.all'), value: 'ALL' },
  ...tagOptions.value,
]);

const filteredItems = computed(() => {
  const query = keyword.value.trim().toLowerCase();
  return items.value.filter((item) => {
    const matchKeyword =
      !query ||
      [item.description, item.name, item.title, item.tags]
        .map((value) => String(value ?? '').toLowerCase())
        .some((value) => value.includes(query));
    const tags = parseTagList(item.tags);
    const matchTag =
      selectedTag.value === 'ALL' || tags.includes(selectedTag.value);
    return matchKeyword && matchTag;
  });
});

const pagedItems = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value;
  return filteredItems.value.slice(start, start + pageSize.value);
});

const uploadReady = computed(() =>
  uploadMode.value === 'zip'
    ? zipFileList.value.length > 0
    : folderFileList.value.length > 0,
);

watch([keyword, selectedTag], () => {
  currentPage.value = 1;
});

const actionItems = computed(() => [
  { key: 'upload', label: $t('skills.list.upload'), icon: PackageOpen },
]);

const [UploadModal, uploadModalApi] = useVbenModal({
  class: 'w-[640px]',
  confirmDisabled: true,
  onConfirm: handleUpload,
});

watch(
  () => $t('skills.list.startUpload'),
  (value) => {
    uploadModalApi.setState({ confirmText: value });
  },
  { immediate: true },
);

watch(
  () => $t('skills.list.upload'),
  (value) => {
    uploadModalApi.setState({ title: value });
  },
  { immediate: true },
);

async function loadList() {
  loading.value = true;
  try {
    items.value = await skillApi.list();
  } finally {
    loading.value = false;
  }
}

function openUpload() {
  uploadMode.value = 'zip';
  zipFileList.value = [];
  folderFileList.value = [];
  uploadModalApi.setState({ confirmDisabled: true });
  uploadModalApi.open();
}

function handleAction(action: { key: string }) {
  if (action.key === 'upload') {
    openUpload();
  }
}

function resolveRelativePath(option: UploadFileInfo) {
  const rawFile = option.file as undefined | { webkitRelativePath?: string };
  return rawFile?.webkitRelativePath || option.fullPath || option.name;
}

async function handleUpload() {
  uploadModalApi.setState({ confirmLoading: true });
  try {
    const tags = stringifyTagList(uploadTags.value);
    let skill: AigcSkill;
    if (uploadMode.value === 'zip') {
      const file = zipFileList.value[0]?.file;
      if (!file) {
        message.warning($t('skills.messages.zipRequired'));
        return;
      }
      const formData = new FormData();
      formData.append('file', file);
      if (tags) {
        formData.append('tags', tags);
      }
      skill = await skillApi.uploadPackage(formData);
    } else {
      const entries = folderFileList.value
        .map((option) => {
          const file = option.file;
          if (!file) {
            return null;
          }
          return { file, path: resolveRelativePath(option) };
        })
        .filter((entry): entry is { file: File; path: string } => !!entry);
      if (entries.length === 0) {
        message.warning($t('skills.messages.folderRequired'));
        return;
      }
      const formData = new FormData();
      entries.forEach((entry) => {
        formData.append('files', entry.file);
        formData.append('paths', entry.path);
      });
      if (tags) {
        formData.append('tags', tags);
      }
      skill = await skillApi.uploadFolder(formData);
    }
    message.success(
      $t('skills.messages.uploaded', { name: skill.title || skill.name }),
    );
    uploadModalApi.close();
    await loadList();
  } finally {
    uploadModalApi.setState({ confirmLoading: false });
  }
}

watch(uploadReady, (ready) => {
  uploadModalApi.setState({ confirmDisabled: !ready });
});

function handleEdit(item: AigcSkill) {
  currentSkill.value = item;
  showEdit.value = true;
}

function handleToggle(item: AigcSkill) {
  if (!item.id) {
    return;
  }
  const nextEnabled = !(item.enabled ?? false);
  const name = item.title || item.name;
  dialog.warning({
    closable: false,
    content: nextEnabled
      ? $t('skills.messages.enableConfirm', { name })
      : $t('skills.messages.disableConfirm', { name }),
    negativeText: $t('common.actions.cancel'),
    positiveText: $t('common.actions.confirm'),
    title: nextEnabled
      ? $t('skills.messages.enableTitle')
      : $t('skills.messages.disableTitle'),
    onPositiveClick: async () => {
      await skillApi.update(item.id!, { enabled: nextEnabled });
      message.success(
        nextEnabled
          ? $t('skills.messages.enabled')
          : $t('skills.messages.disabled'),
      );
      await loadList();
    },
  });
}

async function handleDelete(item: AigcSkill) {
  if (!item.id) {
    return;
  }
  dialog.warning({
    closable: false,
    content: $t('skills.messages.deleteConfirm', {
      name: item.title || item.name,
    }),
    negativeText: $t('common.actions.cancel'),
    positiveText: $t('common.actions.confirmDelete'),
    title: $t('skills.messages.deleteTitle'),
    onPositiveClick: async () => {
      await skillApi.remove(item.id!);
      message.success($t('skills.messages.deleted'));
      await loadList();
    },
  });
}

function handleSaved(updated: AigcSkill) {
  if (currentSkill.value) {
    Object.assign(currentSkill.value, updated);
  }
}

onMounted(loadList);
</script>

<template>
  <Page>
    <div class="flex flex-col gap-2">
      <LcListCard
        :active-tag="selectedTag"
        :items="pagedItems"
        :loading="loading"
        :search-placeholder="$t('skills.list.searchPlaceholder')"
        :search-value="keyword"
        :tags="tagFilterOptions"
        @update:active-tag="selectedTag = $event"
        @update:search-value="keyword = $event"
      >
        <template #leading-card>
          <LcActionCard
            :actions="actionItems"
            :description="$t('skills.list.actionsDescription')"
            :title="$t('skills.list.actionsTitle')"
            @action="handleAction"
          />
        </template>
        <template #item="{ item }">
          <SkillCard
            :item="item"
            @delete="handleDelete"
            @edit="handleEdit"
            @toggle="handleToggle"
          />
        </template>
      </LcListCard>

      <div v-if="filteredItems.length > pageSize" class="flex justify-end">
        <NPagination
          v-model:page="currentPage"
          v-model:page-size="pageSize"
          :item-count="filteredItems.length"
          :page-sizes="[6, 9, 12, 18]"
          show-size-picker
        />
      </div>

      <SkillEdit
        v-model:show="showEdit"
        :skill="currentSkill"
        @saved="handleSaved"
      />
    </div>

    <UploadModal>
      <div class="flex flex-col gap-3">
        <NAlert type="info" :show-icon="true" :title="$t('skills.list.formatTitle')">
          <div class="flex flex-col gap-1 text-xs leading-5">
            <span>
              {{ $t('skills.list.formatFirstPre') }}<b>SKILL.md</b>{{ $t('skills.list.formatFirstPost') }}<code>name</code>{{ $t('skills.list.formatFirstNameHint') }}<code>description</code>{{ $t('skills.list.formatFirstDescHint') }}<code>version</code>{{ $t('skills.list.formatFirstVersionHint') }}
            </span>
            <span>
              {{ $t('skills.list.formatSecondPre') }}<code>scripts/</code>{{ $t('skills.list.formatSecondScriptsHint') }}<code>references/</code>{{ $t('skills.list.formatSecondRefsHint') }}<code>assets/</code>{{ $t('skills.list.formatSecondAssetsHint') }}
            </span>
            <span>{{ $t('skills.list.formatThird') }}</span>
            <span>{{ $t('skills.list.formatFourth') }}</span>
          </div>
        </NAlert>

        <NRadioGroup v-model:value="uploadMode">
          <NRadioButton value="zip">
{{
            $t('skills.list.zipMode')
          }}
</NRadioButton>
          <NRadioButton value="folder">
{{
            $t('skills.list.folderMode')
          }}
</NRadioButton>
        </NRadioGroup>

        <NUpload
          v-if="uploadMode === 'zip'"
          v-model:file-list="zipFileList"
          accept=".zip"
          :default-upload="false"
          :max="1"
        >
          <NUploadDragger>
            <div class="flex flex-col items-center gap-1 py-4">
              <PackageOpen class="size-8 text-primary" />
              <span class="text-sm">{{ $t('skills.list.zipDropTitle') }}</span>
              <span class="text-[11px] text-muted-foreground">
                {{ $t('skills.list.zipDropHint') }}
              </span>
            </div>
          </NUploadDragger>
        </NUpload>

        <NUpload
          v-else
          v-model:file-list="folderFileList"
          :default-upload="false"
          :directory="true"
          :directory-dnd="true"
          multiple
        >
          <NUploadDragger>
            <div class="flex flex-col items-center gap-1 py-4">
              <FolderUp class="size-8 text-primary" />
              <span class="text-sm">{{
                $t('skills.list.folderDropTitle')
              }}</span>
              <span class="text-[11px] text-muted-foreground">
                {{ $t('skills.list.folderDropHint') }}
              </span>
            </div>
          </NUploadDragger>
        </NUpload>

        <div class="flex items-center gap-2">
          <span class="text-xs text-muted-foreground">
            {{
              $t('skills.list.selectedFiles', {
                count:
                  uploadMode === 'zip'
                    ? zipFileList.length
                    : folderFileList.length,
              })
            }}
          </span>
        </div>
      </div>
    </UploadModal>
  </Page>
</template>
