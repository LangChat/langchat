<script lang="ts" setup>
import type { AigcAgentApiKey } from '#/api/aigc/agent-api-key';
import type { AigcMessage } from '#/api/aigc/chat';

import { computed, ref, watch } from 'vue';

import { useVbenDrawer, useVbenModal } from '@vben/common-ui';
import {
  Activity,
  Clock3,
  Copy,
  FileText,
  KeyRound,
  Plus,
  ShieldCheck,
  Tag,
  Trash2,
} from '@vben/icons';
import { $t } from '@vben/locales';

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
  return `curl -X POST "${origin}${OPENAI_COMPATIBLE_PATH}" \\\n  -H "Authorization: Bearer sk-xxxx" \\\n  -H "Content-Type: application/json" \\\n  -d '{"model":"agent","messages":[{"role":"user","content":"${$t('agents.apiKey.exampleContent')}"}]}'`;
});

const [CreateModal, createModalApi] = useVbenModal({
  class: 'w-[min(560px,calc(100vw-32px))]',
  confirmText: $t('common.actions.create'),
  title: $t('agents.apiKey.createTitle'),
  onConfirm: async () => {
    await handleCreate();
  },
});

watch(
  () => $t('agents.apiKey.createTitle'),
  (value) => {
    createModalApi.setState({ title: value });
  },
  { immediate: true },
);

watch(
  () => $t('common.actions.create'),
  (value) => {
    createModalApi.setState({ confirmText: value });
  },
  { immediate: true },
);

const [LogDrawer, logDrawerApi] = useVbenDrawer({
  class: 'w-[640px]',
  footer: false,
  title: $t('agents.apiKey.logs'),
});

watch(
  () => $t('agents.apiKey.logs'),
  (value) => {
    logDrawerApi.setState({ title: value });
  },
  { immediate: true },
);

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
    message.warning($t('agents.apiKey.messages.nameRequired'));
    return;
  }
  submitting.value = true;
  try {
    await createAgentApiKeyApi(props.agentId, { ...createForm.value });
    message.success($t('agents.apiKey.messages.created'));
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
    message.success(
      checked
        ? $t('common.status.enabledMessage')
        : $t('common.status.disabledMessage'),
    );
  } catch {
    row.status = checked ? 'DISABLED' : 'ENABLED';
  }
}

async function handleCopy(text?: string, tip?: string) {
  if (!text) {
    return;
  }
  try {
    await navigator.clipboard.writeText(text);
    message.success(tip ?? $t('common.messages.copySuccess'));
  } catch {
    message.error($t('agents.apiKey.messages.copyFailed'));
  }
}

function handleRemove(row: AigcAgentApiKey) {
  dialog.warning({
    closable: false,
    content: $t('agents.apiKey.messages.deleteConfirmContent', {
      name: row.name || $t('agents.apiKey.unnamed'),
    }),
    negativeText: $t('common.actions.cancel'),
    positiveText: $t('common.actions.confirmDelete'),
    title: $t('agents.apiKey.deleteTitle'),
    onPositiveClick: async () => {
      await removeAgentApiKeyApi(row.id!);
      message.success($t('agents.apiKey.messages.deleted'));
      await loadKeys();
    },
  });
}

