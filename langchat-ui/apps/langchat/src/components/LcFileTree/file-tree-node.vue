<script setup lang="ts">
import type { Component } from 'vue';

import type { LcFileTreeNode } from './types';

import { computed } from 'vue';

import {
  ChevronRight,
  File,
  FileCode,
  FileJson,
  FileText,
  Folder,
  FolderOpen,
} from '@vben/icons';

interface Props {
  depth?: number;
  expandedPaths: Set<string>;
  node: LcFileTreeNode;
  selectedPath?: string;
}

defineOptions({ name: 'LcFileTreeNode' });

const props = withDefaults(defineProps<Props>(), {
  depth: 0,
  selectedPath: '',
});

const emit = defineEmits<{
  select: [path: string];
  toggle: [path: string];
}>();

const isExpanded = computed(() => props.expandedPaths.has(props.node.path));
const isSelected = computed(() => props.selectedPath === props.node.path);
const rowStyle = computed(() => ({
  paddingInlineStart: `${8 + props.depth * 16}px`,
}));
const fileIcon = computed<Component>(() => {
  const extension = props.node.name.split('.').pop()?.toLowerCase() ?? '';
  if (['json', 'jsonc'].includes(extension)) {
    return FileJson;
  }
  if (
    [
      'bash',
      'c',
      'cc',
      'cpp',
      'css',
      'go',
      'html',
      'java',
      'js',
      'jsx',
      'kt',
      'mjs',
      'py',
      'rs',
      'sh',
      'sql',
      'ts',
      'tsx',
      'vue',
      'xml',
    ].includes(extension)
  ) {
    return FileCode;
  }
  if (
    [
      'cfg',
      'conf',
      'env',
      'ini',
      'markdown',
      'md',
      'properties',
      'toml',
      'txt',
      'yaml',
      'yml',
    ].includes(extension)
  ) {
    return FileText;
  }
  return File;
});

function handleActivate() {
  if (props.node.directory) {
    emit('toggle', props.node.path);
    return;
  }
  emit('select', props.node.path);
}
</script>

<template>
  <li role="none">
    <button
      :aria-expanded="node.directory ? isExpanded : undefined"
      :aria-selected="!node.directory ? isSelected : undefined"
      class="group flex h-7 w-full min-w-0 cursor-pointer items-center gap-1.5 rounded-md pe-2 text-start text-xs transition-colors hover:bg-muted/60 focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-primary"
      :class="isSelected ? 'bg-muted text-foreground' : 'text-foreground/80'"
      role="treeitem"
      :style="rowStyle"
      type="button"
      @click="handleActivate"
    >
      <ChevronRight
        v-if="node.directory"
        class="size-3.5 shrink-0 text-muted-foreground transition-transform duration-150"
        :class="isExpanded ? 'rotate-90' : ''"
      />
      <span v-else class="size-3.5 shrink-0"></span>

      <FolderOpen
        v-if="node.directory && isExpanded"
        class="size-4 shrink-0 text-blue-500"
      />
      <Folder
        v-else-if="node.directory"
        class="size-4 shrink-0 text-blue-500"
      />
      <component
        :is="fileIcon"
        v-else
        class="size-4 shrink-0 text-muted-foreground group-hover:text-foreground/75"
      />

      <span class="min-w-0 flex-1 truncate font-mono">{{ node.name }}</span>
    </button>

    <ul
      v-if="node.directory && isExpanded && node.children?.length"
      role="group"
    >
      <LcFileTreeNode
        v-for="child in node.children"
        :key="child.path"
        :depth="depth + 1"
        :expanded-paths="expandedPaths"
        :node="child"
        :selected-path="selectedPath"
        @select="emit('select', $event)"
        @toggle="emit('toggle', $event)"
      />
    </ul>
  </li>
</template>
