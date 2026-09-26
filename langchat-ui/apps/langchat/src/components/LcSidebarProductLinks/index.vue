<script lang="ts" setup>
import { computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import { Info, SvgGithubIcon } from '@vben/icons';
import { openWindow } from '@vben/utils';

import { LANGCHAT_PRODUCT_LINKS } from '#/constants/product';

defineOptions({ name: 'LcSidebarProductLinks' });

defineProps<{
  collapsed?: boolean;
}>();

const router = useRouter();
const route = useRoute();
const isAboutActive = computed(
  () => route.path === '/about' || route.path.startsWith('/about/'),
);

function openAbout() {
  void router.push({ path: '/about' });
}

function openSourceRepository() {
  openWindow(LANGCHAT_PRODUCT_LINKS.github, { target: '_blank' });
}
</script>

<template>
  <div class="min-w-0">
    <div
      v-if="!collapsed"
      class="mb-1 px-2 text-[10px] font-medium uppercase tracking-[0.14em] text-muted-foreground/70"
    >
      产品与开源
    </div>
    <div class="flex flex-col gap-0.5">
      <button
        :aria-current="isAboutActive ? 'page' : undefined"
        class="group flex h-9 w-full cursor-pointer items-center rounded-md text-sm transition-colors"
        :class="[
          collapsed ? 'justify-center px-0' : 'gap-2.5 px-2',
          isAboutActive
            ? 'bg-primary font-semibold text-primary-foreground dark:bg-accent dark:text-foreground'
            : 'text-muted-foreground hover:bg-accent hover:text-accent-foreground',
        ]"
        title="关于 LangChat"
        type="button"
        @click="openAbout"
      >
        <Info class="size-4 shrink-0" />
        <span v-if="!collapsed" class="truncate">关于</span>
      </button>
      <button
        class="group flex h-9 w-full cursor-pointer items-center rounded-md text-sm text-muted-foreground transition-colors hover:bg-accent hover:text-accent-foreground"
        :class="collapsed ? 'justify-center px-0' : 'gap-2.5 px-2'"
        title="访问 LangChat GitHub 开源仓库"
        type="button"
        @click="openSourceRepository"
      >
        <SvgGithubIcon class="size-4 shrink-0" />
        <span v-if="!collapsed" class="truncate">开源地址</span>
      </button>
    </div>
  </div>
</template>
