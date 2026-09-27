<script lang="ts" setup>
import type { AigcAgent } from '#/api/aigc/agent';

import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';

import { Page } from '@vben/common-ui';
import {
  ArrowRight,
  Bot,
  Search,
  SvgGithubIcon,
} from '@vben/icons';
import { $t } from '@vben/locales';
import { preferences, usePreferences } from '@vben/preferences';

import { NInput, NSpin } from 'naive-ui';

import { message } from '#/adapter/naive';
import { agentApi } from '#/api/aigc/agent';
import {
  LANGCHAT_PRODUCT_LINKS,
  LANGCHAT_PRODUCT_TEAM,
} from '#/constants/product';
import { useAigcLookups } from '#/views/shared/aigc/lookups';

import MarketCard from './card.vue';

const { loadLookups, lookups } = useAigcLookups({
  knowledges: true,
  models: true,
  skills: true,
});

const router = useRouter();
const { isDark } = usePreferences();
const appName = computed(() => preferences.app.name);
const logoSrc = computed(() =>
  isDark.value && preferences.logo.sourceDark
    ? preferences.logo.sourceDark
    : preferences.logo.source,
);
const loading = ref(false);
const keyword = ref('');
const items = ref<AigcAgent[]>([]);

const publishedAgents = computed(() =>
  items.value.filter(
    (item) => String(item.status || '').toUpperCase() === 'PUBLISHED',
  ),
);

const filteredPublishedAgents = computed(() => {
  const query = keyword.value.trim().toLowerCase();
  if (!query) {
    return publishedAgents.value;
  }
  return publishedAgents.value.filter((item) =>
    [item.agentName, item.description, item.tags]
      .map((value) => String(value ?? '').toLowerCase())
      .some((value) => value.includes(query)),
  );
});

async function loadList() {
  loading.value = true;
  try {
    items.value = await agentApi.list();
  } finally {
    loading.value = false;
  }
}

function openRoute(path: string) {
  void router.push(path);
}

function enterMarketAgent(item: AigcAgent) {
  const id = String(item.id || '');
  if (!id) {
    message.error($t('market.missingId'));
    return;
  }
  void router.push(`/market/${id}/chat`);
}

async function initializePage() {
  await Promise.all([loadLookups(), loadList()]);
}

onMounted(() => {
  void initializePage();
});
</script>

