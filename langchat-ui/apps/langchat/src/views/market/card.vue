<script lang="ts" setup>
import type { AigcAgent } from '#/api/aigc/agent';
import type { LabelOption } from '#/views/shared/aigc/options';

import { MessageCircle } from '@vben/icons';

import { NButton } from 'naive-ui';

import AigcAgentCard from '#/components/AigcAgentCard/index.vue';

interface Props {
  item: AigcAgent;
  knowledgeOptions: LabelOption[];
  modelOptions: LabelOption[];
  skillOptions: LabelOption[];
}

defineProps<Props>();

const emit = defineEmits<{
  enter: [item: AigcAgent];
}>();
</script>

<template>
  <AigcAgentCard
    :item="item"
    :knowledge-options="knowledgeOptions"
    :model-options="modelOptions"
    :skill-options="skillOptions"
    @click="emit('enter', $event)"
  >
    <template #footer>
      <NButton size="small" type="primary" @click.stop="emit('enter', item)">
        <template #icon>
          <MessageCircle class="size-3.5" />
        </template>
        {{ $t('market.enterApp') }}
      </NButton>
    </template>
  </AigcAgentCard>
</template>
