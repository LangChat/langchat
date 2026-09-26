<script lang="ts" setup>
import type { AigcSkill, SkillFileNode } from '#/api/aigc/skill';

import { computed, ref, watch } from 'vue';

import { useVbenDrawer } from '@vben/common-ui';
import { Save } from '@vben/icons';

import { NButton, NEmpty, NSpin, NTag } from 'naive-ui';

import { message } from '#/adapter/naive';
import { skillApi } from '#/api/aigc/skill';
import LcCodeEditor from '#/components/LcCodeEditor/index.vue';
import LcFileTree from '#/components/LcFileTree/index.vue';

interface Props {
  show: boolean;
  skill?: null | Partial<AigcSkill>;
}

const props = withDefaults(defineProps<Props>(), {
  skill: null,
});

const emit = defineEmits<{
  saved: [skill: AigcSkill];
  'update:show': [value: boolean];
}>();

const loading = ref(false);
const saving = ref(false);
const fileNodes = ref<SkillFileNode[]>([]);
const activePath = ref('');
const activeContent = ref('');
const activeBinary = ref(false);
const dirty = ref(false);

const currentLanguage = computed(() => resolveLanguage(activePath.value));
const defaultExpandedPaths = computed(() =>
  collectDirectoryPaths(fileNodes.value),
);

function resolveLanguage(path: string) {
  const extension = path.split('.').pop()?.toLowerCase() ?? '';
  if (['cjs', 'js', 'mjs'].includes(extension)) {
    return 'javascript';
  }
  if (['jsx', 'ts', 'tsx'].includes(extension)) {
    return 'typescript';
  }
  if (['json'].includes(extension)) {
    return 'json';
  }
  if (['markdown', 'md'].includes(extension)) {
    return 'markdown';
  }
  if (['yaml', 'yml'].includes(extension)) {
    return 'yaml';
  }
  if (['htm', 'html'].includes(extension)) {
    return 'html';
  }
  if (['css'].includes(extension)) {
    return 'css';
  }
  if (['sql'].includes(extension)) {
    return 'sql';
  }
  if (['py', 'pyw'].includes(extension)) {
    return 'python';
  }
  return 'text';
}

function collectDirectoryPaths(nodes: SkillFileNode[]): string[] {
  return nodes.flatMap((node) =>
    node.directory ? [node.path, ...collectDirectoryPaths(node.children)] : [],
  );
}

async function loadFiles(selectEntry = false) {
  if (!props.skill?.id) {
    return;
  }
  loading.value = true;
  try {
    fileNodes.value = await skillApi.listFiles(props.skill.id);
    if (selectEntry) {
      const entry = props.skill.entryFile || 'SKILL.md';
      const firstLeaf = findFirstLeaf(fileNodes.value);
      activePath.value = findPath(fileNodes.value, entry)
        ? entry
        : (firstLeaf ?? '');
      if (activePath.value) {
        await loadFileContent(activePath.value);
      }
    }
  } finally {
    loading.value = false;
  }
}

function findPath(nodes: SkillFileNode[], path: string): boolean {
  for (const node of nodes) {
    if (!node.directory && node.path === path) {
      return true;
    }
    if (node.directory && findPath(node.children, path)) {
      return true;
    }
  }
  return false;
}

function findFirstLeaf(nodes: SkillFileNode[]): string {
  for (const node of nodes) {
    if (!node.directory) {
      return node.path;
    }
    const childPath = findFirstLeaf(node.children);
    if (childPath) {
      return childPath;
    }
  }
  return '';
}

async function loadFileContent(path: string) {
  if (!props.skill?.id || !path) {
    return;
  }
  try {
    const content = await skillApi.readFile(props.skill.id, path);
    activePath.value = path;
    activeBinary.value = content.binary;
    activeContent.value = content.content ?? '';
    dirty.value = false;
  } catch (error) {
    message.error(`文件读取失败：${(error as Error)?.message || path}`);
  }
}

