<script lang="ts" setup>
/**
 * LcChat 会话流中的内联状态行(参考 shadcn-vue Marker):
 * default 内联备注 / border 带下边框 / separator 两侧分割线的居中标签。
 */
interface Props {
  variant?: 'border' | 'default' | 'separator';
}

withDefaults(defineProps<Props>(), {
  variant: 'default',
});
</script>

<template>
  <div :class="[`chat-marker--${variant}`]" class="chat-marker">
    <span aria-hidden="true" class="chat-marker__icon">
      <slot name="icon"></slot>
    </span>
    <span class="chat-marker__content">
      <slot></slot>
    </span>
  </div>
</template>

<style scoped>
.chat-marker {
  display: flex;
  gap: 8px;
  align-items: center;
  font-size: 12px;
  color: hsl(var(--muted-foreground));
}

.chat-marker--border {
  padding-bottom: 8px;
  border-bottom: 1px solid hsl(var(--border));
}

.chat-marker--separator {
  justify-content: center;
}

.chat-marker--separator::before,
.chat-marker--separator::after {
  flex: 1;
  height: 1px;
  content: '';
  background: hsl(var(--border));
}

.chat-marker__icon {
  display: inline-flex;
  flex-shrink: 0;
}

.chat-marker__content {
  min-width: 0;
}
</style>
