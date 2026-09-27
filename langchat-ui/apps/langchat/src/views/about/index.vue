<script lang="ts" setup>
import { computed } from 'vue';

import { Page } from '@vben/common-ui';
import { ExternalLink, Globe, SvgGithubIcon } from '@vben/icons';
import { $t } from '@vben/locales';
import { preferences, usePreferences } from '@vben/preferences';

import LcCard from '#/components/LcCard/index.vue';
import {
  LANGCHAT_PRODUCT_LINKS,
  LANGCHAT_PRODUCT_NAME,
  LANGCHAT_PRODUCT_TEAM,
} from '#/constants/product';

import LangChatArchitecture from './components/langchat-architecture.vue';

const { isDark } = usePreferences();
const appName = computed(() => preferences.app.name);
const logoSrc = computed(() =>
  isDark.value && preferences.logo.sourceDark
    ? preferences.logo.sourceDark
    : preferences.logo.source,
);

const productHighlights = computed(() => [
  {
    description: $t('about.features.models.description'),
    label: $t('about.features.models.label'),
  },
  {
    description: $t('about.features.knowledge.description'),
    label: $t('about.features.knowledge.label'),
  },
  {
    description: $t('about.features.orchestration.description'),
    label: $t('about.features.orchestration.label'),
  },
  {
    description: $t('about.features.streaming.description'),
    label: $t('about.features.streaming.label'),
  },
]);

const techStacks = computed(() => [
  {
    label: $t('about.stack.frontend.label'),
    value: $t('about.stack.frontend.value'),
  },
  {
    label: $t('about.stack.backend.label'),
    value: $t('about.stack.backend.value'),
  },
  {
    label: $t('about.stack.runtime.label'),
    value: $t('about.stack.runtime.value'),
  },
  {
    label: $t('about.stack.storage.label'),
    value: $t('about.stack.storage.value'),
  },
]);
</script>

