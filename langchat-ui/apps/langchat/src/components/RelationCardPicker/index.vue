<script setup lang="ts">
import type { Component } from 'vue';

import { computed, ref } from 'vue';

import { CheckCircle2, Circle, Link, Plus, X } from '@vben/icons';

import { useVbenModal } from '@vben-core/popup-ui';

import { NButton, NInput, NTag } from 'naive-ui';

interface RelationOption {
  description?: string;
  label: string;
  metrics?: string[];
  tags?: string[];
  value: string;
}

interface Props {
  addText?: string;
  /** 紧凑模式：隐藏内置标题行与虚线触发器，由外部通过 open() 打开选择弹窗 */
  compact?: boolean;
  icon?: Component;
  modalTitle?: string;
  options: RelationOption[];
  placeholder?: string;
  title?: string;
}

const props = withDefaults(defineProps<Props>(), {
  addText: undefined,
  compact: false,
  icon: () => Link,
  modalTitle: '',
  placeholder: undefined,
  title: undefined,
});

const modelValue = defineModel<string[]>({ default: () => [] });
const keyword = ref('');
const [Modal, modalApi] = useVbenModal({
  onCancel() {
    modalApi.close();
  },
});

const selectedOptions = computed(() =>
  props.options.filter((item) => modelValue.value.includes(item.value)),
);

const filteredOptions = computed(() => {
  const query = keyword.value.trim().toLowerCase();
  if (!query) {
    return props.options;
  }
  return props.options.filter((item) =>
    [item.label, item.description, ...(item.tags || [])]
      .map((value) => String(value || '').toLowerCase())
      .some((value) => value.includes(query)),
  );
});

function toggleOption(value: string) {
  if (modelValue.value.includes(value)) {
    modelValue.value = modelValue.value.filter((item) => item !== value);
    return;
  }
  modelValue.value = [...modelValue.value, value];
}

function openSelectModal() {
  keyword.value = '';
  modalApi.open();
}

defineExpose({ open: openSelectModal });
</script>

