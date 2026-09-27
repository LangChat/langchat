<script setup lang="ts">
import type { Component } from 'vue';
import { computed, useSlots } from 'vue';
import { LayoutGrid, Search, Tag } from '@vben/icons';
import { $t } from '@vben/locales';
import { NInput, NTag } from 'naive-ui';

import LcCard from '#/components/LcCard/index.vue';
import LcEmptyState from '#/components/LcEmptyState/index.vue';

interface ListAttribute {
  field: string;
  formatter?: (value: any, item: Record<string, any>) => string;
  label: string;
}

interface ListTagOption {
  label: string;
  value: string;
}

interface Props {
  activeTag?: string;
  allTagIcon?: Component;
  attributes?: ListAttribute[];
  cardDescriptionField?: string;
  cardIconField?: string;
  cardTitleField?: string;
  columnsClass?: string;
  commonTagIcon?: Component;
  emptyDescription?: string;
  emptyHint?: string;
  itemKeyField?: string;
  items?: Record<string, any>[];
  loading?: boolean;
  searchPlaceholder?: string;
  searchValue?: string;
  skeletonCount?: number;
  tags?: ListTagOption[];
}

const props = withDefaults(defineProps<Props>(), {
  activeTag: 'ALL',
  allTagIcon: () => LayoutGrid,
  attributes: () => [],
  cardDescriptionField: 'description',
  cardIconField: 'icon',
  cardTitleField: 'name',
  columnsClass:
    'grid gap-3 md:grid-cols-1 lg:grid-cols-2 xl:grid-cols-3 2xl:grid-cols-4',
  commonTagIcon: () => Tag,
  emptyDescription: undefined,
  emptyHint: '',
  itemKeyField: 'id',
  items: () => [],
  loading: false,
  searchPlaceholder: undefined,
  searchValue: '',
  skeletonCount: 8,
  tags: () => [],
});

const emit = defineEmits<{
  'update:active-tag': [value: string];
  'update:search-value': [value: string];
}>();
const slots = useSlots();

const normalizedTags = computed(() => {
  const tags = props.tags.some((item) => item.value === 'ALL')
    ? props.tags
    : [{ label: $t('common.labels.all'), value: 'ALL' }, ...props.tags];

  return tags.map((item) => ({
    ...item,
    icon: item.value === 'ALL' ? props.allTagIcon : props.commonTagIcon,
  }));
});

const normalizedItems = computed(() =>
  props.items.map((item, index) => {
    const rawKey = item[props.itemKeyField];
    const key =
      rawKey === null || rawKey === undefined || rawKey === ''
        ? `${index}`
        : String(rawKey);
    return {
      item,
      key,
    };
  }),
);
const hasLeadingCard = computed(() => Boolean(slots['leading-card']));

function resolveCardTitle(item: Record<string, any>) {
  return String(item[props.cardTitleField] || $t('common.labels.untitled'));
}

function resolveCardDescription(item: Record<string, any>) {
  return String(item[props.cardDescriptionField] || $t('common.empty.noDescription'));
}

function resolveCardIcon(item: Record<string, any>) {
  return String(item[props.cardIconField] || 'lucide:box');
}
</script>

<template>
  <div class="flex flex-col gap-2.5">
    <div
      class="flex flex-col gap-2 lg:flex-row lg:items-center lg:justify-between"
    >
      <div class="flex min-w-0 flex-wrap items-center gap-1.5">
        <NTag
          v-for="option in normalizedTags"
          :key="option.value"
          :bordered="activeTag !== option.value"
          :type="activeTag === option.value ? 'primary' : 'default'"
          class="lc-list-card-tag cursor-pointer rounded-lg"
          @click="emit('update:active-tag', String(option.value))"
        >
          <span class="inline-flex items-center gap-1">
            <component :is="option.icon" class="size-3" />
            <span>{{ option.label }}</span>
          </span>
        </NTag>
      </div>

      <div class="w-full sm:w-72">
        <NInput
          :value="searchValue"
          clearable
          :placeholder="searchPlaceholder ?? $t('components.listCard.searchPlaceholder')"
          @update:value="
            (value) => emit('update:search-value', String(value ?? ''))
          "
        >
          <template #prefix>
            <Search class="size-4 text-muted-foreground" />
          </template>
        </NInput>
      </div>
    </div>

    <div v-if="loading" :class="columnsClass">
      <slot name="leading-card"></slot>
      <LcCard
        v-for="index in skeletonCount"
        :key="index"
        loading
        :show-icon="false"
      />
    </div>

    <div
      v-else-if="normalizedItems.length > 0 || hasLeadingCard"
      class="flex flex-col gap-2.5"
    >
      <div :class="columnsClass">
        <slot name="leading-card"></slot>
        <template v-for="entry in normalizedItems" :key="entry.key">
          <slot name="item" :item="entry.item">
            <LcCard :icon="resolveCardIcon(entry.item)">
              <template #header>
                <div class="min-w-0">
                  <div
                    class="truncate text-[13px] font-semibold text-foreground"
                  >
                    {{ resolveCardTitle(entry.item) }}
                  </div>
                  <div
                    class="mt-0.5 truncate text-[10px] text-muted-foreground"
                  >
                    {{ resolveCardDescription(entry.item) }}
                  </div>
                </div>
              </template>
            </LcCard>
          </slot>
        </template>
      </div>

      <LcEmptyState
        v-if="normalizedItems.length === 0"
        :description="emptyHint"
        :title="emptyDescription ?? $t('common.empty.noData')"
      >
        <slot name="empty-extra"></slot>
      </LcEmptyState>
    </div>

    <LcEmptyState
      v-else
      :description="emptyHint"
      :title="emptyDescription ?? $t('common.empty.noData')"
    >
      <slot name="empty-extra"></slot>
    </LcEmptyState>
  </div>
</template>

<style scoped>
:deep(.lc-list-card-tag.n-tag) {
  padding: 1px 10px;
  cursor: pointer;
}

:deep(.lc-list-card-tag.n-tag:hover) {
  background-color: hsl(var(--primary) / 0.1);
}

:deep(.lc-list-card-tag .n-tag__content) {
  font-size: 10px;
  font-weight: 700;
}
</style>
