<script setup lang="ts">
import type { AgentChatRagEvent } from './types';

import { computed } from 'vue';
import { DatabaseZap, FileText } from '@vben/icons';

interface Props {
  item: AgentChatRagEvent;
}

const props = defineProps<Props>();

const title = computed(() => `知识检索命中 ${props.item.items.length} 条`);
</script>

<template>
  <div class="rounded-md border border-border/70 bg-muted/20 p-1.5">
    <div class="flex items-center gap-1.5 text-xs text-foreground">
      <DatabaseZap class="size-3 text-primary" />
      <span class="font-medium">{{ title }}</span>
    </div>

    <div v-if="item.items.length > 0" class="mt-1 flex flex-wrap gap-1">
      <span
        v-for="entry in item.items.slice(0, 6)"
        :key="`${entry.segmentId || entry.docsId || entry.docsName || ''}-${entry.score || ''}`"
        class="inline-flex max-w-full items-center gap-1 rounded border border-border bg-background px-1.5 py-0.5 text-[10px] text-muted-foreground"
      >
        <FileText class="size-3 text-muted-foreground" />
        <span class="max-w-[190px] truncate">{{
          entry.docsName || entry.knowledgeName || '未命名文档'
        }}</span>
        <span v-if="entry.score !== undefined" class="text-[10px] text-primary"
          >score {{ Number(entry.score).toFixed(2) }}</span
        >
      </span>
    </div>
  </div>
</template>