<template>
  <div :class="compact ? 'space-y-1.5' : 'space-y-2.5'">
    <div
      v-if="!compact"
      class="flex items-center justify-between gap-3"
    >
      <div
        class="text-xs font-semibold uppercase tracking-[0.14em] text-muted-foreground"
      >
        {{ title ?? $t('components.relationPicker.title') }}
      </div>
      <NButton secondary size="small" @click="openSelectModal">
        <template #icon>
          <component :is="icon" class="size-3.5" />
        </template>
        {{ addText ?? $t('components.relationPicker.add') }}
      </NButton>
    </div>

    <button
      v-if="!compact"
      class="flex w-full items-center justify-between rounded-lg border border-dashed border-primary/35 bg-primary/5 px-3 py-2 text-left transition-colors hover:bg-primary/8"
      type="button"
      @click="openSelectModal"
    >
      <span class="inline-flex min-w-0 items-center gap-2">
        <component :is="icon" class="size-3.5 text-primary" />
        <span class="truncate text-xs text-muted-foreground">
          {{
            selectedOptions.length > 0
              ? $t('components.relationPicker.selectedCount', {
                  count: selectedOptions.length,
                })
              : (placeholder ?? $t('common.placeholder.pleaseSelect'))
          }}
        </span>
      </span>
      <Plus class="size-4 text-primary" />
    </button>

    <div
      v-if="selectedOptions.length > 0"
      class="space-y-1"
    >
      <div
        v-for="option in selectedOptions"
        :key="`selected-${option.value}`"
        class="group flex items-center justify-between gap-2 rounded-md border border-border/70 bg-muted/20 px-2.5 py-2"
      >
        <div class="min-w-0 flex-1">
          <div class="flex min-w-0 items-center gap-1.5">
            <component :is="icon" class="size-3.5 shrink-0 text-primary" />
            <div class="truncate text-xs font-medium text-foreground">
              {{ option.label }}
            </div>
          </div>
          <div
            v-if="option.description"
            class="mt-0.5 truncate text-xs leading-5 text-muted-foreground"
          >
            {{ option.description }}
          </div>
          <div
            v-if="(option.metrics || []).length > 0 || (option.tags || []).length > 0"
            class="mt-0.5 flex flex-wrap gap-1"
          >
            <span
              v-for="metric in option.metrics || []"
              :key="metric"
              class="rounded bg-primary/8 px-1 text-[10px] leading-4 text-primary"
            >
              {{ metric }}
            </span>
            <span
              v-for="tag in option.tags || []"
              :key="tag"
              class="rounded bg-muted px-1 text-[10px] leading-4 text-muted-foreground"
            >
              {{ tag }}
            </span>
          </div>
        </div>
        <button
          v-tippy="$t('components.relationPicker.remove')"
          class="shrink-0 rounded p-1 text-destructive/70 transition-colors hover:bg-destructive/10 hover:text-destructive"
          type="button"
          @click="toggleOption(option.value)"
        >
          <X class="size-3" />
        </button>
      </div>
    </div>

    <div
      v-else-if="compact"
      class="rounded-md bg-muted/25 px-2.5 py-2 text-xs leading-5 text-muted-foreground"
    >
      {{ $t('components.relationPicker.empty') }}
    </div>

    <Modal
      :title="modalTitle || $t('components.relationPicker.selectPrefix', { title: title ?? $t('components.relationPicker.title') })"
      class="w-[900px]"
      header-class="border-b"
    >
      <div class="space-y-3">
        <NInput
          v-model:value="keyword"
          clearable
          :placeholder="$t('components.relationPicker.searchPlaceholder')"
        />
        <div class="max-h-[60vh] overflow-y-auto pr-1">
          <div class="grid gap-2 sm:grid-cols-2 lg:grid-cols-3">
            <button
              v-for="option in filteredOptions"
              :key="option.value"
              :class="
                modelValue.includes(option.value)
                  ? 'border-primary/45 bg-primary/8'
                  : 'border-border bg-background hover:border-primary/35 hover:bg-primary/5'
              "
              class="rounded-md border px-2.5 py-2 text-left transition-colors"
              type="button"
              @click="toggleOption(option.value)"
            >
              <div class="flex items-start justify-between gap-2">
                <div class="min-w-0">
                  <div class="truncate text-xs font-medium text-foreground">
                    {{ option.label }}
                  </div>
                  <div
                    class="mt-0.5 line-clamp-2 text-[11px] leading-5 text-muted-foreground"
                  >
                    {{ option.description || $t('components.relationPicker.noDescription') }}
                  </div>
                </div>
                <CheckCircle2
                  v-if="modelValue.includes(option.value)"
                  class="size-4 shrink-0 text-primary"
                />
                <Circle v-else class="size-4 shrink-0 text-muted-foreground" />
              </div>
              <div class="mt-2 flex flex-wrap gap-1">
                <NTag
                  v-for="metric in option.metrics || []"
                  :key="metric"
                  :bordered="false"
                  size="small"
                  type="info"
                >
                  {{ metric }}
                </NTag>
                <NTag
                  v-for="tag in option.tags || []"
                  :key="tag"
                  :bordered="false"
                  size="small"
                >
                  {{ tag }}
                </NTag>
              </div>
            </button>
          </div>
          <div
            v-if="filteredOptions.length === 0"
            class="rounded-md border border-dashed border-border px-4 py-8 text-center text-xs text-muted-foreground"
          >
            {{ $t('components.relationPicker.noOptions') }}
          </div>
        </div>
      </div>
      <template #footer>
        <div class="flex w-full justify-end">
          <NButton type="primary" @click="modalApi.close()">
            {{ $t('common.actions.done') }}
          </NButton>
        </div>
      </template>
    </Modal>
  </div>
</template>
