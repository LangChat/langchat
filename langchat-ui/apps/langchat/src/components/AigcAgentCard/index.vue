<script lang="ts" setup>
import type { AigcAgent } from '#/api/aigc/agent';
import type { LabelOption } from '#/views/shared/aigc/options';

import { computed } from 'vue';

import LcCard from '#/components/LcCard/index.vue';
import LcStatusTag from '#/components/LcStatusTag/index.vue';
import { resolveOptionLabels } from '#/views/shared/aigc/id-list';
import {
  AGENT_STATUS_OPTIONS,
  findOptionLabel,
} from '#/views/shared/aigc/options';

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
  const kText = knowledgeLabels.value.slice(0, 2).join('、') || '未配置知识库';
  const sText = skillLabels.value.slice(0, 2).join('、') || '未配置技能';
  return `知识库 ${kText}，技能 ${sText}。`;
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
  return props.item.modelConfigJson ? '已配置' : '未配置';
});

const metaItems = computed(() => [
  { label: '模型', value: modelName.value },
  {
    label: '知识库',
    value:
      knowledgeLabels.value.length > 0
        ? `${knowledgeLabels.value.length} 个`
        : '未配置',
  },
  {
    label: '技能',
    value:
      skillLabels.value.length > 0
        ? `${skillLabels.value.length} 个`
        : '未配置',
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

function formatTime(timestamp?: number) {
  if (!timestamp) {
    return '刚刚';
  }
  return new Date(timestamp).toLocaleString('zh-CN', { hour12: false });
}
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
          {{ item.agentName || '未命名应用' }}
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
        :label="findOptionLabel(AGENT_STATUS_OPTIONS, item.status)"
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
        {{ formatTime(item.updateTime) }}
      </div>
    </template>

    <template #footer>
      <slot name="footer"></slot>
    </template>
  </LcCard>
</template>
