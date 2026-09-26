<script lang="ts" setup>
import { ref } from 'vue';

import {
  BrainCircuit,
  ChevronDown,
  ChevronUp,
  Database,
  PlugZap,
  Plus,
  Wrench,
} from '@vben/icons';

import ModelSelector from '#/components/ModelSelector/index.vue';
import RelationCardPicker from '#/components/RelationCardPicker/index.vue';

interface RelationOption {
  description?: string;
  label: string;
  metrics?: string[];
  tags?: string[];
  value: string;
}

interface Props {
  knowledgeRelationOptions: RelationOption[];
  mcpRelationOptions: RelationOption[];
  modelEntities: any[];
  skillRelationOptions: RelationOption[];
}

defineProps<Props>();

const formModel = defineModel<any>({ required: true });
const expandedGroups = ref<string[]>([
  'model',
  'knowledge-relation',
  'skill-relation',
  'mcp-relation',
]);

const knowledgePickerRef = ref<InstanceType<typeof RelationCardPicker> | null>(
  null,
);
const skillPickerRef = ref<InstanceType<typeof RelationCardPicker> | null>(null);
const mcpPickerRef = ref<InstanceType<typeof RelationCardPicker> | null>(null);

function isExpanded(name: string) {
  return expandedGroups.value.includes(name);
}

function toggleGroup(name: string) {
  if (isExpanded(name)) {
    expandedGroups.value = expandedGroups.value.filter((item) => item !== name);
    return;
  }
  expandedGroups.value = [...expandedGroups.value, name];
}
</script>

