<script setup lang="ts">
import type { LcFileTreeNode } from './types';

import { ref, watch } from 'vue';

import LcFileTreeNodeItem from './file-tree-node.vue';

interface Props {
  defaultExpandedPaths?: string[];
  nodes?: LcFileTreeNode[];
  selectedPath?: string;
}

const props = withDefaults(defineProps<Props>(), {
  defaultExpandedPaths: () => [],
  nodes: () => [],
  selectedPath: '',
});

const emit = defineEmits<{
  select: [path: string];
}>();

const expandedPaths = ref(new Set(props.defaultExpandedPaths));

function togglePath(path: string) {
  const next = new Set(expandedPaths.value);
  if (next.has(path)) {
    next.delete(path);
  } else {
    next.add(path);
  }
  expandedPaths.value = next;
}

watch(
  () => props.defaultExpandedPaths,
  (paths) => {
    expandedPaths.value = new Set(paths);
  },
);
</script>

<template>
  <div
    class="h-full min-h-0 overflow-auto rounded-lg border border-border bg-background py-1.5 font-mono"
  >
    <ul v-if="nodes.length > 0" class="min-w-max px-1" role="tree">
      <LcFileTreeNodeItem
        v-for="node in nodes"
        :key="node.path"
        :expanded-paths="expandedPaths"
        :node="node"
        :selected-path="selectedPath"
        @select="emit('select', $event)"
        @toggle="togglePath"
      />
    </ul>
    <div
      v-else
      class="flex h-full min-h-40 items-center justify-center px-4 text-xs text-muted-foreground"
    >
      {{ $t('components.fileTree.empty') }}
    </div>
  </div>
</template>
