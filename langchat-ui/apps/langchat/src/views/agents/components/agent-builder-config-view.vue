<script lang="ts" setup>
import type {
  LcChatMessage,
  LcChatSendPayload,
} from '#/components/LcChat/types';

import { ref } from 'vue';

import {
  ChevronDown,
  ChevronUp,
  FileText,
  Maximize2,
  Minimize2,
} from '@vben/icons';

import { ResizableHandle, ResizablePanel, ResizablePanelGroup } from '@vben-core/shadcn-ui';

import { NDynamicInput, NInput, NSwitch } from 'naive-ui';

import LcChat from '#/components/LcChat/index.vue';
import LcIconDisplay from '#/components/LcIcon/display.vue';
import AgentConfigPanel from '#/views/agents/components/agent-config-panel.vue';

interface RelationOption {
  description?: string;
  label: string;
  metrics?: string[];
  tags?: string[];
  value: string;
}

interface Props {
  appIcon?: string;
  chatLoading?: boolean;
  defaultSuggestions: string[];
  historyLoading?: boolean;
  knowledgeRelationOptions: RelationOption[];
  mcpRelationOptions: RelationOption[];
  modelEntities: any[];
  skillRelationOptions: RelationOption[];
  userIcon?: string;
  welcomeMessage: string;
}

withDefaults(defineProps<Props>(), {
  appIcon: '',
  chatLoading: false,
  historyLoading: false,
  userIcon: '',
});

const emit = defineEmits<{
  submit: [payload: LcChatSendPayload];
}>();

const formModel = defineModel<any>({ required: true });

/** 首行合并折叠卡片(提示词与欢迎语)的展开状态 */
const promptExpanded = ref(true);

type BuilderPanelKey = 'chat' | 'config';

/** 当前页面内全屏的面板，null 表示两栏常规布局 */
const maximizedPanel = ref<BuilderPanelKey | null>(null);

function toggleMaximize(panel: BuilderPanelKey) {
  maximizedPanel.value = maximizedPanel.value === panel ? null : panel;
}

/** 聊天布局实例,由父级(builder)通过它驱动消息流 */
const layoutRef = ref<InstanceType<typeof LcChat> | null>(null);
/** 独立于 LcChat 实例保存消息，避免面板全屏切换重建组件后丢失历史。 */
const chatMessages = ref<LcChatMessage[]>([]);

defineExpose({ layoutRef });

</script>

