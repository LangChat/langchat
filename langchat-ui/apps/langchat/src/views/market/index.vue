<script lang="ts" setup>
import type {AigcAgent} from '#/api/aigc/agent';

import {computed, onMounted, onUnmounted, ref} from 'vue';
import {useRouter} from 'vue-router';

import {Page} from '@vben/common-ui';
import {Search} from '@vben/icons';
import {preferences, usePreferences} from '@vben/preferences';

import {NInput, NSpin} from 'naive-ui';

import {message} from '#/adapter/naive';
import {agentApi} from '#/api/aigc/agent';
import LcEmptyState from '#/components/LcEmptyState/index.vue';
import {useAigcLookups} from '#/views/shared/aigc/lookups';

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

function enterMarketAgent(item: AigcAgent) {
  const id = String(item.id || '');
  if (!id) {
    message.error('当前应用缺少 ID');
    return;
  }
  void router.push(`/market/${id}/chat`);
}

// —— 产品场景打字机效果 ——
const MARKET_FEATURE_WORDS = [
  'AIGC 应用构建',
  '知识库检索问答',
  '技能编排调度',
  'MCP 工具集成',
  '智能问数分析',
  '图片理解生成',
];
const typedText = ref('');
let typeTimer: null | ReturnType<typeof setTimeout> = null;
let typeWordIndex = 0;
let typeCharIndex = 0;
let typeDeleting = false;

function typeTick() {
  const word = MARKET_FEATURE_WORDS[typeWordIndex] ?? '';
  if (!typeDeleting) {
    typeCharIndex += 1;
    typedText.value = word.slice(0, typeCharIndex);
    if (typeCharIndex >= word.length) {
      typeDeleting = true;
      typeTimer = setTimeout(typeTick, 2000);
      return;
    }
    typeTimer = setTimeout(typeTick, 150);
  } else {
    typeCharIndex -= 1;
    typedText.value = word.slice(0, typeCharIndex);
    if (typeCharIndex <= 0) {
      typeDeleting = false;
      typeWordIndex = (typeWordIndex + 1) % MARKET_FEATURE_WORDS.length;
      typeTimer = setTimeout(typeTick, 400);
      return;
    }
    typeTimer = setTimeout(typeTick, 70);
  }
}

async function initializePage() {
  await Promise.all([loadLookups(), loadList()]);
}

onMounted(() => {
  initializePage();
  typeTimer = setTimeout(typeTick, 500);
});

onUnmounted(() => {
  if (typeTimer) {
    clearTimeout(typeTimer);
  }
});
</script>

