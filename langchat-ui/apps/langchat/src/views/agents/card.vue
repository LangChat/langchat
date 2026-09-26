<script lang="ts" setup>
import type { AigcAgent } from '#/api/aigc/agent';
import type { LabelOption } from '#/views/shared/aigc/options';

import { SquarePen, Trash2 } from '@vben/icons';

import { NButton, NSpace } from 'naive-ui';

import AigcAgentCard from '#/components/AigcAgentCard/index.vue';

interface Props {
  item: AigcAgent;
  knowledgeOptions: LabelOption[];
  modelOptions: LabelOption[];
  skillOptions: LabelOption[];
}

defineProps<Props>();

const emit = defineEmits<{
  builder: [item: AigcAgent];
  delete: [item: AigcAgent];
  edit: [item: AigcAgent];
}>();
</script>

<template>
  <AigcAgentCard
    :item="item"
    :knowledge-options="knowledgeOptions"
    :model-options="modelOptions"
    :skill-options="skillOptions"
    @click="emit('builder', $event)"
  >
    <template #footer>
      <NSpace :size="4">
        <NButton
          v-tippy="'编辑'"
          circle
          class="text-muted-foreground hover:text-primary"
          quaternary
          size="small"
          @click.stop="emit('edit', item)"
        >
          <SquarePen class="size-3.5" />
        </NButton>
        <NButton
          v-tippy="'删除'"
          circle
          class="text-muted-foreground hover:text-destructive"
          quaternary
          size="small"
          type="error"
          @click.stop="emit('delete', item)"
        >
          <Trash2 class="size-3.5" />
        </NButton>
      </NSpace>
    </template>
  </AigcAgentCard>
</template>