async function openLogs(row: AigcAgentApiKey) {
  activeLogKey.value = row;
  logMessages.value = [];
  logDrawerApi.setState({
    title: $t('agents.apiKey.logTitle', {
      name: row.name || $t('agents.apiKey.logTitleFallback'),
    }),
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
  <div class="flex h-full min-h-0 flex-col gap-4 overflow-y-auto rounded-xl border border-border bg-card p-4 lg:p-5">
    <header class="flex flex-wrap items-start justify-between gap-4">
      <div class="flex min-w-0 items-start gap-3">
        <div class="flex size-10 shrink-0 items-center justify-center rounded-lg border border-primary/20 bg-primary/8 text-primary">
          <KeyRound class="size-5" />
        </div>
        <div class="min-w-0">
          <div class="flex flex-wrap items-center gap-2">
            <h2 class="text-base font-semibold text-foreground">
              {{ $t('agents.apiKey.title') }}
            </h2>
            <span class="text-[10px] font-medium uppercase tracking-[0.12em] text-muted-foreground">
              OpenAI compatible
            </span>
          </div>
          <p class="mt-1 max-w-2xl text-xs leading-5 text-muted-foreground">
            {{ $t('agents.apiKey.description') }}
          </p>
        </div>
      </div>
      <NButton :disabled="!hasAgent" type="primary" @click="openCreate">
        <template #icon>
          <Plus class="size-4" />
        </template>
        {{ $t('agents.apiKey.createButton') }}
      </NButton>
    </header>

    <div
      v-if="!hasAgent"
      class="flex min-h-48 flex-col items-center justify-center rounded-lg border border-dashed border-border px-6 text-center"
    >
      <div class="flex size-10 items-center justify-center rounded-full bg-muted/50 text-muted-foreground">
        <KeyRound class="size-5" />
      </div>
      <div class="mt-3 text-sm font-medium text-foreground">
        {{ $t('agents.apiKey.saveFirstTitle') }}
      </div>
      <div class="mt-1 text-xs text-muted-foreground">
        {{ $t('agents.apiKey.saveFirstDescription') }}
      </div>
    </div>

    <template v-else>
      <div class="grid min-h-0 gap-4 lg:grid-cols-[minmax(0,1.35fr)_minmax(300px,0.85fr)]">
        <aside class="order-1 min-w-0 self-start lg:sticky lg:top-0 lg:col-start-2 lg:row-start-1">
          <div class="overflow-hidden rounded-lg border border-border/70 bg-muted/15">
            <div class="flex items-start gap-2.5 border-b border-border/60 px-4 py-3">
              <div class="flex size-7 shrink-0 items-center justify-center rounded-md bg-primary/10 text-primary">
                <ShieldCheck class="size-3.5" />
              </div>
              <div>
                <div class="text-xs font-semibold text-foreground">
                  {{ $t('agents.apiKey.guideTitle') }}
                </div>
                <div class="mt-0.5 text-[11px] text-muted-foreground">
                  {{ $t('agents.apiKey.guideDescription') }}
                </div>
              </div>
            </div>
            <section class="overflow-hidden">
        <div class="flex flex-wrap items-center justify-between gap-3 border-b border-border/60 px-4 py-3">
          <div class="flex min-w-0 items-center gap-2.5">
            <div class="flex size-7 shrink-0 items-center justify-center rounded-md bg-primary/10 text-primary">
              <KeyRound class="size-3.5" />
            </div>
            <div class="min-w-0">
              <div class="text-xs font-semibold text-foreground">
                {{ $t('agents.apiKey.endpointTitle') }}
              </div>
              <div class="mt-0.5 flex flex-wrap items-center gap-1.5 text-[11px] text-muted-foreground">
                <NTag :bordered="false" size="small" type="info">POST</NTag>
                <code class="truncate">{{ OPENAI_COMPATIBLE_PATH }}</code>
              </div>
            </div>
          </div>
          <NButton
            quaternary
            size="small"
            type="primary"
            @click="handleCopy(endpointExample, $t('agents.apiKey.exampleCopied'))"
          >
            <template #icon>
              <Copy class="size-3.5" />
            </template>
            {{ $t('agents.apiKey.copyExample') }}
          </NButton>
        </div>
        <pre class="max-h-36 overflow-auto bg-background/65 px-4 py-3 text-[11px] leading-5 text-muted-foreground">{{ endpointExample }}</pre>
            </section>
            <div class="border-t border-border/60 px-4 py-3">
              <div class="mb-2 text-[10px] font-semibold uppercase tracking-[0.08em] text-muted-foreground">
                {{ $t('agents.apiKey.quickStart') }}
              </div>
              <ol class="space-y-2.5 text-xs leading-5 text-muted-foreground">
                <li class="flex gap-2">
                  <span class="flex size-5 shrink-0 items-center justify-center rounded-full bg-primary/10 text-[10px] font-semibold text-primary">1</span>
                  <span>{{ $t('agents.apiKey.steps.first') }}</span>
                </li>
                <li class="flex gap-2">
                  <span class="flex size-5 shrink-0 items-center justify-center rounded-full bg-primary/10 text-[10px] font-semibold text-primary">2</span>
                  <span>{{ $t('agents.apiKey.steps.second') }}</span>
                </li>
                <li class="flex gap-2">
                  <span class="flex size-5 shrink-0 items-center justify-center rounded-full bg-primary/10 text-[10px] font-semibold text-primary">3</span>
                  <span>
                    {{ $t('agents.apiKey.steps.thirdBefore') }}
                    <code class="rounded bg-muted/50 px-1 py-0.5 text-[11px] text-foreground">Bearer</code>
                    {{ $t('agents.apiKey.steps.thirdAfter') }}
                  </span>
                </li>
              </ol>
            </div>
          </div>
        </aside>

      <section class="order-2 min-h-0 lg:col-start-1 lg:row-start-1">
        <div class="mb-2 flex items-center justify-between gap-3">
          <div>
            <h3 class="text-sm font-semibold text-foreground">
              {{ $t('agents.apiKey.keysTitle') }}
            </h3>
            <p class="mt-0.5 text-[11px] text-muted-foreground">
              {{ $t('agents.apiKey.keysDescription') }}
            </p>
          </div>
          <span
            v-if="items.length > 0"
            class="text-[11px] tabular-nums text-muted-foreground"
          >
            {{ $t('agents.apiKey.count', { count: items.length }) }}
          </span>
        </div>

        <div
          v-if="loading"
          class="rounded-lg border border-border/60 px-4 py-10 text-center text-sm text-muted-foreground"
        >
          {{ $t('agents.apiKey.loading') }}
        </div>

        <div
          v-else-if="items.length === 0"
          class="rounded-lg border border-dashed border-border px-6 py-10 text-center"
        >
          <NEmpty :description="$t('agents.apiKey.empty')">
            <template #extra>
              <NButton size="small" type="primary" @click="openCreate">
                <template #icon><Plus class="size-3.5" /></template>
                {{ $t('agents.apiKey.createFirst') }}
              </NButton>
            </template>
          </NEmpty>
        </div>

        <div v-else class="divide-y divide-border/70 overflow-hidden rounded-lg border border-border/70 bg-background/35">
          <article
            v-for="row in items"
            :key="row.id"
            class="p-4 transition-colors hover:bg-muted/20"
          >
            <div class="flex flex-wrap items-start justify-between gap-3">
              <div class="flex min-w-0 items-start gap-3">
                <div class="flex size-9 shrink-0 items-center justify-center rounded-md border border-border bg-muted/25 text-muted-foreground">
                  <KeyRound class="size-4" />
                </div>
                <div class="min-w-0">
                  <div class="flex flex-wrap items-center gap-2">
                    <span class="truncate text-sm font-medium text-foreground">
                      {{ row.name || $t('agents.apiKey.unnamed') }}
                    </span>
                    <NTag
                      :bordered="false"
                      size="small"
                      :type="row.status === 'ENABLED' ? 'success' : 'default'"
                    >
                      {{
                        row.status === 'ENABLED'
                          ? $t('common.status.enabledMessage')
                          : $t('common.status.disabledMessage')
                      }}
                    </NTag>
                  </div>
                  <div class="mt-1.5 flex min-w-0 items-center gap-1.5">
                    <code class="max-w-[min(420px,65vw)] truncate rounded bg-muted/45 px-2 py-1 text-[11px] text-muted-foreground">
                      {{ maskKey(row.apiKey) }}
                    </code>
                    <NButton
                      v-tippy="$t('agents.apiKey.copyKey')"
                      :aria-label="$t('agents.apiKey.copyKey')"
                      circle
                      quaternary
                      size="tiny"
                      @click="handleCopy(row.apiKey, $t('agents.apiKey.keyCopied'))"
                    >
                      <template #icon><Copy class="size-3.5" /></template>
                    </NButton>
                  </div>
                </div>
              </div>

              <div class="flex items-center gap-1">
                <NButton quaternary size="small" @click="openLogs(row)">
                  <template #icon><Activity class="size-3.5" /></template>
                  {{ $t('agents.apiKey.logs') }}
                </NButton>
                <div class="ml-1 flex items-center gap-1.5 border-l border-border/70 pl-3">
                  <span class="text-[11px] text-muted-foreground">
                    {{ $t('agents.apiKey.enabledLabel') }}
                  </span>
                  <NSwitch
                    :value="row.status === 'ENABLED'"
                    size="small"
                    @update:value="(checked: boolean) => handleStatusChange(row, checked)"
                  />
                </div>
                <NButton
                  v-tippy="$t('agents.apiKey.deleteKey')"
                  :aria-label="$t('agents.apiKey.deleteKey')"
                  circle
                  quaternary
                  size="small"
                  type="error"
                  @click="handleRemove(row)"
                >
                  <template #icon><Trash2 class="size-3.5" /></template>
                </NButton>
              </div>
            </div>

            <div class="mt-4 grid grid-cols-2 gap-2 sm:grid-cols-4">
              <div class="rounded-md bg-muted/25 px-3 py-2">
                <div class="text-[10px] uppercase tracking-[0.08em] text-muted-foreground">
                  {{ $t('agents.apiKey.stats.calls') }}
                </div>
                <div class="mt-1 text-sm font-semibold tabular-nums text-foreground">{{ row.callCount ?? 0 }}</div>
              </div>
              <div class="rounded-md bg-muted/25 px-3 py-2">
                <div class="text-[10px] uppercase tracking-[0.08em] text-muted-foreground">
                  {{ $t('agents.apiKey.stats.inputTokens') }}
                </div>
                <div class="mt-1 text-sm font-semibold tabular-nums text-foreground">{{ row.inputTokens ?? 0 }}</div>
              </div>
              <div class="rounded-md bg-muted/25 px-3 py-2">
                <div class="text-[10px] uppercase tracking-[0.08em] text-muted-foreground">
                  {{ $t('agents.apiKey.stats.outputTokens') }}
                </div>
                <div class="mt-1 text-sm font-semibold tabular-nums text-foreground">{{ row.outputTokens ?? 0 }}</div>
              </div>
              <div class="rounded-md bg-muted/25 px-3 py-2">
                <div class="text-[10px] uppercase tracking-[0.08em] text-muted-foreground">
                  {{ $t('agents.apiKey.stats.lastCall') }}
                </div>
                <div class="mt-1 truncate text-sm font-semibold text-foreground">{{ formatDocsTimestamp(row.lastCallTime) }}</div>
              </div>
            </div>

            <div v-if="row.remark" class="mt-3 flex items-start gap-1.5 text-[11px] leading-5 text-muted-foreground">
              <FileText class="mt-0.5 size-3.5 shrink-0" />
              <span class="line-clamp-2">{{ row.remark }}</span>
            </div>
          </article>
        </div>
      </section>
      </div>
    </template>

    <CreateModal>
      <div class="space-y-5">
        <div class="flex items-start gap-3 rounded-lg border border-primary/15 bg-primary/5 px-4 py-3">
          <div class="flex size-8 shrink-0 items-center justify-center rounded-md bg-primary/10 text-primary">
            <KeyRound class="size-4" />
          </div>
          <div>
            <div class="text-sm font-medium text-foreground">
              {{ $t('agents.apiKey.modal.title') }}
            </div>
            <div class="mt-1 text-xs leading-5 text-muted-foreground">
              {{ $t('agents.apiKey.modal.description') }}
            </div>
          </div>
        </div>

        <NForm label-placement="top" class="space-y-1">
          <NFormItem required>
            <template #label>
              <span class="inline-flex items-center gap-1.5 text-xs font-medium text-foreground">
                <Tag class="size-3.5 text-muted-foreground" />
                {{ $t('agents.apiKey.modal.name') }}
              </span>
            </template>
            <NInput
              v-model:value="createForm.name"
              :maxlength="50"
              :placeholder="$t('agents.apiKey.modal.namePlaceholder')"
              show-count
            />
          </NFormItem>
          <NFormItem>
            <template #label>
              <span class="inline-flex items-center gap-1.5 text-xs font-medium text-foreground">
                <FileText class="size-3.5 text-muted-foreground" />
                {{ $t('agents.apiKey.modal.remark') }}
                <span class="font-normal text-muted-foreground">
                  {{ $t('common.labels.optional') }}
                </span>
              </span>
            </template>
            <NInput
              v-model:value="createForm.remark"
              :autosize="{ minRows: 3, maxRows: 5 }"
              :maxlength="200"
              :placeholder="$t('agents.apiKey.modal.remarkPlaceholder')"
              show-count
              type="textarea"
            />
          </NFormItem>
        </NForm>

        <div class="flex items-start gap-2.5 border-t border-border/70 pt-4 text-xs leading-5 text-muted-foreground">
          <ShieldCheck class="mt-0.5 size-4 shrink-0 text-emerald-500" />
          <p>
            {{ $t('agents.apiKey.modal.warningBefore') }}
            <code class="rounded bg-muted/50 px-1 py-0.5 text-[11px] text-foreground">
              {{ $t('agents.apiKey.authHeader') }}
            </code>
            {{ $t('agents.apiKey.modal.warningAfter') }}
          </p>
        </div>
      </div>
    </CreateModal>

    <LogDrawer>
      <div class="space-y-2">
        <div
          v-if="logLoading"
          class="py-10 text-center text-sm text-muted-foreground"
        >
          {{ $t('agents.apiKey.logsLoading') }}
        </div>
        <div
          v-else-if="logMessages.length === 0"
          class="py-10 text-center text-sm text-muted-foreground"
        >
          {{ $t('agents.apiKey.logsEmpty') }}
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
                size="small"
                :type="resolveRoleTone(item.role)"
              >
                {{ item.role || '--' }}
              </NTag>
              <span class="inline-flex items-center gap-1 text-[11px] text-muted-foreground">
                <Clock3 class="size-3" />
                {{ formatDocsTimestamp(item.createTime) }}
                <template v-if="item.duration"> · {{ item.duration }}ms</template>
              </span>
            </div>
            <div class="mt-1.5 whitespace-pre-wrap break-all text-xs leading-5 text-foreground">
              {{ item.message || '--' }}
            </div>
          </div>
        </template>
      </div>
    </LogDrawer>
  </div>
</template>
