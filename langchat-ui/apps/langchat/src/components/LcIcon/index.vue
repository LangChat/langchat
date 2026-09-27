<script lang="ts" setup>
import { computed, ref, watch } from 'vue';
import * as LucideIcons from '@vben/icons';
import { Box, Pencil } from '@vben/icons';
import { NInput, NPagination, NPopover, NTabPane, NTabs } from 'naive-ui';
import LcIconDisplay from './display.vue';

interface Props {
  editable?: boolean;
  fallbackIcon?: string;
  showSvgTab?: boolean;
  showUrlTab?: boolean;
  size?: number;
}

const props = withDefaults(defineProps<Props>(), {
  editable: false,
  fallbackIcon: 'lucide:box',
  showSvgTab: true,
  showUrlTab: true,
  size: 56,
});

const iconValue = defineModel<string>({ default: '' });
const keyword = ref('');
const page = ref(1);
const PAGE_SIZE = 100;

const iconMap = LucideIcons as Record<string, unknown>;

function toKebabCase(value: string) {
  return value
    .replace(/([a-z0-9])([A-Z])/g, '$1-$2')
    .replace(/([A-Z])([A-Z][a-z])/g, '$1-$2')
    .toLowerCase();
}

function isLucideComponentExport(key: string, value: unknown) {
  if (!/^[A-Z]/.test(key)) {
    return false;
  }
  if (
    key === 'Icon' ||
    key.startsWith('Svg') ||
    key === 'IconDefault' ||
    key === 'MdiMenuClose' ||
    key === 'MdiMenuOpen'
  ) {
    return false;
  }
  return typeof value === 'function' || typeof value === 'object';
}

const builtinIconOptions = computed(() =>
  Array.from(
    new Set(
      Object.entries(iconMap)
        .filter(([key, value]) => isLucideComponentExport(key, value))
        .map(([key]) => `lucide:${toKebabCase(key)}`)
    ),
  ).sort((a, b) => a.localeCompare(b)),
);

const filteredBuiltinIconOptions = computed(() => {
  const query = keyword.value.trim().toLowerCase();
  if (!query) {
    return builtinIconOptions.value;
  }
  return builtinIconOptions.value.filter((item) =>
    item.toLowerCase().includes(query),
  );
});

const pagedBuiltinIconOptions = computed(() => {
  const start = (page.value - 1) * PAGE_SIZE;
  return filteredBuiltinIconOptions.value.slice(start, start + PAGE_SIZE);
});

const resolvedIcon = computed(() => {
  const value = String(iconValue.value || '').trim();
  if (!value) {
    return props.fallbackIcon;
  }
  return value;
});

const sizeStyle = computed(() => ({
  height: `${props.size}px`,
  width: `${props.size}px`,
}));

const isImageUrl = computed(() => /^https?:\/\//.test(resolvedIcon.value));
const isInlineSvg = computed(
  () =>
    resolvedIcon.value.startsWith('<svg') &&
    resolvedIcon.value.endsWith('</svg>'),
);

function toPascalCase(value: string) {
  return value
    .split('-')
    .filter(Boolean)
    .map((segment) => `${segment.charAt(0).toUpperCase()}${segment.slice(1)}`)
    .join('');
}

const resolvedLucideComponent = computed(() => {
  const icon = String(resolvedIcon.value || '').trim();
  if (!icon.startsWith('lucide:')) {
    return Box;
  }
  const key = toPascalCase(icon.replace('lucide:', ''));
  if (!key || key === 'Icon') {
    return Box;
  }
  return (LucideIcons as Record<string, any>)[key] || Box;
});

function resolveBuiltinLucideComponent(icon: string) {
  const key = toPascalCase(icon.replace('lucide:', ''));
  if (key === 'Icon') {
    return Box;
  }
  return (LucideIcons as Record<string, any>)[key] || Box;
}

function useBuiltinIcon(icon: string) {
  iconValue.value = icon;
}

watch(keyword, () => {
  page.value = 1;
});
</script>

<template>
  <NPopover
    v-if="editable"
    trigger="click"
    placement="bottom-start"
    :show-arrow="false"
  >
    <template #trigger>
      <button
        class="group relative inline-flex items-center justify-center overflow-hidden rounded-lg border border-border bg-background transition-colors hover:border-primary/40"
        :style="sizeStyle"
        type="button"
      >
        <template v-if="isImageUrl">
          <img :src="resolvedIcon" alt="icon" class="size-full object-cover" />
        </template>
        <template v-else-if="isInlineSvg">
          <span class="size-[85%]" v-html="resolvedIcon"></span>
        </template>
        <template v-else>
          <component :is="resolvedLucideComponent" class="size-[78%] text-primary" />
        </template>
        <span
          class="absolute inset-0 flex items-center justify-center bg-black/35 text-white opacity-0 transition-opacity group-hover:opacity-100"
        >
          <Pencil class="size-4" />
        </span>
      </button>
    </template>

    <div class="w-[440px] max-w-[440px] p-1">
      <NTabs type="line" animated>
        <NTabPane name="builtin" :tab="$t('components.iconPicker.builtinTab')">
          <div class="mb-2">
            <NInput
              v-model:value="keyword"
              clearable
              :placeholder="$t('components.iconPicker.searchPlaceholder')"
            />
          </div>
          <div class="grid max-h-[240px] grid-cols-9 gap-1.5 overflow-y-auto pr-1">
            <button
              v-for="icon in pagedBuiltinIconOptions"
              :key="icon"
              :class="
                resolvedIcon === icon
                  ? 'border-primary bg-primary/10 text-primary'
                  : 'border-border bg-background text-muted-foreground hover:border-primary/40 hover:text-primary'
              "
              class="inline-flex h-8 items-center justify-center rounded-md border transition-colors"
              type="button"
              @click="useBuiltinIcon(icon)"
            >
              <component
                :is="resolveBuiltinLucideComponent(icon)"
                class="size-3.5"
              />
            </button>
          </div>
          <div class="mt-2 flex justify-end">
            <NPagination
              v-model:page="page"
              :item-count="filteredBuiltinIconOptions.length"
              :page-size="PAGE_SIZE"
              simple
            />
          </div>
        </NTabPane>
        <NTabPane v-if="showSvgTab" name="svg" :tab="$t('components.iconPicker.svgTab')">
          <NInput
            v-model:value="iconValue"
            :autosize="{ minRows: 8, maxRows: 14 }"
            placeholder="<svg ...>...</svg>"
            type="textarea"
          />
        </NTabPane>
        <NTabPane v-if="showUrlTab" name="url" :tab="$t('components.iconPicker.urlTab')">
          <NInput
            v-model:value="iconValue"
            placeholder="https://example.com/icon.png"
          />
        </NTabPane>
      </NTabs>
    </div>
  </NPopover>

  <span
    v-else
    class="inline-flex"
  >
    <LcIconDisplay
      :fallback-icon="fallbackIcon"
      :icon="resolvedIcon"
      :size="size"
    />
  </span>
</template>
