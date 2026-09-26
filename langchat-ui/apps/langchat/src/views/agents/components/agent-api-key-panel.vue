<script lang="ts" setup>
import type { AigcAgentApiKey } from '#/api/aigc/agent-api-key';
import type { AigcMessage } from '#/api/aigc/chat';

import { computed, ref, watch } from 'vue';

import { useVbenDrawer, useVbenModal } from '@vben/common-ui';
import { Copy, KeyRound, Plus, Trash2 } from '@vben/icons';

import {
  NButton,
  NEmpty,
  NForm,
  NFormItem,
  NInput,
  NSwitch,
  NTag,
} from 'naive-ui';

import { dialog, message } from '#/adapter/naive';
import {
  createAgentApiKeyApi,
  listAgentApiKeyMessagesApi,
  listAgentApiKeysApi,
  OPENAI_COMPATIBLE_PATH,
  removeAgentApiKeyApi,
  updateAgentApiKeyApi,
} from '#/api/aigc/agent-api-key';
import { formatDocsTimestamp } from '#/views/shared/aigc/docs-status';

interface Props {
  agentId?: string;
}

const props = withDefaults(defineProps<Props>(), {
  agentId: '',
});

const loading = ref(false);
const submitting = ref(false);
const items = ref<AigcAgentApiKey[]>([]);
const createForm = ref({ name: '', remark: '' });
const activeLogKey = ref<AigcAgentApiKey | null>(null);
const logLoading = ref(false);
const logMessages = ref<AigcMessage[]>([]);

const hasAgent = computed(() => Boolean(props.agentId));

const endpointExample = computed(() => {
  const origin = window.location.origin;
  return `curl -X POST "${origin}${OPENAI_COMPATIBLE_PATH}" \\\n  -H "Authorization: Bearer sk-xxxx" \\\n  -H "Content-Type: application/json" \\\n  -d '{"model":"agent","messages":[{"role":"user","content":"你好"}]}'`;
});

const [CreateModal, createModalApi] = useVbenModal({
  class: 'w-[480px]',
  confirmText: '创建',
  title: '新建 API Key',
  onConfirm: async () => {
    await handleCreate();
  },
});

const [LogDrawer, logDrawerApi] = useVbenDrawer({
  class: 'w-[640px]',
  footer: false,
  title: '调用日志',
});

watch(
  () => props.agentId,
  () => {
    void loadKeys();
  },
  { immediate: true },
);

async function loadKeys() {
  if (!hasAgent.value) {
    items.value = [];
    return;
  }
  loading.value = true;
  try {
    items.value = await listAgentApiKeysApi(props.agentId);
  } finally {
    loading.value = false;
  }
}

function openCreate() {
  createForm.value = { name: '', remark: '' };
  createModalApi.open();
}

async function handleCreate() {
  if (!createForm.value.name.trim()) {
    message.warning('请填写密钥名称');
    return;
  }
  submitting.value = true;
  try {
    await createAgentApiKeyApi(props.agentId, { ...createForm.value });
    message.success('API Key 已创建');
    createModalApi.close();
    await loadKeys();
  } finally {
    submitting.value = false;
  }
}

async function handleStatusChange(row: AigcAgentApiKey, checked: boolean) {
  try {
    await updateAgentApiKeyApi(row.id!, { status: checked ? 'ENABLED' : 'DISABLED' });
    row.status = checked ? 'ENABLED' : 'DISABLED';
    message.success(checked ? '已启用' : '已停用');
  } catch {
    row.status = checked ? 'DISABLED' : 'ENABLED';
  }
}

async function handleCopy(text?: string, tip = '已复制到剪贴板') {
  if (!text) {
    return;
  }
  try {
    await navigator.clipboard.writeText(text);
    message.success(tip);
  } catch {
    message.error('复制失败，请手动复制');
  }
}

