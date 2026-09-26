<script lang="ts" setup>
interface Props {
  duration?: number;
}
const props = withDefaults(defineProps<Props>(), {
  duration: 2,
});
</script>

<template>
  <span
    class="animate-shimmer inline-flex bg-[length:200%_100%] bg-clip-text text-transparent bg-[linear-gradient(110deg,var(--tw-gradient-stops))] from-muted-foreground/40 via-muted-foreground/80 to-muted-foreground/40 dark:from-muted-foreground/40 dark:to-muted-foreground/40 dark:via-white/80"
    :class="[$attrs.class]"
    :style="{ animationDuration: `${duration}s` }"
  >
    <slot></slot>
  </span>
</template>

<style scoped>
@keyframes shimmer {
  0% {
    background-position: 200% 0;
  }

  100% {
    background-position: -200% 0;
  }
}

.animate-shimmer {
  animation: shimmer infinite linear;
}
</style>
