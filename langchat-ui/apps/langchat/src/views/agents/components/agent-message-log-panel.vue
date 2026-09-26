<script lang="ts" setup>
import type {AigcMessage, AigcMessageEvent, OpenAiChatCompletionChunk,} from '#/api/aigc/chat';

import {computed, ref, watch} from 'vue';

import {useVbenDrawer} from '@vben/common-ui';

import {FileText, Logs, Wrench} from '@vben/icons';

import {NButton, NEmpty, NInput, NSelect, NSpin, NTag} from 'naive-ui';

import {useVbenVxeGrid} from '#/adapter/vxe-table';
import {
  listConversationApi,
  listConversationMessagesApi,
  listMessageEventsApi,
} from '#/api/aigc/chat';

interface Props {
  agentId?: string;
}

interface MessageLogRow extends AigcMessage {
  conversationTitle?: string;
}

interface ReplayNode {
  content?: string;
  detail?: Record<string, unknown>;
  finishReason?: string;
  id: string;
  items?: Array<Record<string, unknown>>;
  kind: 'assistant-message' | 'log' | 'meta' | 'rag' | 'stop' | 'tool';
  name: string;
  payloadJson?: string;
  result?: string;
  status?: string;
  toolArguments?: string;
  toolName?: string;
  type?: string;
  usage?: Record<string, unknown>;
}

const props = withDefaults(defineProps<Props>(), {
  agentId: '',
});

const loading = ref(false);
const eventLoading = ref(false);
const keyword = ref('');
const roleFilter = ref('');
const rows = ref<MessageLogRow[]>([]);
const selectedRowId = ref('');
const selectedRow = ref<MessageLogRow | null>(null);
const eventRows = ref<AigcMessageEvent[]>([]);

const roleOptions = [
  { label: '全部角色', value: '' },
  { label: '用户', value: 'user' },
  { label: '助手', value: 'assistant' },
  { label: '系统', value: 'system' },
  { label: '工具', value: 'tool' },
];

const filteredRows = computed(() => {
  const query = keyword.value.trim().toLowerCase();
  return rows.value.filter((item) => {
    const roleMatched =
      !roleFilter.value || String(item.role || '') === roleFilter.value;
    if (!roleMatched) {
      return false;
    }
    if (!query) {
      return true;
    }
    return [item.message, item.model, item.conversationTitle, item.role]
      .map((value) => String(value || '').toLowerCase())
      .some((value) => value.includes(query));
  });
});

const replayNodes = computed<ReplayNode[]>(() => {
  const nodes: ReplayNode[] = [];
  let assistantMessageNode: null | ReplayNode = null;
  const toolNodeMap = new Map<string, ReplayNode>();

  eventRows.value.forEach((item, index) => {
    const parsed = parseChunk(item.payloadJson || '');
    const detail = resolveChunkEvent(parsed) || undefined;
    const content = resolveChunkContent(parsed);
    const finishReason = resolveFinishReason(parsed);
    const eventName = String(item.eventName || detail?.name || '--');
    const eventStatus = item.eventStatus || String(detail?.status || '');

    if (content) {
      if (!assistantMessageNode) {
        assistantMessageNode = {
          content: '',
          id: `assistant-message-${index}`,
          kind: 'assistant-message',
          name: 'assistant.message',
          status: 'completed',
          type: 'message',
        };
        nodes.push(assistantMessageNode);
      }
      assistantMessageNode.content = `${assistantMessageNode.content || ''}${content}`;
      return;
    }

    if (eventName === 'tool.before' || eventName === 'tool.executed') {
      const requestId = String(
        detail?.request_id || detail?.requestId || `tool-${index}`,
      );
      let node = toolNodeMap.get(requestId);
      if (!node) {
        node = {
          id: `tool-${requestId}`,
          kind: 'tool',
          name: 'tool.call',
          status: 'running',
          toolArguments: String(detail?.arguments || ''),
          toolName: String(
            detail?.tool_name || detail?.toolName || '未命名工具',
          ),
          type: 'tool',
        };
        toolNodeMap.set(requestId, node);
        nodes.push(node);
      }
      node.status = eventStatus || node.status;
      node.toolArguments = String(
        detail?.arguments || node.toolArguments || '',
      );
      node.toolName = String(
        detail?.tool_name || detail?.toolName || node.toolName || '未命名工具',
      );
      node.result = String(detail?.result || node.result || '');
      node.detail = detail;
      node.payloadJson = item.payloadJson || '';
      return;
    }

    if (eventName === 'rag.retrieved') {
      nodes.push({
        detail,
        id: `rag-${index}`,
        items: resolveChunkItems(parsed),
        kind: 'rag',
        name: eventName,
        payloadJson: item.payloadJson || '',
        status: eventStatus,
        type: item.eventType || 'rag',
      });
      return;
    }

    if (eventName === 'log.delta') {
      nodes.push({
        content: String(detail?.message || ''),
        detail,
        id: `log-${index}`,
        kind: 'log',
        name: String(detail?.phase || eventName),
        payloadJson: item.payloadJson || '',
        status: eventStatus,
        type: item.eventType || 'log',
      });
      return;
    }

    if (eventName === 'message.completed') {
      nodes.push({
        detail,
        finishReason: String(detail?.finish_reason || ''),
        id: `meta-${index}`,
        kind: 'meta',
        name: eventName,
        payloadJson: item.payloadJson || '',
        status: eventStatus,
        type: item.eventType || 'meta',
        usage: resolveUsage(detail),
      });
      return;
    }

    if (finishReason) {
      nodes.push({
        finishReason,
        id: `stop-${index}`,
        kind: 'stop',
        name: 'message.stop',
        payloadJson: item.payloadJson || '',
        status: eventStatus || 'completed',
        type: 'stop',
      });
      return;
    }
  });

  return nodes;
});

