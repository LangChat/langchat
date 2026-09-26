<!--
  - © 2024-present LangChat团队. 版权所有.
  -->
<script lang="ts" setup>
import type { Component } from 'vue';

import { computed } from 'vue';

import { NSkeleton } from 'naive-ui';

import LcIconDisplay from '#/components/LcIcon/display.vue';
import LcIcon from '#/components/LcIcon/index.vue';

interface Props {
  /** 是否显示骨架屏 */
  loading?: boolean;
  /** 是否开启悬停效果与边框装饰 */
  hoverable?: boolean;
  /** 自定义高度 */
  height?: number | string;
  /** 卡片图标 */
  icon?: string;
  /** 卡片图标组件 */
  iconComponent?: Component | null;
  /** 卡片图标兜底 */
  defaultIcon?: string;
  /** 是否可编辑图标 */
  editableIcon?: boolean;
  /** 图标尺寸 */
  iconSize?: number;
  /** 是否显示图标 */
  showIcon?: boolean;
  /** 卡片指标项 */
  metaItems?: Array<{
    label: string;
    tone?: 'danger' | 'info' | 'primary' | 'success' | 'warning';
    value: number | string;
  }>;
}

const props = withDefaults(defineProps<Props>(), {
  loading: false,
  hoverable: true,
  icon: '',
  iconComponent: null,
  defaultIcon: 'lucide:box',
  editableIcon: false,
  height: undefined,
  iconSize: 36,
  showIcon: true,
  metaItems: () => [],
});
const emit = defineEmits<{
  'update:icon': [value: string];
}>();

const cardStyle = computed(() => {
  if (!props.height) return {};
  const h = String(props.height);
  const value = /^\d+$/.test(h) ? `${h}px` : h;
  return {
    height: value,
    minHeight: value,
    maxHeight: value,
  };
});
const iconValue = computed({
  get: () => props.icon || '',
  set: (value: string) => emit('update:icon', value),
});

const metaTones = ['primary', 'success', 'warning', 'info'] as const;

function resolveMetaTone(index: number, tone?: string) {
  return tone || metaTones[index % metaTones.length];
}
</script>

<template>
  <!-- 骨架屏展示 -->
  <div
    v-if="loading"
    :style="cardStyle"
    class="lc-card overflow-hidden rounded-lg border border-border bg-card px-3 pb-2 pt-3"
  >
    <div class="flex h-full flex-col gap-2.5">
      <!-- 头部骨架 -->
      <div class="flex items-start justify-between">
        <slot name="skeleton-header">
          <div class="flex items-center gap-2.5">
            <NSkeleton
              :sharp="false"
              class="rounded-lg"
              height="36px"
              width="36px"
            />
            <div class="flex flex-col gap-1">
              <NSkeleton :sharp="false" height="14px" width="92px" />
              <NSkeleton :sharp="false" height="10px" width="60px" />
            </div>
          </div>
          <NSkeleton :sharp="false" height="18px" width="40px" />
        </slot>
      </div>

      <!-- 主体内容骨架 -->
      <slot name="skeleton-content">
        <NSkeleton :sharp="false" height="12px" width="72%" />
        <div class="grid grid-cols-2 gap-2">
          <NSkeleton :sharp="false" height="34px" />
          <NSkeleton :sharp="false" height="34px" />
        </div>
      </slot>

      <!-- 底部骨架 -->
      <div
        class="mt-auto flex items-center justify-between border-t border-border pt-2"
      >
        <slot name="skeleton-footer">
          <NSkeleton :sharp="false" height="12px" width="45px" />
          <div class="flex gap-1">
            <NSkeleton circle height="20px" width="20px" />
            <NSkeleton circle height="20px" width="20px" />
          </div>
        </slot>
      </div>
    </div>
  </div>

  <!-- 真实内容展示 -->
  <div
    v-else
    :class="[
      hoverable
        ? 'cursor-pointer hover:border-primary/50 hover:bg-primary/5'
        : '',
    ]"
    :style="cardStyle"
    class="lc-card group overflow-hidden rounded-lg border border-border bg-card px-3 pb-2 pt-3 transition-colors duration-200"
  >
    <div class="flex h-full flex-col gap-2.5">
      <!-- 头部区域 -->
      <div
        v-if="
          $slots.header ||
          $slots.description ||
          $slots['header-extra'] ||
          showIcon
        "
        class="flex min-w-0 items-start gap-2.5"
      >
        <div v-if="showIcon" class="lc-card-icon-host shrink-0">
          <slot name="icon">
            <span
              v-if="iconComponent"
              :style="{ width: `${iconSize}px`, height: `${iconSize}px` }"
              class="lc-card-icon-shell"
            >
              <component :is="iconComponent" class="lc-card-icon-svg" />
            </span>
            <LcIcon
              v-else-if="editableIcon"
              v-model="iconValue"
              :editable="editableIcon"
              :fallback-icon="defaultIcon"
              :size="iconSize"
              class="lc-card-icon-render"
            />
            <LcIconDisplay
              v-else
              :fallback-icon="defaultIcon"
              :icon="iconValue"
              :size="iconSize"
              class="lc-card-icon-render"
            />
          </slot>
        </div>
        <div class="min-w-0 flex-1">
          <div class="flex min-w-0 items-start justify-between gap-3">
            <div class="min-w-0 flex-1">
              <slot name="header"></slot>
            </div>
            <div
              class="lc-card-header-extra flex shrink-0 items-start self-start"
            >
              <slot name="header-extra"></slot>
            </div>
          </div>
          <div v-if="$slots.description" class="lc-card-description mt-1">
            <slot name="description"></slot>
          </div>
        </div>
      </div>

      <!-- 主体内容 -->
      <div class="flex min-w-0 flex-1 flex-col gap-2">
        <div v-if="metaItems.length > 0" class="lc-card-meta-grid">
          <div
            v-for="(meta, index) in metaItems"
            :key="`${meta.label}-${index}`"
            :class="`lc-card-meta-item lc-card-meta-item--${resolveMetaTone(index, meta.tone)}`"
          >
            <span class="lc-card-meta-marker"></span>
            <span class="lc-card-meta-copy">
              <span class="lc-card-meta-label">{{ meta.label }}</span>
              <span class="lc-card-meta-value" :title="String(meta.value)">
                {{ meta.value }}
              </span>
            </span>
          </div>
        </div>
        <slot></slot>
      </div>

      <!-- 底部区域 -->
      <div
        v-if="$slots.footer || $slots['footer-extra']"
        class="mt-auto flex items-center justify-between gap-3 border-t border-border pt-2"
      >
        <div class="flex min-w-0 flex-1 items-center gap-1">
          <div class="min-w-0 flex-1 truncate whitespace-nowrap">
            <slot name="footer-extra"></slot>
          </div>
        </div>
        <div class="flex shrink-0 items-center gap-0.5">
          <slot name="footer"></slot>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.lc-card-icon-shell {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  background: hsl(var(--background) / 75%);
  border: 1px var(--border-style, solid) hsl(var(--border));
  border-radius: var(--radius);
}