<template>
  <ResizablePanelGroup
    :key="maximizedPanel ?? 'split'"
    class="overflow-hidden rounded-xl"
    direction="horizontal"
  >
    <!-- 配置列:应用配置 + 折叠分组(系统提示词 / 欢迎语与建议) -->
    <ResizablePanel
      v-if="!maximizedPanel || maximizedPanel === 'config'"
      :default-size="maximizedPanel ? 100 : 34"
      :min-size="maximizedPanel ? undefined : 24"
    >
      <div
        class="flex h-full min-h-0 flex-col rounded-lg border border-border/70 bg-card p-3"
      >
        <div class="mb-2 flex shrink-0 items-center justify-between gap-2">
          <div class="text-sm font-semibold text-foreground">应用配置</div>
          <button
            v-tippy="maximizedPanel === 'config' ? '还原' : '全屏'"
            class="flex size-7 cursor-pointer items-center justify-center rounded-md text-muted-foreground transition-colors hover:bg-primary/10 hover:text-primary"
            type="button"
            @click="toggleMaximize('config')"
          >
            <Minimize2 v-if="maximizedPanel === 'config'" class="size-4" />
            <Maximize2 v-else class="size-4" />
          </button>
        </div>

        <AgentConfigPanel
          v-model="formModel"
          :knowledge-relation-options="knowledgeRelationOptions"
          :mcp-relation-options="mcpRelationOptions"
          :model-entities="modelEntities"
          :skill-relation-options="skillRelationOptions"
        >
          <template #after-model>
            <section class="rounded-lg border border-border/70 bg-card">
              <button
                class="flex w-full items-center justify-between rounded-lg px-2.5 py-2 text-left transition-colors hover:bg-muted/60"
                type="button"
                @click="promptExpanded = !promptExpanded"
              >
                <span
                  class="inline-flex items-center gap-2 text-sm font-semibold text-foreground"
                >
                  <FileText class="size-4 text-primary" />
                  提示词与欢迎语配置
                </span>
                <ChevronUp
                  v-if="promptExpanded"
                  class="size-3.5 text-muted-foreground"
                />
                <ChevronDown v-else class="size-3.5 text-muted-foreground" />
              </button>
              <div
                v-if="promptExpanded"
                class="space-y-3 border-t border-border/60 p-2.5"
              >
                <div>
                  <div class="mb-1.5 text-xs font-medium text-foreground/80">
                    系统提示词
                  </div>
                  <NInput
                    v-model:value="formModel.systemPrompt"
                    :autosize="{ minRows: 6, maxRows: 14 }"
                    placeholder="请输入系统提示词"
                    type="textarea"
                  />
                </div>
                <div>
                  <div class="mb-1.5 text-xs font-medium text-foreground/80">
                    欢迎语
                  </div>
                  <NInput
                    v-model:value="formModel.welcomeMessage"
                    placeholder="当聊天为空时展示的欢迎语"
                  />
                </div>
                <div
                  class="rounded-lg border border-border/70 bg-background px-2.5 py-2"
                >
                  <div class="flex items-center justify-between gap-3">
                    <div>
                      <div class="text-xs font-semibold text-foreground">
                        自动建议
                      </div>
                      <div class="text-xs leading-5 text-muted-foreground">
                        开启后，每次 AI 回复后会显示推荐追问卡片
                      </div>
                    </div>
                    <NSwitch v-model:value="formModel.enableAutoSuggestion" />
                  </div>
                </div>
                <div>
                  <div class="mb-1.5 text-xs font-medium text-foreground/80">
                    默认建议问题
                  </div>
                  <NDynamicInput
                    v-model:value="formModel.defaultSuggestionsList"
                    :on-create="() => ''"
                  >
                    <template #default="{ index, value }">
                      <NInput
                        :placeholder="`建议问题 ${index + 1}`"
                        :value="value"
                        @update:value="
                          formModel.defaultSuggestionsList[index] = $event
                        "
                      />
                    </template>
                  </NDynamicInput>
                </div>
              </div>
            </section>
          </template>
        </AgentConfigPanel>
      </div>
    </ResizablePanel>

    <ResizableHandle
      v-if="!maximizedPanel"
      class="mx-px transition-colors hover:bg-primary/60"
      with-handle
    />

    <!-- 聊天调试列:尽量占据剩余宽度 -->
    <ResizablePanel
      v-if="!maximizedPanel || maximizedPanel === 'chat'"
      :default-size="maximizedPanel ? 100 : 66"
      :min-size="maximizedPanel ? undefined : 40"
    >
      <div class="flex h-full min-h-0 flex-col rounded-lg border border-border/70 bg-card p-3">
        <div class="mb-3 flex items-center justify-between gap-2">
          <div class="text-sm font-semibold text-foreground">聊天调试</div>
          <button
            v-tippy="maximizedPanel === 'chat' ? '还原' : '全屏'"
            class="flex size-7 cursor-pointer items-center justify-center rounded-md text-muted-foreground transition-colors hover:bg-primary/10 hover:text-primary"
            type="button"
            @click="toggleMaximize('chat')"
          >
            <Minimize2 v-if="maximizedPanel === 'chat'" class="size-4" />
            <Maximize2 v-else class="size-4" />
          </button>
        </div>
        <div class="min-h-0 flex-1">
          <LcChat
            ref="layoutRef"
            :assistant-icon="appIcon || ''"
            :disabled="chatLoading"
            :empty-title="welcomeMessage"
            :initial-messages="chatMessages"
            :loading="historyLoading"
            placeholder="输入调试消息并回车发送"
            :show-attachment="true"
            :show-model="false"
            :suggestions="defaultSuggestions.map((label) => ({ label }))"
            :user-avatar="userIcon || ''"
            @messages-change="chatMessages = $event"
            @send="emit('submit', $event)"
          >
            <template #empty-head>
              <div class="flex flex-col items-center gap-3 text-center">
                <LcIconDisplay
                  :icon="appIcon"
                  fallback-icon="lucide:bot"
                  :size="64"
                  class="!rounded-full border-border/80 bg-background"
                />
                <div class="text-base font-semibold text-foreground">
                  {{ welcomeMessage }}
                </div>
              </div>
            </template>
          </LcChat>
        </div>
      </div>
    </ResizablePanel>
  </ResizablePanelGroup>
</template>
