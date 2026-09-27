<script lang="ts" setup>
import type { AigcAgent } from '#/api/aigc/agent';
import type { LabelOption } from '#/views/shared/aigc/options';

import { computed } from 'vue';

import { $t } from '@vben/locales';

import LcCard from '#/components/LcCard/index.vue';
import LcStatusTag from '#/components/LcStatusTag/index.vue';
import { resolveOptionLabels } from '#/views/shared/aigc/id-list';
import {
  agentStatusOptions,
  findOptionLabel,
} from '#/views/shared/aigc/options';
import { formatRelativeTime } from '#/views/shared/aigc/time';

interface Props {
  item: AigcAgent;
  knowledgeOptions: LabelOption[];
  modelOptions: LabelOption[];
  skillOptions: LabelOption[];
}

const props = defineProps<Props>();

const emit = defineEmits<{
  click: [item: AigcAgent];
}>();

const knowledgeLabels = computed(() =>
  resolveOptionLabels(props.knowledgeOptions, props.item.knowledgeIds),
);
const skillLabels = computed(() =>
  resolveOptionLabels(props.skillOptions, props.item.skillIds),
);

const description = computed(() => {
  const kText =
    knowledgeLabels.value.slice(0, 2).join('、') ||
    $t('components.aigcAgentCard.noKnowledge');
  const sText =
    skillLabels.value.slice(0, 2).join('、') ||
    $t('components.aigcAgentCard.noSkills');
  return $t('components.aigcAgentCard.knowledgeAndSkills', {
    knowledge: kText,
    skills: sText,
  });
});

const modelName = computed(() => {
  try {
    const cfg = props.item.modelConfigJson
      ? JSON.parse(props.item.modelConfigJson)
      : null;
    if (cfg?.modelName) return cfg.modelName;
    if (cfg?.modelId) {
      const found = props.modelOptions.find((o) => o.value === cfg.modelId);
      if (found) return found.label;
    }
  } catch {
    // noop
  }
  return props.item.modelConfigJson
    ? $t('common.status.configured')
    : $t('common.status.notConfigured');
});

const metaItems = computed(() => [
  { label: $t('common.labels.model'), value: modelName.value },
  {
    label: $t('common.labels.knowledgeBase'),
    value:
      knowledgeLabels.value.length > 0
        ? $t('common.labels.count', { count: knowledgeLabels.value.length })
        : $t('common.status.notConfigured'),
  },
  {
    label: $t('common.labels.skill'),
    value:
      skillLabels.value.length > 0
        ? $t('common.labels.count', { count: skillLabels.value.length })
        : $t('common.status.notConfigured'),
  },
]);

const tags = computed(() =>
  String(props.item.tags || '')
    .split(/[,，]/)
    .map((tag) => tag.trim())
    .filter(Boolean),
);

const agentIcon = computed(() => {
  if (props.item.icon) {
    return props.item.icon;
  }
  try {
    const parsed = props.item.metaJson ? JSON.parse(props.item.metaJson) : null;
    if (parsed && typeof parsed === 'object' && parsed.icon) {
      return String(parsed.icon);
    }
  } catch {
    // noop
  }
  return props.item.avatar || 'lucide:box';
});

const statusType = computed(() =>
  props.item.status === 'PUBLISHED'
    ? 'success'
    : props.item.status === 'DISABLED'
      ? 'error'
      : 'info',
);
</script>

<template>
  <LcCard
    :icon="agentIcon"
    :meta-items="metaItems"
    class="h-full cursor-pointer"
    hoverable
    @click="emit('click', item)"
  >
    <template #header>
      <div class="min-w-0">
        <div
          class="truncate text-sm font-semibold text-foreground group-hover:text-primary"
        >
          {{ item.agentName || $t('components.aigcAgentCard.unnamedApp') }}
        </div>
      </div>
    </template>

    <template #description>
      <p class="text-[11px] text-muted-foreground line-clamp-2">
        {{ item.description || description }}
      </p>
    </template>

    <template #header-extra>
      <LcStatusTag
        :label="findOptionLabel(agentStatusOptions(), item.status)"
        :type="statusType"
      />
    </template>

    <div v-if="tags.length > 0" class="flex flex-wrap gap-1">
      <span
        v-for="tag in tags"
        :key="tag"
        class="rounded-md bg-muted/50 px-2 py-0.5 text-[10px] text-muted-foreground"
      >{{ tag }}</span>
    </div>

    <slot></slot>

    <template #footer-extra>
      <div class="truncate whitespace-nowrap text-[11px] text-muted-foreground">
        {{ formatRelativeTime(item.updateTime) }}
      </div>
    </template>

    <template #footer>
      <slot name="footer"></slot>
    </template>
  </LcCard>
</template>