.lc-card-icon-svg {
  width: 58%;
  height: 58%;
  color: hsl(var(--primary));
}

.lc-card-icon-render :deep(svg) {
  width: 60% !important;
  height: 60% !important;
}

.lc-card-description {
  min-width: 0;
  overflow: hidden;
}

.lc-card-description :deep(*) {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  line-height: 1.55;
  white-space: nowrap;
}

.lc-card-meta-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.375rem 0.5rem;
}

.lc-card-meta-item {
  --lc-card-meta-color: hsl(var(--primary));

  display: flex;
  gap: 0.375rem;
  align-items: stretch;
  min-width: 0;
  padding: 0.325rem 0.5rem;
  background: hsl(var(--muted) / 25%);
  border-radius: calc(var(--radius) * 0.75);
}

.lc-card-meta-item--success {
  --lc-card-meta-color: hsl(var(--success));
}

.lc-card-meta-item--warning {
  --lc-card-meta-color: hsl(var(--warning));
}

.lc-card-meta-item--info {
  --lc-card-meta-color: hsl(199deg 89% 48%);
}

.lc-card-meta-item--danger {
  --lc-card-meta-color: hsl(var(--destructive));
}

.lc-card-meta-marker {
  flex: 0 0 2px;
  align-self: stretch;
  width: 2px;
  background: var(--lc-card-meta-color);
  border-radius: var(--radius);
  opacity: 0.55;
}

.lc-card-meta-copy {
  display: flex;
  flex: 1;
  gap: 0.5rem;
  align-items: baseline;
  justify-content: space-between;
  min-width: 0;
}

.lc-card-meta-label {
  flex: 0 0 auto;
  font-size: 0.625rem;
  line-height: 1.125rem;
  color: hsl(var(--muted-foreground) / 85%);
}

.lc-card-meta-value {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  font-size: 0.6875rem;
  font-weight: 500;
  line-height: 1.125rem;
  color: hsl(var(--foreground) / 82%);
  text-align: right;
  white-space: nowrap;
}

.lc-card-header-extra :deep(.iconify),
.lc-card-header-extra :deep(svg) {
  flex: 0 0 14px;
  width: 14px;
  height: 14px;
}

.lc-card-header-extra :deep(.n-tag .n-tag__content) {
  font-size: 10px;
  font-weight: 500;
  text-transform: none;
  letter-spacing: 0;
}

@media (width < 420px) {
  .lc-card-meta-grid {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