<template>
  <Page>
    <div class="flex flex-col items-center">
      <!-- 居中产品 Hero -->
      <div
        class="flex flex-col items-center gap-3 pb-2 pt-10 text-center md:pt-14"
      >
        <div class="relative inline-block">
          <h1 class="market-title text-5xl font-extrabold tracking-tight md:text-6xl">
            LangChat
          </h1>
          <!-- 手绘感渐变下划线 -->
          <svg
            class="absolute -bottom-3 left-1/2 h-4 w-[110%] -translate-x-1/2"
            fill="none"
            preserveAspectRatio="none"
            viewBox="0 0 320 20"
            xmlns="http://www.w3.org/2000/svg"
          >
            <defs>
              <linearGradient id="marketUnderline" x1="0" x2="1" y1="0" y2="0">
                <stop offset="0" stop-color="#3b82f6" />
                <stop offset="0.5" stop-color="#8b5cf6" />
                <stop offset="1" stop-color="#0ea5e9" />
              </linearGradient>
            </defs>
            <path
              class="market-underline"
              d="M8 13 C 70 4, 130 17, 190 9 S 290 5, 312 11"
              stroke="url(#marketUnderline)"
              stroke-linecap="round"
              stroke-width="5"
            />
          </svg>
          <span class="market-spark absolute -right-8 -top-2 text-2xl text-primary/60">
            ✦
          </span>
        </div>
        <p
          class="max-w-2xl text-sm leading-7 text-muted-foreground md:text-base"
        >
          开箱即用的企业级 AIGC 应用构建平台，聚合大模型、知识库、技能与 MCP
          工具，让 AI 能力快速融入你的业务。
        </p>
        <div class="flex items-center gap-3 text-sm md:text-base">
          <span
            class="hidden h-px w-12 bg-gradient-to-r from-transparent to-primary/40 sm:block"
          ></span>
          <span class="text-muted-foreground">已覆盖</span>
          <span class="font-semibold text-primary">{{ typedText }}</span>
          <span
            class="market-cursor inline-block h-4 w-0.5 rounded-full bg-primary md:h-5"
          ></span>
          <span
            class="hidden h-px w-12 bg-gradient-to-l from-transparent to-primary/40 sm:block"
          ></span>
        </div>
        <NInput
          v-model:value="keyword"
          class="market-search mt-5 w-full max-w-2xl"
          clearable
          placeholder="搜索已发布应用，点击卡片即可进入会话"
        >
          <template #prefix>
            <Search class="size-5 text-muted-foreground" />
          </template>
        </NInput>
      </div>

      <!-- 应用卡片 / 空状态 -->
      <NSpin :show="loading" class="mt-10 w-full">
        <div
          v-if="filteredPublishedAgents.length > 0"
          class="mx-auto grid w-full max-w-7xl gap-3 md:grid-cols-1 lg:grid-cols-2 xl:grid-cols-3 2xl:grid-cols-4"
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
        <LcEmptyState
          v-else
          class="py-16"
          :description="
            keyword.trim()
              ? '没有找到匹配的应用，换个关键词试试'
              : '发布 Agent 后会自动出现在这里，点击卡片即可进入会话'
          "
          title="暂无已发布应用"
        />
      </NSpin>

      <!-- 页脚署名 -->
      <div
        class="mt-12 flex flex-col items-center gap-1 pb-2 text-xs text-muted-foreground"
      >
        <div class="flex items-center gap-1.5">
          <img
            v-if="logoSrc"
            :alt="appName"
            :src="logoSrc"
            class="size-4 rounded"
          />
          <span class="text-sm font-semibold text-foreground/80">
            {{ appName }}
          </span>
          <span>· LangChat Team 作品</span>
        </div>
        <div>
          官网：
          <a
            class="transition-colors hover:text-primary"
            href="https://langchat.cn"
            rel="noopener"
            target="_blank"
          >
            langchat.cn
          </a>
        </div>
      </div>
    </div>
  </Page>
</template>

<style scoped>
/* 标题渐变 + 流动动效 */
.market-title {
  background: linear-gradient(
    90deg,
    hsl(var(--primary)) 0%,
    #7c3aed 30%,
    hsl(var(--primary)) 60%,
    #0ea5e9 100%
  );
  background-size: 250% auto;
  background-clip: text;
  -webkit-background-clip: text;
  color: transparent;
  animation: market-title-flow 8s linear infinite;
}

@keyframes market-title-flow {
  to {
    background-position: 250% center;
  }
}

/* 手绘下划线：入场描画动效 */
.market-underline {
  stroke-dasharray: 340;
  stroke-dashoffset: 340;
  animation: market-underline-draw 1s ease-out 0.3s forwards;
}

@keyframes market-underline-draw {
  to {
    stroke-dashoffset: 0;
  }
}

/* 标题旁的星形点缀：缓慢闪烁 */
.market-spark {
  animation: market-spark-twinkle 3s ease-in-out infinite;
}

@keyframes market-spark-twinkle {
  0%,
  100% {
    opacity: 0.45;
    transform: scale(0.9) rotate(0deg);
  }

  50% {
    opacity: 1;
    transform: scale(1.1) rotate(18deg);
  }
}

/* 打字机光标闪烁 */
.market-cursor {
  animation: market-cursor-blink 1s steps(2, start) infinite;
}

@keyframes market-cursor-blink {
  50% {
    opacity: 0;
  }
}

/* 大号胶囊搜索框：无阴影，文字居中 */
.market-search {
  height: 3.5rem;
  border-radius: 9999px;
  font-size: 1rem;
}

.market-search :deep(input) {
  font-size: 1rem;
  text-align: center;
}

@media (prefers-reduced-motion: reduce) {
  .market-underline {
    stroke-dashoffset: 0;
  }

  .market-spark {
    animation: none;
  }

  .market-title {
    animation: none;
  }
}
</style>
