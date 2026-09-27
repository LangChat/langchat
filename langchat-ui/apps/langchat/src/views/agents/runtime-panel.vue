<script setup lang="ts">
import type { AigcAgent } from '#/api/aigc/agent';
import type { LabelOption } from '#/views/shared/aigc/options';

import { computed } from 'vue';

import { $t } from '@vben/locales';

import { NEmpty, NSelect, NTag } from 'naive-ui';

import { resolveOptionLabels } from '#/views/shared/aigc/id-list';
import {
  agentStatusOptions,
  findOptionLabel,
} from '#/views/shared/aigc/options';

interface Props {
  agent: AigcAgent | null;
  knowledgeOptions: LabelOption[];
  modelOptions: LabelOption[];
  options: Array<{ label: string; value: string }>;
  selectedAgentId?: string;
  skillOptions: LabelOption[];
}

const props = defineProps<Props>();

const emit = defineEmits<{
  builder: [];
  refresh: [];
  'update:selectedAgentId': [value: string];
}>();

const selectedKnowledgeLabels = computed(() =>
  resolveOptionLabels(props.knowledgeOptions, props.agent?.knowledgeIds),
);
const selectedSkillLabels = computed(() =>
  resolveOptionLabels(props.skillOptions, props.agent?.skillIds),
);

const modelConfigText = computed(() => {
  if (!props.agent?.modelConfigJson) {
    return $t('agents.runtimePanel.paramsEmpty');
  }
  try {
    return JSON.stringify(JSON.parse(props.agent.modelConfigJson), null, 2);
  } catch {
    return props.agent.modelConfigJson;
  }
});

function resolveModelLabel(modelId?: string) {
  return (
    props.modelOptions.find((item) => item.value === modelId)?.label ||
    $t('common.status.notConfigured')
  );
}
</script>

<template>
  <div class="rounded-xl border border-border bg-card p-3">
    <div class="flex items-start justify-between gap-3">
      <div>
        <div class="text-sm font-semibold text-foreground">
          {{ $t('agents.runtimePanel.title') }}
        </div>
        <div class="mt-1 text-xs leading-5 text-muted-foreground">
          {{ $t('agents.runtimePanel.description') }}
        </div>
      </div>
      <div class="flex items-center gap-2">
        <button
          v-if="agent"
          class="rounded-md border border-border px-2.5 py-1 text-xs text-muted-foreground transition-colors hover:border-primary/40 hover:bg-primary/5 hover:text-primary"
          type="button"
          @click="emit('builder')"
        >
          {{ $t('agents.runtimePanel.buildDebug') }}
        </button>
        <button
          class="rounded-md border border-border px-2.5 py-1 text-xs text-muted-foreground transition-colors hover:border-primary/40 hover:bg-primary/5 hover:text-primary"
          type="button"
          @click="emit('refresh')"
        >
          {{ $t('common.actions.refresh') }}
        </button>
      </div>
    </div>

    <div class="mt-3">
      <div
        class="mb-2 text-[10px] font-semibold uppercase tracking-[0.16em] text-muted-foreground"
      >
        {{ $t('agents.runtimePanel.currentAgent') }}
      </div>
      <NSelect
        :options="options"
        :value="selectedAgentId"
        clearable
        :placeholder="$t('agents.runtimePanel.agentPlaceholder')"
        @update:value="emit('update:selectedAgentId', $event || '')"
      />
    </div>

    <div v-if="agent" class="mt-4 space-y-4">
      <div class="rounded-lg border border-border bg-muted/40 p-3">
        <div class="flex items-start justify-between gap-3">
          <div class="truncate text-sm font-semibold text-foreground">
            {{ agent.agentName || $t('agents.runtimePanel.unnamed') }}
          </div>
          <NTag
            :bordered="false"
            :type="
              agent.status === 'PUBLISHED'
                ? 'success'
                : agent.status === 'DISABLED'
                  ? 'error'
                  : 'info'
            "
            round
          >
            {{ findOptionLabel(agentStatusOptions(), agent.status) }}
          </NTag>
        </div>
      </div>

      <div class="grid gap-3">
        <div class="rounded-lg border border-border bg-card p-3">
          <div class="text-xs font-medium text-muted-foreground">
            {{ $t('agents.runtimePanel.modelConfig') }}
          </div>
          <div class="mt-2 text-sm">
            <div class="text-[11px] text-muted-foreground">
              {{ $t('agents.runtimePanel.reasoningModel') }}
            </div>
            <div class="mt-1 truncate text-foreground">
              {{ resolveModelLabel(agent.reasoningModelId) }}
            </div>
          </div>
        </div>

        <div class="rounded-lg border border-border bg-card p-3">
          <div class="text-xs font-medium text-muted-foreground">
            {{ $t('agents.runtimePanel.boundKnowledge') }}
          </div>
          <div class="mt-2 flex flex-wrap gap-2">
            <NTag
              v-for="label in selectedKnowledgeLabels"
              :key="label"
              :bordered="false"
              round
              type="info"
            >
              {{ label }}
            </NTag>
            <span
              v-if="selectedKnowledgeLabels.length === 0"
              class="text-sm text-muted-foreground"
            >
              {{ $t('agents.runtimePanel.knowledgeEmpty') }}
            </span>
          </div>
        </div>

        <div class="rounded-lg border border-border bg-card p-3">
          <div class="text-xs font-medium text-muted-foreground">
            {{ $t('agents.runtimePanel.boundSkills') }}
          </div>
          <div class="mt-2 flex flex-wrap gap-2">
            <NTag
              v-for="label in selectedSkillLabels"
              :key="label"
              :bordered="false"
              round
              type="success"
            >
              {{ label }}
            </NTag>
            <span
              v-if="selectedSkillLabels.length === 0"
              class="text-sm text-muted-foreground"
            >
              {{ $t('agents.runtimePanel.skillsEmpty') }}
            </span>
          </div>
        </div>

        <div class="rounded-lg border border-border bg-card p-3">
          <div class="text-xs font-medium text-muted-foreground">
            {{ $t('agents.runtimePanel.systemPrompt') }}
          </div>
          <div
            class="mt-2 line-clamp-6 whitespace-pre-wrap text-sm leading-7 text-foreground/85"
          >
            {{ agent.systemPrompt || $t('agents.runtimePanel.systemPromptEmpty') }}
          </div>
        </div>

        <div class="rounded-lg border border-border bg-card p-3">
          <div class="text-xs font-medium text-muted-foreground">
            {{ $t('agents.runtimePanel.paramsOverride') }}
          </div>
          <pre
            class="mt-2 overflow-x-auto whitespace-pre-wrap text-xs leading-6 text-foreground/85"
            >{{ modelConfigText }}</pre>
        </div>
      </div>
    </div>

    <div v-else class="mt-4">
      <NEmpty :description="$t('agents.runtimePanel.empty')" />
    </div>
  </div>
</template>
