<script lang="ts" setup>
import { computed } from 'vue';

import { Page } from '@vben/common-ui';
import { ExternalLink, Globe, SvgGithubIcon } from '@vben/icons';
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

const productHighlights = [
  {
    description: '统一管理不同厂商与能力类型的模型服务',
    label: '多模型统一接入',
  },
  {
    description: '覆盖文档解析、切片、向量化与检索增强',
    label: '知识库与文档工程',
  },
  {
    description: '让 Agent 连接技能、工具与外部业务能力',
    label: 'Skills 与 MCP 编排',
  },
  {
    description: '以实时响应、权限体系和模块化架构支撑交付',
    label: '流式对话与平台治理',
  },
];

const techStacks = [
  {
    label: '前端应用',
    value: 'Vue 3、TypeScript、Vben Admin、Naive UI、Tailwind CSS',
  },
  {
    label: '后端服务',
    value: 'JDK 17、Spring Boot 3、MyBatis-Plus、Sa-Token',
  },
  {
    label: 'AI 运行时',
    value: 'LangChain4j、RAG、SSE 流式响应、工具与技能执行',
  },
  {
    label: '数据与存储',
    value: 'MySQL、PGVector / Milvus、S3 兼容对象存储',
  },
];
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
                {{ LANGCHAT_PRODUCT_TEAM }} · 开源产品
              </span>
            </div>
            <div class="mt-3 flex flex-wrap items-center gap-2">
              <h1 class="text-balance text-xl font-semibold text-foreground">
                {{ LANGCHAT_PRODUCT_NAME }}：连接模型、知识与智能体的开源应用平台
              </h1>
              <span
                class="rounded-md border border-primary/20 bg-primary/5 px-2 py-0.5 text-[10px] font-semibold text-primary"
              >
                Open Source
              </span>
            </div>
            <p
              class="mt-3 max-w-4xl text-pretty text-sm leading-6 text-muted-foreground"
            >
              LangChat 面向需要构建知识增强与智能体应用的团队，将模型接入、知识工程、Agent、Skills 与 MCP 汇聚到统一的产品体系中。它不仅提供应用配置界面，也打通从数据准备、能力编排到对话运行和平台治理的完整链路。
            </p>
            <p
              class="mt-1.5 max-w-4xl text-pretty text-xs leading-5 text-muted-foreground"
            >
              项目的核心价值是把分散的 AI 基础能力沉淀为可管理、可组合、可交付的业务模块，让开发团队能够保留技术自主性，同时减少重复建设模型网关、RAG 流程、工具调用和权限体系的成本。
            </p>
          </div>

          <div class="shrink-0">
            <div
              class="mb-2 text-[10px] font-medium text-muted-foreground"
            >
              官方与开源地址
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
                官方网站
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
            <div class="text-sm font-semibold text-foreground">系统组件架构</div>
            <div class="mt-1 text-xs font-normal text-muted-foreground">
              从前端交互、服务聚合到 AI 核心能力与基础设施的模块关系
            </div>
          </div>
        </template>
        <LangChatArchitecture />
      </LcCard>

      <LcCard :hoverable="false" :show-icon="false">
        <template #header>
          <div class="text-sm font-semibold text-foreground">技术架构</div>
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
