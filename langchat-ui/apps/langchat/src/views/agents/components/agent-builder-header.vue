<script setup lang="ts">
import { computed, ref } from 'vue';

import {
  ArrowLeft,
  Ban,
  BrainCircuit,
  ChartColumn,
  ChevronDown,
  Clock3,
  KeyRound,
  Logs,
  Rocket,
  Save,
  SlidersHorizontal,
} from '@vben/icons';

import { NButton, NPopover, NTag } from 'naive-ui';

import LcIconDisplay from '#/components/LcIcon/display.vue';
import LcStatusTag from '#/components/LcStatusTag/index.vue';

type BuilderTab = 'config' | 'keys' | 'logs' | 'stats';

interface TabOption {
  key: BuilderTab;
  label: string;
}

interface Props {
  appIcon?: string;
  createTime?: number;
  creator?: string;
  modelLabel?: string;
  pageTitle: string;
  saving?: boolean;
  statusLabel?: string;
  statusType?: 'error' | 'success' | 'warning';
  summary?: string;
  tab: BuilderTab;
  tabs: TabOption[];
  updateTime?: number;
  updater?: string;
}

const props = withDefaults(defineProps<Props>(), {
  appIcon: '',
  createTime: 0,
  creator: '--',
  modelLabel: '--',
  saving: false,
  statusLabel: '草稿',
  statusType: 'warning',
  summary: '',
  updateTime: 0,
  updater: '--',
});

const emit = defineEmits<{
  back: [];
  saveDraft: [];
  saveWithStatus: [status: 'DISABLED' | 'PUBLISHED'];
  'update:tab': [value: BuilderTab];
}>();

const actionShow = ref(false);

const updateTimeText = computed(() => {
  if (!props.updateTime) {
    return '--';
  }
  return new Date(props.updateTime).toLocaleString('zh-CN', { hour12: false });
});

const createTimeText = computed(() => {
  if (!props.createTime) {
    return '--';
  }
  return new Date(props.createTime).toLocaleString('zh-CN', { hour12: false });
});

function resolveTabIcon(tab: BuilderTab) {
  if (tab === 'keys') {
    return KeyRound;
  }
  if (tab === 'logs') {
    return Logs;
  }
  if (tab === 'stats') {
    return ChartColumn;
  }
  return SlidersHorizontal;
}

const publishLabel = computed(() =>
  props.statusLabel === '已发布' ? '取消发布' : '发布应用',
);
const publishStatus = computed<'DISABLED' | 'PUBLISHED'>(() =>
  props.statusLabel === '已发布' ? 'DISABLED' : 'PUBLISHED',
);
const publishIcon = computed(() =>
  props.statusLabel === '已发布' ? Ban : Rocket,
);

function handleSaveDraft() {
  actionShow.value = false;
  emit('saveDraft');
}

function handlePublish() {
  actionShow.value = false;
  emit('saveWithStatus', publishStatus.value);
}
</script>

<template>
  <div class="rounded-xl border border-border bg-card">
    <!-- 顶部行 -->
    <div class="flex items-center justify-between gap-3 px-4 py-2.5">
      <div class="flex min-w-0 items-center gap-3">
        <NButton circle quaternary size="small" @click="emit('back')">
          <template #icon>
            <ArrowLeft class="size-4" />
          </template>
        </NButton>

        <LcIconDisplay
          :icon="appIcon"
          fallback-icon="lucide:bot"
          :size="40"
          class="shrink-0 rounded-[10px] border-border/80 bg-background"
        />

        <div class="min-w-0">
          <div class="flex items-center gap-2">
            <div class="truncate text-lg font-semibold text-foreground">
              {{ pageTitle }}
            </div>
            <LcStatusTag :label="statusLabel" :type="statusType" />
          </div>
          <div
            class="mt-1 flex flex-wrap items-center gap-x-1.5 gap-y-0.5 text-[11px] leading-4 text-muted-foreground"
          >
            <span class="max-w-[200px] truncate">{{ summary || '未命名应用' }}</span>
            <span class="size-0.5 shrink-0 rounded-full bg-muted-foreground/40"></span>
            <span class="inline-flex items-center gap-1">
              <Clock3 class="size-3 shrink-0" />
              更新时间 {{ updateTimeText }}
            </span>
            <span class="size-0.5 shrink-0 rounded-full bg-muted-foreground/40"></span>
            <span class="inline-flex items-center gap-1">
              <BrainCircuit class="size-3 shrink-0" />
              模型 {{ modelLabel || '--' }}
            </span>
          </div>
        </div>
      </div>

      <div class="flex shrink-0 items-center gap-2">
        <NPopover
          v-model:show="actionShow"
          :show-arrow="false"
          placement="bottom-end"
          trigger="click"
        >
          <template #trigger>
            <NButton :loading="saving" type="primary">
              <template #icon>
                <Save class="size-3.5" />
              </template>
              保存 / 发布
              <ChevronDown class="size-3.5" />
            </NButton>
          </template>
          <div class="w-60 px-1">
            <div
              class="mb-2 flex flex-col gap-1.5 rounded-md bg-muted/30 px-3 py-2.5"
            >
              <div class="flex items-center justify-between">
                <span class="text-[11px] text-muted-foreground">创建人</span>
                <NTag :bordered="false" round size="small" type="info">
                  {{ creator || '--' }}
                </NTag>
              </div>
              <div class="flex items-center justify-between">
                <span class="text-[11px] text-muted-foreground">创建时间</span>
                <NTag :bordered="false" round size="small">
                  {{ createTimeText }}
                </NTag>
              </div>
              <div class="flex items-center justify-between">
                <span class="text-[11px] text-muted-foreground">最后修改</span>
                <NTag :bordered="false" round size="small">
                  {{ updateTimeText }}
                </NTag>
              </div>
              <div class="flex items-center justify-between">
                <span class="text-[11px] text-muted-foreground">修改人</span>
                <NTag :bordered="false" round size="small" type="info">
                  {{ updater || '--' }}
                </NTag>
              </div>
            </div>
            <div class="flex flex-col gap-2">
              <NButton block secondary @click="handleSaveDraft">
                <template #icon>
                  <Save class="size-3.5" />
                </template>
                保存草稿
              </NButton>
              <NButton
                block
                :type="publishStatus === 'PUBLISHED' ? 'warning' : 'primary'"
                @click="handlePublish"
              >
                <template #icon>
                  <component :is="publishIcon" class="size-3.5" />
                </template>
                {{ publishLabel }}
              </NButton>
            </div>
          </div>
        </NPopover>
      </div>
    </div>

    <!-- 选项切换条 -->
    <div
      class="flex w-full items-center gap-2 overflow-x-auto border-t border-border px-3 py-1.5"
    >
      <button
        v-for="item in tabs"
        :key="item.key"
        :class="
          tab === item.key
            ? 'border-primary/50 bg-primary/8 text-primary'
            : 'border-border text-muted-foreground hover:border-primary/40 hover:bg-muted/40 hover:text-foreground'
        "
        class="inline-flex shrink-0 items-center gap-1.5 rounded-md border px-3 py-1 text-xs font-medium transition-colors"
        type="button"
        @click="emit('update:tab', item.key)"
      >
        <component :is="resolveTabIcon(item.key)" class="size-3.5" />
        {{ item.label }}
      </button>
    </div>
  </div>
</template>
