<script lang="ts" setup>
import { computed } from 'vue';

import * as LucideIcons from '@vben/icons';
import { Box } from '@vben/icons';

interface Props {
  fallbackIcon?: string;
  icon?: string;
  size?: number;
}

const props = withDefaults(defineProps<Props>(), {
  fallbackIcon: 'lucide:box',
  icon: '',
  size: 42,
});

function toPascalCase(value: string) {
  return value
    .split('-')
    .filter(Boolean)
    .map((segment) => `${segment.charAt(0).toUpperCase()}${segment.slice(1)}`)
    .join('');
}

function isBlockedLucideExport(name: string) {
  return (
    !name ||
    name === 'Icon' ||
    name === 'IconDefault' ||
    name === 'MdiMenuClose' ||
    name === 'MdiMenuOpen' ||
    name.startsWith('Svg')
  );
}

const resolvedIcon = computed(() => {
  const value = String(props.icon || '').trim();
  if (!value) {
    return props.fallbackIcon;
  }
  return value;
});

const isImageUrl = computed(() =>
  /^(?:https?:\/\/|\/|data:image\/)/.test(resolvedIcon.value),
);
const isInlineSvg = computed(
  () =>
    resolvedIcon.value.startsWith('<svg') &&
    resolvedIcon.value.endsWith('</svg>'),
);

const resolvedLucideComponent = computed(() => {
  const icon = String(resolvedIcon.value || '').trim();
  if (!icon.startsWith('lucide:')) {
    return null;
  }
  const iconName = icon.replace('lucide:', '');
  const key = toPascalCase(iconName);
  if (isBlockedLucideExport(key)) {
    return Box;
  }
  const target = (LucideIcons as Record<string, any>)[key];
  return target || Box;
});

const sizeStyle = computed(() => ({
  height: `${props.size}px`,
  width: `${props.size}px`,
}));
</script>

<template>
  <span
    :class="isImageUrl ? 'bg-white' : 'bg-background'"
    class="inline-flex items-center justify-center overflow-hidden rounded-lg border border-border"
    :style="sizeStyle"
  >
    <template v-if="isImageUrl">
      <img :src="resolvedIcon" alt="icon" class="size-[75%] object-contain" />
    </template>
    <template v-else-if="isInlineSvg">
      <span class="size-[75%]" v-html="resolvedIcon"></span>
    </template>
    <template v-else-if="resolvedLucideComponent">
      <component
        :is="resolvedLucideComponent"
        class="size-[75%] text-primary"
      />
    </template>
    <template v-else>
      <Box class="size-[75%] text-primary" />
    </template>
  </span>
</template>