async function loadRows() {
  if (!props.agentId) {
    rows.value = [];
    selectedRowId.value = '';
    selectedRow.value = null;
    eventRows.value = [];
    return;
  }
  loading.value = true;
  try {
    const conversations = await listConversationApi({
      agentId: props.agentId,
      pageNo: 1,
      pageSize: 30,
    });
    const messageGroups = await Promise.all(
      conversations.map(async (conversation) => {
        if (!conversation.id) {
          return [] as MessageLogRow[];
        }
        const records = await listConversationMessagesApi(conversation.id);
        return records.map((item) => ({
          ...item,
          conversationTitle: conversation.title || '未命名会话',
        }));
      }),
    );
    rows.value = messageGroups
      .flat()
      .sort((a, b) => (b.createTime ?? 0) - (a.createTime ?? 0));
    await gridApi.reload();
  } finally {
    loading.value = false;
  }
}

async function openDetail(row: MessageLogRow) {
  selectedRow.value = row;
  selectedRowId.value = row.id || '';
  drawerApi.open();
  if (!row.chatId) {
    eventRows.value = [];
    return;
  }
  eventLoading.value = true;
  try {
    eventRows.value = await listMessageEventsApi({
      chatId: row.chatId,
    });
  } finally {
    eventLoading.value = false;
  }
}

const [DetailDrawer, drawerApi] = useVbenDrawer({
  class: 'w-[640px]',
  footer: false,
  title: '链路详情',
});

function parseChunk(payloadJson: string) {
  if (!payloadJson) {
    return null;
  }
  try {
    return JSON.parse(payloadJson) as OpenAiChatCompletionChunk;
  } catch {
    return null;
  }
}

function resolveChunkEvent(chunk: null | OpenAiChatCompletionChunk) {
  const event = chunk?.choices?.[0]?.delta?.event;
  if (!event || typeof event !== 'object') {
    return null;
  }
  return event as Record<string, unknown>;
}

function resolveChunkContent(chunk: null | OpenAiChatCompletionChunk) {
  const content = chunk?.choices?.[0]?.delta?.content;
  return content ? String(content) : '';
}

function resolveFinishReason(chunk: null | OpenAiChatCompletionChunk) {
  return String(
    chunk?.choices?.[0]?.finish_reason ||
      chunk?.choices?.[0]?.finishReason ||
      '',
  );
}

function resolveChunkItems(chunk: null | OpenAiChatCompletionChunk) {
  const event = resolveChunkEvent(chunk);
  const items = event?.items;
  return Array.isArray(items) ? (items as Array<Record<string, unknown>>) : [];
}