function handleRemove(row: AigcAgentApiKey) {
  dialog.warning({
    closable: false,
    content: `删除后使用该密钥的外部调用将立即失效，确认删除「${row.name || '未命名密钥'}」吗？`,
    negativeText: '取消',
    positiveText: '确认删除',
    title: '删除 API Key',
    onPositiveClick: async () => {
      await removeAgentApiKeyApi(row.id!);
      message.success('API Key 已删除');
      await loadKeys();
    },
  });
}

async function openLogs(row: AigcAgentApiKey) {
  activeLogKey.value = row;
  logMessages.value = [];
  logDrawerApi.setState({
    title: `调用日志 · ${row.name || 'API Key'}`,
  });
  logDrawerApi.open();
  logLoading.value = true;
  try {
    logMessages.value = await listAgentApiKeyMessagesApi(row.id!);
  } finally {
    logLoading.value = false;
  }
}

function maskKey(key?: string) {
  if (!key) {
    return '--';
  }
  if (key.length <= 12) {
    return key;
  }
  return `${key.slice(0, 10)}****${key.slice(-4)}`;
}

function resolveRoleTone(role?: string) {
  if (role === 'assistant') {
    return 'success' as const;
  }
  if (role === 'user') {
    return 'info' as const;
  }
  return 'default' as const;
}
</script>

