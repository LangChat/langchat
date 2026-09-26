<script lang="ts" setup>
import {Page} from '@vben/common-ui';

import {NButton, NTag} from 'naive-ui';

import LcCard from '#/components/LcCard/index.vue';

const productHighlights = [
  '统一接入多模型供应商与向量数据库',
  '知识库、文档解析、切片、向量化一体化',
  '支持 Agent、RAG、Skills、MCP 的业务编排',
  '后端事件流兼容 OpenAI Chat Completions 风格',
];

const moduleArchitecture = [
  {
    name: 'langchat-ui',
    desc: '前端控制台，基于 Vben + Naive UI + Tailwind 构建。',
  },
  {
    name: 'langchat-server',
    desc: 'Spring Boot 启动层，聚合认证、AIGC 与核心能力模块。',
  },
  { name: 'langchat-auth', desc: '账号、角色、菜单与动态路由等权限治理能力。' },
  {
    name: 'langchat-aigc',
    desc: '模型、知识库、文档、智能体、技能等业务 CRUD 与流程。',
  },
  {
    name: 'langchat-core',
    desc: 'LangChain4j 驱动的聊天、检索、事件流与技能执行核心。',
  },
  {
    name: 'langchat-common',
    desc: '通用认证、核心工具、OSS、任务等基础能力沉淀。',
  },
];

const techStacks = [
  '前端：Vue 3、TypeScript、Vben Admin、Naive UI、Tailwind CSS',
  '后端：JDK 17、Spring Boot 3、MyBatis-Plus、Sa-Token、Lombok',
  'AI 编排：LangChain4j、SSE 流式响应、OpenAI 风格消息协议',
  '数据能力：MySQL、PGVector、Milvus、Tika、PDFBox',
];

function openRepo() {
  window.open('https://github.com/langchat-team/langchat', '_blank');
}
</script>

<template>
  <Page>
    <div class="flex flex-col gap-2">
      <div class="rounded-xl border border-border bg-card px-4 py-3">
        <div
          class="flex flex-col gap-2 xl:flex-row xl:items-end xl:justify-between"
        >
          <div class="space-y-2">
            <div class="text-lg font-semibold text-foreground">
              关于 LangChat
            </div>
            <div class="max-w-4xl text-sm leading-7 text-muted-foreground">
              LangChat
              是一个面向企业知识增强与智能体应用构建的全栈开源平台，聚焦模型接入、知识工程、对话编排、权限治理与运行观测，目标是让业务团队用统一控台完成
              AI 应用的建设、管理与发布。
            </div>
            <div class="flex flex-wrap gap-2">
              <NTag
                v-for="item in productHighlights"
                :key="item"
                :bordered="false"
                round
                size="small"
                type="primary"
              >
                {{ item }}
              </NTag>
            </div>
          </div>

          <div class="flex items-center gap-2">
            <NButton secondary @click="openRepo"> 开源地址 </NButton>
          </div>
        </div>
      </div>

      <div class="grid gap-4 xl:grid-cols-[1.2fr,1fr]">
        <LcCard>
          <template #header>
            <div class="text-sm font-semibold text-foreground">产品定位</div>
          </template>

          <div class="space-y-3 text-sm leading-7 text-muted-foreground">
            <p>
              LangChat 面向需要快速搭建企业级 AI
              应用的平台型团队，重点覆盖多供应商模型管理、知识库接入、文档解析向量化、智能体配置、实时对话与权限治理等核心场景。
            </p>
            <p>
              在交互层，平台统一控制台、动态菜单与权限体系；在业务层，沉淀模型、知识、文档、技能、MCP
              与智能体；在核心层，围绕 LangChain4j 构建
              RAG、事件流、工具执行与消息协议封装。
            </p>
          </div>
        </LcCard>

        <LcCard>
          <template #header>
            <div class="text-sm font-semibold text-foreground">技术栈</div>
          </template>

          <div class="space-y-2">
            <div
              v-for="item in techStacks"
              :key="item"
              class="rounded-lg border border-border bg-muted/30 px-3 py-2.5 text-sm text-muted-foreground"
            >
              {{ item }}
            </div>
          </div>
        </LcCard>
      </div>

      <LcCard>
        <template #header>
          <div class="text-sm font-semibold text-foreground">模块架构</div>
        </template>

        <div class="grid gap-2.5 md:grid-cols-1 xl:grid-cols-3">
          <div
            v-for="item in moduleArchitecture"
            :key="item.name"
            class="rounded-lg border border-border bg-card px-3 py-2.5"
          >
            <div class="text-sm font-medium text-foreground">
              {{ item.name }}
            </div>
            <div class="mt-1 text-[12px] leading-6 text-muted-foreground">
              {{ item.desc }}
            </div>
          </div>
        </div>
      </LcCard>
    </div>
  </Page>
</template>