<template>
  <Page>
    <div class="flex flex-col gap-4">
      <section
        class="rounded-xl border border-border bg-card px-4 py-5 sm:px-6"
      >
        <div
          class="grid gap-5 border-b border-border pb-5 xl:grid-cols-[minmax(0,1fr)_360px] xl:items-center"
        >
          <div class="min-w-0">
            <div class="flex items-center gap-2.5">
              <div
                class="flex size-9 shrink-0 items-center justify-center overflow-hidden rounded-lg border border-primary/20 bg-primary/10"
              >
                <img
                  v-if="logoSrc"
                  :alt="appName"
                  :src="logoSrc"
                  class="size-6 object-contain"
                />
                <span v-else class="text-xs font-bold text-primary">LC</span>
              </div>
              <span class="text-xs font-medium text-muted-foreground">
                {{ LANGCHAT_PRODUCT_TEAM }} · {{ $t('about.hero.openSourceLabel') }}
              </span>
            </div>
            <div class="mt-3 flex flex-wrap items-center gap-2">
              <h1 class="text-balance text-xl font-semibold text-foreground">
                {{
                  $t('about.hero.title', { name: LANGCHAT_PRODUCT_NAME })
                }}
              </h1>
              <span
                class="rounded-md border border-primary/20 bg-primary/5 px-2 py-0.5 text-[10px] font-semibold text-primary"
              >
                {{ $t('market.openSourceLabel') }}
              </span>
            </div>
            <p
              class="mt-3 max-w-4xl text-pretty text-sm leading-6 text-muted-foreground"
            >
              {{ $t('about.hero.introFirst') }}
            </p>
            <p
              class="mt-1.5 max-w-4xl text-pretty text-xs leading-5 text-muted-foreground"
            >
              {{ $t('about.hero.introSecond') }}
            </p>
          </div>

          <div class="shrink-0">
            <div
              class="mb-2 text-[10px] font-medium text-muted-foreground"
            >
              {{ $t('about.links.title') }}
            </div>
            <div class="grid grid-cols-2 gap-2 sm:grid-cols-4 xl:grid-cols-2">
              <a
                class="group flex h-9 items-center gap-2 rounded-lg border border-border bg-background/70 px-3 text-xs font-medium text-foreground transition-colors hover:border-primary/30 hover:bg-primary/5 hover:text-primary"
                :href="LANGCHAT_PRODUCT_LINKS.github"
                rel="noopener"
                target="_blank"
              >
                <SvgGithubIcon class="size-3.5 shrink-0" />
                GitHub
                <ExternalLink
                  class="ml-auto size-3 text-muted-foreground transition-colors group-hover:text-primary"
                />
              </a>
              <a
                class="group flex h-9 items-center gap-2 rounded-lg border border-border bg-background/70 px-3 text-xs font-medium text-foreground transition-colors hover:border-primary/30 hover:bg-primary/5 hover:text-primary"
                :href="LANGCHAT_PRODUCT_LINKS.gitee"
                rel="noopener"
                target="_blank"
              >
                <span class="font-semibold text-[#c71d23]">G</span>
                Gitee
                <ExternalLink
                  class="ml-auto size-3 text-muted-foreground transition-colors group-hover:text-primary"
                />
              </a>
              <a
                class="group flex h-9 items-center gap-2 rounded-lg border border-border bg-background/70 px-3 text-xs font-medium text-foreground transition-colors hover:border-primary/30 hover:bg-primary/5 hover:text-primary"
                :href="LANGCHAT_PRODUCT_LINKS.gitcode"
                rel="noopener"
                target="_blank"
              >
                <span class="font-semibold text-primary">GC</span>
                GitCode
                <ExternalLink
                  class="ml-auto size-3 text-muted-foreground transition-colors group-hover:text-primary"
                />
              </a>
              <a
                class="group flex h-9 items-center gap-2 rounded-lg border border-border bg-background/70 px-3 text-xs font-medium text-foreground transition-colors hover:border-primary/30 hover:bg-primary/5 hover:text-primary"
                :href="LANGCHAT_PRODUCT_LINKS.website"
                rel="noopener"
                target="_blank"
              >
                <Globe class="size-3.5 shrink-0" />
                {{ $t('about.links.website') }}
                <ExternalLink
                  class="ml-auto size-3 text-muted-foreground transition-colors group-hover:text-primary"
                />
              </a>
            </div>
          </div>
        </div>

        <div class="mt-4 grid gap-2 sm:grid-cols-2 xl:grid-cols-4">
          <div
            v-for="item in productHighlights"
            :key="item.label"
            class="rounded-lg border border-border bg-background/60 px-3 py-2.5"
          >
            <div class="text-xs font-semibold text-foreground">
              {{ item.label }}
            </div>
            <div class="mt-1 text-[11px] leading-4 text-muted-foreground">
              {{ item.description }}
            </div>
          </div>
        </div>
      </section>

      <LcCard :hoverable="false" :show-icon="false">
        <template #header>
          <div>
            <div class="text-sm font-semibold text-foreground">
              {{ $t('about.architecture.title') }}
            </div>
            <div class="mt-1 text-xs font-normal text-muted-foreground">
              {{ $t('about.architecture.description') }}
            </div>
          </div>
        </template>
        <LangChatArchitecture />
      </LcCard>

      <LcCard :hoverable="false" :show-icon="false">
        <template #header>
          <div class="text-sm font-semibold text-foreground">
            {{ $t('about.stack.title') }}
          </div>
        </template>
        <div class="grid gap-2 sm:grid-cols-2 xl:grid-cols-4">
          <div
            v-for="item in techStacks"
            :key="item.label"
            class="rounded-lg border border-border bg-muted/20 px-3 py-2.5"
          >
            <div class="text-xs font-medium text-foreground">
              {{ item.label }}
            </div>
            <div class="mt-1 text-xs leading-5 text-muted-foreground">
              {{ item.value }}
            </div>
          </div>
        </div>
      </LcCard>
    </div>
  </Page>
</template>