<template>
  <div class="flex h-full min-h-0 flex-col gap-3 overflow-y-auto rounded-xl border border-border bg-card p-4">
    <div class="flex flex-wrap items-center justify-between gap-2">
      <div>
        <div class="text-sm font-semibold text-foreground">API 接入</div>
        <div class="mt-1 text-xs text-muted-foreground">
          通过 OpenAI 兼容接口将当前 Agent 应用对外提供服务，按密钥管理启停与统计。
        </div>
      </div>
      <NButton :disabled="!hasAgent" type="primary" @click="openCreate">
        <template #icon>
          <Plus class="size-4" />
        </template>
        新建 API Key
      </NButton>
    </div>

    <div
      v-if="!hasAgent"
      class="rounded-lg border border-dashed border-border px-6 py-12 text-center text-sm text-muted-foreground"
    >
      请先保存当前 Agent 应用，再创建 API Key。
    </div>

    <template v-else>
      <div class="rounded-lg border border-border/70 bg-muted/20 p-3">
        <div class="mb-2 flex items-center gap-2">
          <KeyRound class="size-3.5 text-primary" />
          <span class="text-xs font-semibold text-foreground">
            OpenAI 兼容接口
          </span>
          <NTag :bordered="false" size="small" type="info">POST</NTag>
          <code class="rounded bg-background px-2 py-0.5 text-[11px] text-foreground">
            {{ OPENAI_COMPATIBLE_PATH }}
          </code>
        </div>
        <pre
          class="overflow-x-auto rounded-md border border-border/70 bg-background p-2.5 text-[11px] leading-5 text-muted-foreground"
          >{{ endpointExample }}</pre
        >
        <div class="mt-2 flex items-center justify-end">
          <NButton
            quaternary
            size="small"
            type="primary"
            @click="handleCopy(endpointExample, '调用示例已复制')"
          >
            <template #icon>
              <Copy class="size-3.5" />
            </template>
            复制示例
          </NButton>
        </div>
      </div>

      <div v-if="loading" class="py-10 text-center text-sm text-muted-foreground">
        正在加载密钥列表...
      </div>

      <div v-else-if="items.length === 0" class="py-6">
        <NEmpty description="还没有创建 API Key，点击右上角新建。">
          <template #extra>
            <NButton size="small" type="primary" @click="openCreate">
              新建 API Key
            </NButton>
          </template>
        </NEmpty>
      </div>

      <div v-else class="space-y-2.5">
        <div
          v-for="row in items"
          :key="row.id"
          class="rounded-lg border border-border/70 bg-background/60 p-3"
        >
          <div class="flex flex-wrap items-start justify-between gap-3">
            <div class="min-w-0">
              <div class="flex items-center gap-2">
                <span class="text-sm font-medium text-foreground">
                  {{ row.name || '未命名密钥' }}
                </span>
                <NTag
                  :bordered="false"
                  round
                  size="small"
                  :type="row.status === 'ENABLED' ? 'success' : 'default'"
                >
                  {{ row.status === 'ENABLED' ? '已启用' : '已停用' }}
                </NTag>
              </div>
              <div class="mt-1 flex items-center gap-2">
                <code
                  class="max-w-[280px] truncate rounded bg-muted/40 px-2 py-0.5 text-[11px] text-muted-foreground"
                >
                  {{ maskKey(row.apiKey) }}
                </code>
                <NButton
                  v-tippy="'复制完整密钥'"
                  quaternary
                  size="tiny"
                  @click="handleCopy(row.apiKey, 'API Key 已复制')"
                >
                  <template #icon>
                    <Copy class="size-3.5" />
                  </template>
                </NButton>
              </div>
              <div
                v-if="row.remark"
                class="mt-1 text-[11px] text-muted-foreground"
              >
                {{ row.remark }}
              </div>
            </div>

            <div class="flex items-center gap-2">
              <div
                class="mr-2 flex items-center gap-3 text-[11px] text-muted-foreground"
              >
                <span>调用 {{ row.callCount ?? 0 }}</span>
                <span>输入 {{ row.inputTokens ?? 0 }} tokens</span>
                <span>输出 {{ row.outputTokens ?? 0 }} tokens</span>
                <span>最后调用 {{ formatDocsTimestamp(row.lastCallTime) }}</span>
              </div>
              <NButton quaternary size="small" @click="openLogs(row)">
                调用日志
              </NButton>
              <div class="flex items-center gap-1.5">
                <span class="text-[11px] text-muted-foreground">启用</span>
                <NSwitch
                  :value="row.status === 'ENABLED'"
                  size="small"
                  @update:value="(checked: boolean) => handleStatusChange(row, checked)"
                />
              </div>
              <NButton
                v-tippy="'删除密钥'"
                quaternary
                size="small"
                type="error"
                @click="handleRemove(row)"
              >
                <template #icon>
                  <Trash2 class="size-3.5" />
                </template>
              </NButton>
            </div>
          </div>
        </div>
      </div>
    </template>

    <CreateModal>
      <NForm label-placement="top">
        <NFormItem label="密钥名称" required>
          <NInput
            v-model:value="createForm.name"
            :maxlength="50"
            placeholder="例如：客服系统对接"
            show-count
          />
        </NFormItem>
        <NFormItem label="备注">
          <NInput
            v-model:value="createForm.remark"
            :maxlength="200"
            placeholder="记录该密钥的用途，可选"
            type="textarea"
          />
        </NFormItem>
        <div class="rounded-md bg-muted/30 px-3 py-2 text-[11px] leading-5 text-muted-foreground">
          创建后请立即保存生成的密钥，外部系统将通过
          Authorization: Bearer &lt;密钥&gt; 调用 OpenAI 兼容接口。
        </div>
      </NForm>
    </CreateModal>

    <LogDrawer>
      <div class="space-y-2">
        <div v-if="logLoading" class="py-10 text-center text-sm text-muted-foreground">
          正在加载调用日志...
        </div>
        <div
          v-else-if="logMessages.length === 0"
          class="py-10 text-center text-sm text-muted-foreground"
        >
          当前密钥还没有调用记录。
        </div>
        <template v-else>
          <div
            v-for="item in logMessages"
            :key="item.id"
            class="rounded-lg border border-border/70 bg-muted/20 px-3 py-2"
          >
            <div class="flex items-center justify-between gap-2">
              <NTag
                :bordered="false"
                round
                size="small"
                :type="resolveRoleTone(item.role)"
              >
                {{ item.role || '--' }}
              </NTag>
              <span class="text-[11px] text-muted-foreground">
                {{ formatDocsTimestamp(item.createTime) }}
                <template v-if="item.duration"> · {{ item.duration }}ms</template>
              </span>
            </div>
            <div
              class="mt-1.5 whitespace-pre-wrap break-all text-xs leading-5 text-foreground"
            >
              {{ item.message || '--' }}
            </div>
          </div>
        </template>
      </div>
    </LogDrawer>
  </div>
</template>