<template>
  <Page>
    <div class="market-page flex flex-col gap-4">
      <section
        class="market-hero relative overflow-hidden rounded-lg border border-border bg-card"
      >
        <div class="market-grid pointer-events-none absolute inset-0"></div>
        <div
          class="market-glow pointer-events-none absolute -right-20 -top-28 size-80 rounded-full"
        ></div>

        <div
          class="relative flex flex-col gap-5 px-5 py-5 sm:px-6 lg:flex-row lg:items-center lg:justify-between"
        >
          <div class="flex min-w-0 items-start gap-3.5">
            <div
              class="flex size-11 shrink-0 items-center justify-center overflow-hidden rounded-xl border border-primary/20 bg-primary/10"
            >
              <img
                v-if="logoSrc"
                :alt="appName"
                :src="logoSrc"
                class="size-8 object-contain"
              />
              <Bot v-else class="size-5 text-primary" />
            </div>
            <div class="min-w-0">
              <div class="flex flex-wrap items-center gap-2">
                <h1
                  class="text-xl font-semibold tracking-tight text-foreground sm:text-2xl"
                >
                  {{ $t('market.title') }}
                </h1>
                <span
                  class="rounded-md border border-primary/20 bg-primary/5 px-2 py-0.5 text-[10px] font-semibold uppercase tracking-[0.12em] text-primary"
                >
                  {{ $t('market.openSourceLabel') }}
                </span>
              </div>
              <p class="mt-1.5 max-w-3xl text-sm leading-6 text-muted-foreground">
                {{ $t('market.description') }}
              </p>
              <div
                class="mt-2 flex flex-wrap items-center gap-x-3 gap-y-1 text-[11px] text-muted-foreground"
              >
                <span class="inline-flex items-center gap-1.5">
                  <span class="size-1.5 rounded-full bg-emerald-500"></span>
                  {{ $t('market.maintainer', { team: LANGCHAT_PRODUCT_TEAM }) }}
                </span>
                <span>{{
                  $t('market.publishedCount', {
                    count: publishedAgents.length,
                  })
                }}</span>
              </div>
            </div>
          </div>

          <div class="flex shrink-0 flex-wrap items-center gap-2">
            <a
              class="inline-flex h-9 items-center gap-2 rounded-lg border border-border bg-background/70 px-3 text-xs font-medium text-foreground transition-colors hover:border-primary/30 hover:bg-primary/5 hover:text-primary"
              :href="LANGCHAT_PRODUCT_LINKS.github"
              rel="noopener"
              target="_blank"
            >
              <SvgGithubIcon class="size-3.5" />
              {{ $t('market.openSourceRepo') }}
            </a>
            <button
              class="inline-flex h-9 cursor-pointer items-center gap-1.5 rounded-lg bg-primary px-3 text-xs font-medium text-primary-foreground transition-opacity hover:opacity-90"
              type="button"
              @click="openRoute('/about')"
            >
              {{ $t('market.aboutProject') }}
              <ArrowRight class="size-3.5" />
            </button>
          </div>
        </div>
      </section>

      <section class="rounded-xl border border-border bg-card px-4 py-4 sm:px-5">
        <div
          class="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between"
        >
          <div>
            <div class="flex items-center gap-2">
              <h2 class="text-base font-semibold text-foreground">
                {{ $t('market.plazaTitle') }}
              </h2>
              <span
                class="rounded-md bg-muted px-2 py-0.5 text-[10px] font-medium text-muted-foreground"
              >
                {{
                  $t('market.plazaCount', { count: publishedAgents.length })
                }}
              </span>
            </div>
            <p class="mt-1 text-xs text-muted-foreground">
              {{ $t('market.plazaDescription') }}
            </p>
          </div>

          <NInput
            v-model:value="keyword"
            class="market-search w-full lg:w-[420px]"
            clearable
            :placeholder="$t('market.searchPlaceholder')"
          >
            <template #prefix>
              <Search class="size-4 text-muted-foreground" />
            </template>
          </NInput>
        </div>
      </section>

      <NSpin :show="loading" class="min-h-[420px] w-full">
        <div
          v-if="filteredPublishedAgents.length > 0"
          class="grid w-full gap-3 md:grid-cols-1 lg:grid-cols-2 xl:grid-cols-3 2xl:grid-cols-4"
        >
          <MarketCard
            v-for="item in filteredPublishedAgents"
            :key="item.id || item.agentName"
            :item="item"
            :knowledge-options="lookups.knowledges"
            :model-options="lookups.models"
            :skill-options="lookups.skills"
            @enter="enterMarketAgent"
          />
        </div>

        <section
          v-else-if="keyword.trim()"
          class="flex min-h-[280px] flex-col items-center justify-center rounded-xl border border-dashed border-border bg-card px-6 py-12 text-center"
        >
          <div
            class="flex size-12 items-center justify-center rounded-xl border border-border bg-muted/20 text-muted-foreground"
          >
            <Search class="size-5" />
          </div>
          <h3 class="mt-4 text-sm font-semibold text-foreground">
            {{ $t('market.noResults', { keyword: keyword.trim() }) }}
          </h3>
          <p class="mt-1 max-w-md text-xs leading-5 text-muted-foreground">
            {{ $t('market.noResultsHint') }}
          </p>
          <button
            class="mt-4 cursor-pointer text-xs font-medium text-primary hover:underline"
            type="button"
            @click="keyword = ''"
          >
            {{ $t('market.clearSearch') }}
          </button>
        </section>

        <section
          v-else
          class="market-empty relative flex min-h-[360px] items-center justify-center overflow-hidden rounded-xl border border-dashed border-border bg-card"
        >
          <div class="market-empty-grid pointer-events-none absolute inset-0"></div>
          <div
            class="relative flex max-w-2xl flex-col items-center px-6 py-12 text-center"
          >
            <div
              class="flex size-12 items-center justify-center rounded-xl border border-primary/20 bg-primary/10 text-primary"
            >
              <Bot class="size-5" />
            </div>
            <h3 class="mt-4 text-base font-semibold text-foreground">
              {{ $t('market.emptyTitle') }}
            </h3>
            <p class="mt-1.5 text-sm leading-6 text-muted-foreground">
              {{ $t('market.emptyHint') }}
            </p>
            <button
              class="mt-5 inline-flex h-9 cursor-pointer items-center gap-1.5 rounded-lg bg-primary px-3.5 text-xs font-medium text-primary-foreground transition-opacity hover:opacity-90"
              type="button"
              @click="openRoute('/agents')"
            >
              {{ $t('market.goToAgentAdmin') }}
              <ArrowRight class="size-3.5" />
            </button>
            <div class="mt-5 text-[11px] text-muted-foreground">
              {{
                $t('market.openSourceFooter', { team: LANGCHAT_PRODUCT_TEAM })
              }}
            </div>
          </div>
        </section>
      </NSpin>
    </div>
  </Page>
</template>

<style scoped>
.market-hero {
  isolation: isolate;
}

.market-grid,
.market-empty-grid {
  background-image:
    linear-gradient(hsl(var(--border) / 35%) 1px, transparent 1px),
    linear-gradient(90deg, hsl(var(--border) / 35%) 1px, transparent 1px);
  background-size: 32px 32px;
  mask-image: linear-gradient(to bottom right, black, transparent 75%);
  opacity: 0.45;
}

.market-glow {
  background:
    radial-gradient(
      circle at 35% 35%,
      hsl(var(--primary) / 22%),
      transparent 48%
    ),
    radial-gradient(
      circle at 70% 70%,
      hsl(199 89% 48% / 16%),
      transparent 54%
    );
  filter: blur(4px);
}

.market-search {
  height: 2.5rem;
  border-radius: 0.625rem;
}

.market-empty-grid {
  mask-image: radial-gradient(circle at 82% 45%, black, transparent 68%);
  opacity: 0.5;
}

</style>