<template>
  <div class="min-h-0 flex-1 space-y-2 overflow-y-auto pr-1">
    <section class="rounded-lg border border-border/70 bg-card">
      <button
        class="flex w-full items-center justify-between rounded-lg px-2.5 py-2 text-left transition-colors hover:bg-muted/60"
        type="button"
        @click="toggleGroup('model')"
      >
        <span
          class="inline-flex items-center gap-1.5 text-xs font-semibold text-foreground"
        >
          <BrainCircuit class="size-3.5 text-primary" />
          模型信息配置
        </span>
        <ChevronUp
          v-if="isExpanded('model')"
          class="size-3.5 text-muted-foreground"
        />
        <ChevronDown v-else class="size-3.5 text-muted-foreground" />
      </button>
      <div v-if="isExpanded('model')" class="border-t border-border/60 p-2.5">
        <ModelSelector
          :allowed-types="['REASONING', 'CHAT']"
          :config="formModel.modelConfig"
          :model-entities="modelEntities"
          :model-id="formModel.reasoningModelId"
          @update:config="formModel.modelConfig = $event"
          @update:model-id="formModel.reasoningModelId = $event"
        />
      </div>
    </section>

    <!-- 预留插槽:允许使用方在模型分组之后插入同规格的折叠分组 -->
    <slot name="after-model"></slot>

    <section class="rounded-lg border border-border/70 bg-card">
      <div
        class="flex items-center justify-between gap-1 rounded-lg py-1 pl-2.5 pr-1.5 transition-colors hover:bg-muted/60"
      >
        <button
          class="flex min-w-0 flex-1 cursor-pointer items-center gap-1.5 py-0.5 text-left"
          type="button"
          @click="toggleGroup('knowledge-relation')"
        >
          <Database class="size-3.5 shrink-0 text-primary" />
          <span class="truncate text-xs font-medium text-foreground">
            知识库关联
          </span>
          <span
            class="shrink-0 rounded bg-muted px-1 text-[10px] leading-4 text-muted-foreground"
          >
            {{ formModel.knowledgeIdsList.length }}
          </span>
        </button>
        <div class="flex shrink-0 items-center gap-0.5">
          <button
            v-tippy="'添加知识库'"
            class="flex cursor-pointer items-center gap-0.5 rounded p-1 text-[10px] text-muted-foreground transition-colors hover:bg-primary/10 hover:text-primary"
            type="button"
            @click="knowledgePickerRef?.open()"
          >
            <Plus class="size-3" />
            添加
          </button>
          <button
            class="cursor-pointer rounded p-1 text-muted-foreground transition-colors hover:text-foreground"
            type="button"
            @click="toggleGroup('knowledge-relation')"
          >
            <ChevronUp
              v-if="isExpanded('knowledge-relation')"
              class="size-3.5"
            />
            <ChevronDown v-else class="size-3.5" />
          </button>
        </div>
      </div>
      <div
        v-if="isExpanded('knowledge-relation')"
        class="border-t border-border/60 px-2 py-2"
      >
        <div class="mb-1.5 text-[10px] leading-4 text-muted-foreground">
          关联用于检索增强的知识来源，支持多选。
        </div>
        <RelationCardPicker
          ref="knowledgePickerRef"
          v-model="formModel.knowledgeIdsList"
          compact
          :icon="Database"
          modal-title="选择知识库"
          :options="knowledgeRelationOptions"
          placeholder="请选择关联知识库"
          title="知识库关联"
        />
      </div>
    </section>

    <section class="rounded-lg border border-border/70 bg-card">
      <div
        class="flex items-center justify-between gap-1 rounded-lg py-1 pl-2.5 pr-1.5 transition-colors hover:bg-muted/60"
      >
        <button
          class="flex min-w-0 flex-1 cursor-pointer items-center gap-1.5 py-0.5 text-left"
          type="button"
          @click="toggleGroup('skill-relation')"
        >
          <Wrench class="size-3.5 shrink-0 text-primary" />
          <span class="truncate text-xs font-medium text-foreground">
            技能关联
          </span>
          <span
            class="shrink-0 rounded bg-muted px-1 text-[10px] leading-4 text-muted-foreground"
          >
            {{ formModel.skillIdsList.length }}
          </span>
        </button>
        <div class="flex shrink-0 items-center gap-0.5">
          <button
            v-tippy="'添加技能'"
            class="flex cursor-pointer items-center gap-0.5 rounded p-1 text-[10px] text-muted-foreground transition-colors hover:bg-primary/10 hover:text-primary"
            type="button"
            @click="skillPickerRef?.open()"
          >
            <Plus class="size-3" />
            添加
          </button>
          <button
            class="cursor-pointer rounded p-1 text-muted-foreground transition-colors hover:text-foreground"
            type="button"
            @click="toggleGroup('skill-relation')"
          >
            <ChevronUp v-if="isExpanded('skill-relation')" class="size-3.5" />
            <ChevronDown v-else class="size-3.5" />
          </button>
        </div>
      </div>
      <div
        v-if="isExpanded('skill-relation')"
        class="border-t border-border/60 px-2 py-2"
      >
        <div class="mb-1.5 text-[10px] leading-4 text-muted-foreground">
          关联可调用的工具技能，支持多选。
        </div>
        <RelationCardPicker
          ref="skillPickerRef"
          v-model="formModel.skillIdsList"
          compact
          :icon="Wrench"
          modal-title="选择技能"
          :options="skillRelationOptions"
          placeholder="请选择关联技能"
          title="技能关联"
        />
      </div>
    </section>

    <section class="rounded-lg border border-border/70 bg-card">
      <div
        class="flex items-center justify-between gap-1 rounded-lg py-1 pl-2.5 pr-1.5 transition-colors hover:bg-muted/60"
      >
        <button
          class="flex min-w-0 flex-1 cursor-pointer items-center gap-1.5 py-0.5 text-left"
          type="button"
          @click="toggleGroup('mcp-relation')"
        >
          <PlugZap class="size-3.5 shrink-0 text-primary" />
          <span class="truncate text-xs font-medium text-foreground">
            MCP 关联
          </span>
          <span
            class="shrink-0 rounded bg-muted px-1 text-[10px] leading-4 text-muted-foreground"
          >
            {{ formModel.mcpIdsList.length }}
          </span>
        </button>
        <div class="flex shrink-0 items-center gap-0.5">
          <button
            v-tippy="'添加 MCP 服务'"
            class="flex cursor-pointer items-center gap-0.5 rounded p-1 text-[10px] text-muted-foreground transition-colors hover:bg-primary/10 hover:text-primary"
            type="button"
            @click="mcpPickerRef?.open()"
          >
            <Plus class="size-3" />
            添加
          </button>
          <button
            class="cursor-pointer rounded p-1 text-muted-foreground transition-colors hover:text-foreground"
            type="button"
            @click="toggleGroup('mcp-relation')"
          >
            <ChevronUp v-if="isExpanded('mcp-relation')" class="size-3.5" />
            <ChevronDown v-else class="size-3.5" />
          </button>
        </div>
      </div>
      <div
        v-if="isExpanded('mcp-relation')"
        class="border-t border-border/60 px-2 py-2"
      >
        <div class="mb-1.5 text-[10px] leading-4 text-muted-foreground">
          关联 MCP 服务，用于扩展外部工具能力，支持多选。
        </div>
        <RelationCardPicker
          ref="mcpPickerRef"
          v-model="formModel.mcpIdsList"
          compact
          :icon="PlugZap"
          modal-title="选择 MCP 服务"
          :options="mcpRelationOptions"
          placeholder="请选择关联 MCP 服务"
          title="MCP 关联"
        />
      </div>
    </section>
  </div>
</template>