async function handleSelect(path: string) {
  if (!path || path === activePath.value) {
    return;
  }
  await loadFileContent(path);
}

async function handleSave() {
  if (!props.skill?.id || !activePath.value || activeBinary.value) {
    return;
  }
  saving.value = true;
  try {
    const updated = await skillApi.saveFile(
      props.skill.id,
      activePath.value,
      activeContent.value,
    );
    message.success('文档已保存，frontmatter 元数据已同步');
    dirty.value = false;
    emit('saved', updated);
  } finally {
    saving.value = false;
  }
}

function handleClose() {
  emit('update:show', false);
}

const [Drawer, drawerApi] = useVbenDrawer({
  class: 'w-[960px]',
  closable: true,
  contentClass: 'min-h-0 overflow-hidden p-0',
  footer: false,
  onClosed: handleClose,
  title: '编辑技能文档',
});

watch(
  () => props.show,
  async (value) => {
    if (!value) {
      drawerApi.close();
      return;
    }
    drawerApi.open();
    activePath.value = '';
    activeContent.value = '';
    activeBinary.value = false;
    dirty.value = false;
    await loadFiles(true);
  },
  { immediate: true },
);
</script>

<template>
  <Drawer>
    <div class="flex h-full min-h-0 flex-col gap-2 p-3">
      <div
        class="flex shrink-0 flex-wrap items-center gap-1.5 text-xs text-muted-foreground"
      >
        <NTag round size="small" type="primary">
          {{ skill?.title || skill?.name || '未命名技能' }}
        </NTag>
        <NTag round size="small">v{{ skill?.version || '0.0.1' }}</NTag>
        <span>{{ skill?.description || '未填写技能描述' }}</span>
      </div>

      <NSpin
        :show="loading"
        class="min-h-0 flex-1"
        content-class="h-full min-h-0"
      >
        <div
          class="grid h-full min-h-0 gap-2.5 lg:grid-cols-[240px_minmax(0,1fr)]"
        >
          <aside class="flex min-h-0 flex-col gap-1.5">
            <div
              class="shrink-0 px-1 text-[11px] font-medium text-muted-foreground"
            >
              技能包文件
            </div>
            <LcFileTree
              class="min-h-0 flex-1"
              :default-expanded-paths="defaultExpandedPaths"
              :nodes="fileNodes"
              :selected-path="activePath"
              @select="handleSelect"
            />
          </aside>

          <section class="flex min-h-0 min-w-0 flex-col gap-2">
            <div class="flex shrink-0 items-center justify-between gap-2">
              <span
                class="min-w-0 truncate rounded-md bg-muted px-2 py-1 font-mono text-[11px] text-foreground"
              >
                {{ activePath || '未选择文件' }}
              </span>
              <NButton
                :disabled="!activePath || activeBinary || !dirty"
                :loading="saving"
                size="small"
                type="primary"
                @click="handleSave"
              >
                <template #icon>
                  <Save class="size-3.5" />
                </template>
                保存
              </NButton>
            </div>

            <template v-if="activeBinary">
              <NEmpty
                class="min-h-0 flex-1 justify-center rounded-lg border border-dashed border-border bg-muted/20"
                description="二进制文件不支持在线编辑"
              />
            </template>
            <template v-else-if="activePath">
              <LcCodeEditor
                v-model:value="activeContent"
                class="min-h-0 flex-1"
                height="100%"
                :language="currentLanguage"
                min-height="0"
                placeholder="输入文件内容..."
                @update:value="dirty = true"
              />
            </template>
            <template v-else>
              <NEmpty
                class="min-h-0 flex-1 justify-center rounded-lg border border-dashed border-border bg-muted/20"
                description="从左侧选择要编辑的文件"
              />
            </template>
          </section>
        </div>
      </NSpin>
    </div>
  </Drawer>
</template>