function resolveEventTone(status?: string) {
  if (status === 'failed') {
    return 'error' as const;
  }
  if (status === 'completed') {
    return 'success' as const;
  }
  if (status === 'running') {
    return 'info' as const;
  }
  return 'default' as const;
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

function formatTimestamp(value?: number) {
  return value
    ? new Date(value).toLocaleString('zh-CN', { hour12: false })
    : '--';
}

function resolveUsage(detail?: Record<string, unknown>) {
  const usage = detail?.usage;
  if (!usage || typeof usage !== 'object') {
    return undefined;
  }
  return usage as Record<string, unknown>;
}

function shouldRenderDetail(node: ReplayNode) {
  return Boolean(node.detail && node.kind !== 'tool' && node.kind !== 'meta');
}

const [Grid, gridApi] = useVbenVxeGrid<MessageLogRow>({
  class: 'bg-transparent shadow-none',
  gridClass: 'px-0 pb-0',
  gridOptions: {
    columns: [
      { field: 'conversationTitle', minWidth: 160, title: '会话' },
      {
        field: 'role',
        title: '角色',
        width: 90,
        slots: { default: 'roleCol' },
      },
      {
        align: 'left',
        field: 'message',
        minWidth: 260,
        title: '消息内容',
      },
      { field: 'model', title: '模型', width: 140 },
      { field: 'duration', title: '耗时(ms)', width: 100 },
      {
        field: 'createTime',
        title: '时间',
        width: 170,
        formatter: ({ cellValue }: { cellValue: number }) =>
          cellValue
            ? new Date(cellValue).toLocaleString('zh-CN', { hour12: false })
            : '--',
      },
      {
        field: 'actions',
        fixed: 'right',
        title: '操作',
        width: 90,
        slots: { default: 'actionCol' },
      },
    ],
    pagerConfig: {
      pageSize: 20,
      pageSizes: [20, 50, 100],
    },
    proxyConfig: {
      ajax: {
        query: ({
          page,
        }: {
          page?: { currentPage: number; pageSize: number };
        }) => {
          const currentPage = page?.currentPage ?? 1;
          const pageSize = page?.pageSize ?? 20;
          const source = filteredRows.value;
          const start = (currentPage - 1) * pageSize;
          return Promise.resolve({
            items: source.slice(start, start + pageSize),
            total: source.length,
          });
        },
      },
    },
    rowConfig: {
      keyField: 'id',
    },
  },
});

watch(
  () => props.agentId,
  async () => {
    await loadRows();
  },
  { immediate: true },
);

watch([keyword, roleFilter], () => {
  void gridApi.reload();
});
</script>

<template>
  <div
    class="flex min-h-0 flex-col rounded-xl border border-border bg-card p-3"
  >
    <div class="mb-3 flex flex-wrap items-center justify-between gap-2">
      <div class="flex items-center gap-2">
        <div
          class="inline-flex size-8 items-center justify-center rounded-lg border border-border bg-background"
        >
          <Logs class="size-4 text-primary" />
        </div>
        <div>
          <div class="text-sm font-semibold text-foreground">消息日志</div>
          <div class="text-xs text-muted-foreground">
            全量展示消息明细，点击操作列查看消息链路详情。
          </div>
        </div>
      </div>
      <div class="flex flex-wrap items-center gap-2">
        <NInput
          v-model:value="keyword"
          clearable
          placeholder="搜索内容 / 模型 / 会话"
          style="width: 280px"
        />
        <NSelect
          v-model:value="roleFilter"
          :options="roleOptions"
          style="width: 140px"
        />
        <NButton :loading="loading" secondary @click="loadRows">刷新</NButton>
      </div>
    </div>

    <div
      class="flex min-h-0 flex-1 flex-col rounded-lg border border-border/70 bg-background/60 px-3 py-2"
    >
      <Grid class="min-h-0 flex-1" table-title="">
        <template #roleCol="{ row }">
          <NTag
            :bordered="false"
            :type="resolveRoleTone(row.role)"
            round
            size="small"
          >
            {{ row.role || '--' }}
          </NTag>
        </template>

        <template #actionCol="{ row }">
          <NButton
            quaternary
            size="small"
            type="primary"
            @click="openDetail(row)"
          >
            查看详情
          </NButton>
        </template>
      </Grid>
    </div>

    <DetailDrawer>
      <div class="space-y-3">
        <div
          class="flex items-start justify-between gap-3 border-b border-border/70 pb-3"
        >
          <div class="min-w-0">
            <div class="flex items-center gap-2">
              <NTag
                v-if="selectedRow"
                :bordered="false"
                :type="resolveRoleTone(selectedRow.role)"
                round
                size="small"
              >
                {{ selectedRow.role || '--' }}
              </NTag>
              <span
                class="truncate text-sm font-medium text-foreground"
                >{{ selectedRow?.conversationTitle || '--' }}</span
              >
            </div>
            <div class="mt-1 text-xs text-muted-foreground">
              chat_id {{ selectedRow?.chatId || '--' }}
            </div>
          </div>
          <div
            v-if="selectedRow"
            class="text-right text-xs text-muted-foreground"
          >
            <div>时间 {{ formatTimestamp(selectedRow.createTime) }}</div>
          </div>
        </div>

        <div v-if="eventLoading" class="flex h-40 items-center justify-center">
          <NSpin size="small" />
        </div>

        <div
          v-else-if="replayNodes.length === 0"
          class="flex h-40 items-center justify-center"
        >
          <NEmpty description="当前消息还没有事件数据" />
        </div>

        <div v-else class="space-y-2">
          <div
            v-for="(item, index) in replayNodes"
            :key="`${selectedRowId}-${item.id}-${index}`"
            class="rounded-lg border border-border/70 bg-card p-2.5"
          >
            <div class="flex items-center justify-between gap-2">
              <div class="flex min-w-0 items-center gap-2">
                <span
                  class="inline-flex size-5 items-center justify-center rounded-md border border-border bg-background text-[11px] text-muted-foreground"
                >
                  {{ index + 1 }}
                </span>
                <span
                  class="truncate text-xs font-medium text-foreground"
                  >{{ item.name }}</span
                >
                <NTag v-if="item.type" :bordered="false" round size="small">
                  {{ item.type }}
                </NTag>
              </div>
              <NTag
                v-if="item.status"
                :bordered="false"
                :type="resolveEventTone(item.status)"
                round
                size="small"
              >
                {{ item.status }}
              </NTag>
            </div>

            <div
              v-if="item.kind === 'assistant-message' && item.content"
              class="mt-2 rounded-md border border-primary/20 bg-primary/5 px-2.5 py-2 text-xs leading-6 text-foreground"
            >
              {{ item.content }}
            </div>

            <div v-else-if="item.kind === 'tool'" class="mt-2 space-y-2">
              <div class="flex items-center gap-2 text-xs text-foreground">
                <Wrench class="size-3.5 text-primary" />
                <span class="font-medium">{{
                  item.toolName || '未命名工具'
                }}</span>
              </div>
              <pre
                v-if="item.toolArguments"
                class="overflow-x-auto rounded-md border border-border/70 bg-background p-2 text-[11px] leading-5 text-foreground"
                >{{ item.toolArguments }}</pre>
              <pre
                v-if="item.result"
                class="overflow-x-auto rounded-md border border-border/70 bg-background p-2 text-[11px] leading-5 text-foreground"
                >{{ item.result }}</pre>
            </div>

            <div
              v-else-if="item.kind === 'stop' && item.finishReason"
              class="mt-2 text-xs text-muted-foreground"
            >
              finish_reason: {{ item.finishReason }}
            </div>

            <div v-else-if="item.kind === 'meta'" class="mt-2 space-y-2">
              <div
                v-if="item.finishReason"
                class="text-xs text-muted-foreground"
              >
                finish_reason: {{ item.finishReason }}
              </div>
              <div v-if="item.usage" class="flex flex-wrap gap-1.5">
                <NTag :bordered="false" round size="small">
                  prompt_tokens
                  {{ item.usage.prompt_tokens || item.usage.promptTokens || 0 }}
                </NTag>
                <NTag :bordered="false" round size="small" type="success">
                  completion_tokens
                  {{
                    item.usage.completion_tokens ||
                    item.usage.completionTokens ||
                    0
                  }}
                </NTag>
                <NTag :bordered="false" round size="small" type="info">
                  total_tokens
                  {{ item.usage.total_tokens || item.usage.totalTokens || 0 }}
                </NTag>
              </div>
            </div>

            <template v-else>
              <div
                v-if="item.kind === 'log' && item.content"
                class="mt-2 rounded-md border border-dashed border-border/70 bg-background px-2 py-1.5 text-xs text-muted-foreground"
              >
                {{ item.content }}
              </div>

              <div
                v-if="item.items && item.items.length > 0"
                class="mt-2 flex flex-wrap gap-1.5"
              >
                <span
                  v-for="entry in item.items.slice(0, 8)"
                  :key="`${entry.segmentId || entry.segment_id || entry.docsId || entry.docs_id || ''}`"
                  class="inline-flex items-center gap-1 rounded-md border border-border bg-background px-2 py-1 text-[11px] text-muted-foreground"
                >
                  <FileText class="size-3 text-primary" />
                  {{
                    entry.docsName ||
                    entry.docs_name ||
                    entry.knowledgeName ||
                    entry.knowledge_name ||
                    '未命名片段'
                  }}
                </span>
              </div>

              <pre
                v-if="shouldRenderDetail(item)"
                class="mt-2 overflow-x-auto rounded-md border border-border/70 bg-background p-2 text-[11px] leading-5 text-foreground"
                >{{ JSON.stringify(item.detail, null, 2) }}</pre>
            </template>
          </div>
        </div>
      </div>
    </DetailDrawer>
  </div>
</template>
