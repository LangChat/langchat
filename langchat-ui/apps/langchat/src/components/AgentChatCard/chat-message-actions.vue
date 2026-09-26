<script lang="ts" setup>
import {Copy, Square, Volume2} from '@vben/icons';

interface Props {
  speaking?: boolean;
}

withDefaults(defineProps<Props>(), {
  speaking: false,
});

const emit = defineEmits<{
  copy: [];
  speak: [];
}>();
</script>

<template>
  <div class="flex gap-1">
    <button
      v-tippy="'复制'"
      class="inline-flex hover:bg-accent cursor-pointer items-center justify-center p-1 text-muted-foreground transition-colors hover:border-primary/35 hover:text-primary"
      type="button"
      @click="emit('copy')"
    >
      <Copy class="size-3" />
    </button>
    <button
      v-tippy="speaking ? '停止朗读' : '朗读'"
      class="inline-flex hover:bg-accent cursor-pointer items-center justify-center p-1 text-muted-foreground transition-colors hover:border-primary/35 hover:text-primary"
      type="button"
      @click="emit('speak')"
    >
      <Square v-if="speaking" class="size-3" />
      <Volume2 v-else class="size-3" />
    </button>
  </div>
</template>
