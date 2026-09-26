<!--
  - © 2024-present LangChat团队. 版权所有.
  -->
<script lang="ts" setup>
import { computed } from 'vue';

import {
  Bot,
  ChartNoAxesCombined,
  LibraryBig,
  Wrench,
} from '@vben/icons';
import { AuthPageLayout } from '@vben/layouts';
import { preferences } from '@vben/preferences';

import AuthIllustration from './auth-illustration.vue';

const appName = computed(() => preferences.app.name);
const logo = computed(() => preferences.logo.source);
const logoDark = computed(() => preferences.logo.sourceDark);

/**
 * 登录页左侧特色功能卡片
 */
const authFeatures = [
  {
    icon: Bot,
    title: '智能应用构建',
    description: '可视化编排 Agent，一键发布 AI 应用',
  },
  {
    icon: LibraryBig,
    title: '知识库问答',
    description: '文档沉淀与 RAG 检索增强回答',
  },
  {
    icon: Wrench,
    title: '技能 / MCP 扩展',
    description: '灵活挂载技能与外部工具能力',
  },
  {
    icon: ChartNoAxesCombined,
    title: '智能问数',
    description: '自然语言直达数据洞察',
  },
];
</script>

<template>
  <AuthPageLayout :app-name="appName" :logo="logo" :logo-dark="logoDark">
    <!-- 左侧品牌区：自定义 AI 插画 + 文案 + 特色功能卡片 -->
    <template #slogan>
      <div class="flex w-full max-w-xl flex-col items-center">
        <AuthIllustration class="w-56 md:w-72" />
        <div class="mt-6 text-2xl font-semibold tracking-tight text-foreground">
          开箱即用的企业级 AIGC 应用平台
        </div>
        <div class="mt-2 text-sm text-muted-foreground md:text-base">
          聚合大模型、知识库、技能与 MCP 工具，让 AI 能力快速融入你的业务
        </div>

        <div class="mt-8 grid w-full grid-cols-1 gap-3 sm:grid-cols-2">
          <div
            v-for="feature in authFeatures"
            :key="feature.title"
            class="flex items-start gap-3 rounded-lg border border-border/60 bg-background/60 p-3 text-left backdrop-blur-sm transition-colors hover:border-primary/40"
          >
            <div
              class="flex size-9 shrink-0 items-center justify-center rounded-md bg-primary/10 text-primary"
            >
              <component :is="feature.icon" class="size-5" />
            </div>
            <div class="min-w-0">
              <div class="text-sm font-medium text-foreground">
                {{ feature.title }}
              </div>
              <div class="mt-0.5 text-xs leading-5 text-muted-foreground">
                {{ feature.description }}
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 面板底部署名信息（锚定到左侧品牌面板底部） -->
      <div
        class="absolute inset-x-0 bottom-5 flex flex-col items-center gap-1 text-xs text-muted-foreground"
      >
        <div class="flex items-center gap-1.5">
          <span class="text-sm font-semibold text-foreground/80">LangChat</span>
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
    </template>
  </AuthPageLayout>
</template>
