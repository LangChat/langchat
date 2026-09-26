<script lang="ts" setup>
import type { UploadFileInfo } from 'naive-ui';
import type { AigcSkill } from '#/api/aigc/skill';

import { computed, onMounted, ref, watch } from 'vue';

import { Page } from '@vben/common-ui';
import { FolderUp, PackageOpen, RefreshCcw } from '@vben/icons';

import {
  NAlert,
  NButton,
  NModal,
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
import { AIGC_COMMON_TAG_OPTIONS } from '#/views/shared/aigc/options';
import { parseTagList, stringifyTagList } from '#/views/shared/aigc/tags';

import SkillCard from './card.vue';
import SkillEdit from './edit.vue';

const loading = ref(false);
const showEdit = ref(false);
const showUpload = ref(false);
const uploading = ref(false);
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

const tagOptions = AIGC_COMMON_TAG_OPTIONS.map((item) => ({
  label: item.label,
  value: String(item.value),
}));

const tagFilterOptions = computed(() => [
  { label: '全部', value: 'ALL' },
  ...tagOptions,
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
  { key: 'upload-zip', label: '上传技能包 (zip)', icon: PackageOpen },
  { key: 'upload-folder', label: '上传技能文件夹', icon: FolderUp },
  { key: 'refresh', label: '刷新列表', icon: RefreshCcw },
]);

async function loadList() {
  loading.value = true;
  try {
    items.value = await skillApi.list();
  } finally {
    loading.value = false;
  }
}

function openUpload(mode: 'folder' | 'zip') {
  uploadMode.value = mode;
  zipFileList.value = [];
  folderFileList.value = [];
  showUpload.value = true;
}

function handleAction(action: { key: string }) {
  if (action.key === 'refresh') {
    void loadList();
    return;
  }
  if (action.key === 'upload-zip') {
    openUpload('zip');
    return;
  }
  if (action.key === 'upload-folder') {
    openUpload('folder');
  }
}

function resolveRelativePath(option: UploadFileInfo) {
  const rawFile = option.file as undefined | { webkitRelativePath?: string };
  return rawFile?.webkitRelativePath || option.fullPath || option.name;
}

async function handleUpload() {
  uploading.value = true;
  try {
    const tags = stringifyTagList(uploadTags.value);
    let skill: AigcSkill;
    if (uploadMode.value === 'zip') {
      const file = zipFileList.value[0]?.file;
      if (!file) {
        message.warning('请选择技能包 zip 文件');
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
        message.warning('请选择技能文件夹');
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
    message.success(`技能包已安装：${skill.title || skill.name}`);
    showUpload.value = false;
    await loadList();
  } finally {
    uploading.value = false;
  }
}

function handleEdit(item: AigcSkill) {
  currentSkill.value = item;
  showEdit.value = true;
}

function handleToggle(item: AigcSkill) {
  if (!item.id) {
    return;
  }
  const nextEnabled = !(item.enabled ?? false);
  dialog.warning({
    closable: false,
    content: nextEnabled
      ? `确认启用技能「${item.title || item.name}」吗？启用后 Agent 可调用该技能。`
      : `确认停用技能「${item.title || item.name}」吗？停用后 Agent 将无法调用该技能。`,
    negativeText: '取消',
    positiveText: nextEnabled ? '确认启用' : '确认停用',
    title: nextEnabled ? '启用技能' : '停用技能',
    onPositiveClick: async () => {
      await skillApi.update(item.id!, { enabled: nextEnabled });
      message.success(nextEnabled ? '技能已启用' : '技能已停用');
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
    content: `删除将同时移除 OSS 原始包与本地工作区，且不可恢复。确认删除「${item.title || item.name}」吗？`,
    negativeText: '取消',
    positiveText: '确认删除',
    title: '删除技能',
    onPositiveClick: async () => {
      await skillApi.remove(item.id!);
      message.success('技能已删除');
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
        search-placeholder="按技能名称、描述、标签搜索"
        :search-value="keyword"
        :tags="tagFilterOptions"
        @update:active-tag="selectedTag = $event"
        @update:search-value="keyword = $event"
      >
        <template #leading-card>
          <LcActionCard
            :actions="actionItems"
            description="技能以标准技能包形式管理：上传 zip 或文件夹，根目录必须包含 SKILL.md。"
            title="技能操作"
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

    <NModal
      v-model:show="showUpload"
      :auto-focus="false"
      preset="card"
      class="w-[640px]"
      title="上传技能包"
    >
      <div class="flex flex-col gap-3">
        <NAlert type="info" :show-icon="true" title="标准技能包格式">
          <div class="flex flex-col gap-1 text-xs leading-5">
            <span>
              1. 包根目录必须包含
              <b>SKILL.md</b>：使用 YAML frontmatter 声明
              <code>name</code>（技能标识，必填）、<code>description</code>（技能描述）、<code>version</code> 等字段，正文为标准化的技能指令文档。
            </span>
            <span>
              2. 可选目录：<code>scripts/</code>（脚本）、<code>references/</code>（参考文档）、<code>assets/</code>（资源文件）。
            </span>
            <span>
              3. 支持 .zip 压缩包或整个文件夹（自动解析目录结构），单包最大 500MB。
            </span>
            <span>
              4. 原始包将归档到 OSS，解压副本存放在服务器本地工作区，运行时直接调用本地技能文档。
            </span>
          </div>
        </NAlert>

        <NRadioGroup v-model:value="uploadMode">
          <NRadioButton value="zip">zip 压缩包</NRadioButton>
          <NRadioButton value="folder">文件夹</NRadioButton>
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
              <span class="text-sm">点击或拖拽 zip 技能包到此处</span>
              <span class="text-[11px] text-muted-foreground">
                单个 .zip 文件，根目录需包含 SKILL.md
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
              <span class="text-sm">点击或拖拽整个技能文件夹到此处</span>
              <span class="text-[11px] text-muted-foreground">
                将保留目录结构上传，根目录需包含 SKILL.md
              </span>
            </div>
          </NUploadDragger>
        </NUpload>

        <div class="flex items-center justify-between gap-2">
          <span class="text-xs text-muted-foreground">
            已选择 {{ uploadMode === 'zip' ? zipFileList.length : folderFileList.length }} 个文件
          </span>
          <NButton
            :disabled="!uploadReady"
            :loading="uploading"
            type="primary"
            @click="handleUpload"
          >
            开始上传
          </NButton>
        </div>
      </div>
    </NModal>
  </Page>
</template>
