<!--
  - © 2024-present LangChat团队. 版权所有.
  -->
<script lang="ts" setup>
/**
 * 登录页左侧 AI 主题插画：
 * 中心 AI 芯片（机器人面孔）通过链路连接聊天、知识库、能力、数据四个节点。
 */
defineOptions({ name: 'AuthIllustration' });
</script>

<template>
  <svg
    class="lc-illustration"
    fill="none"
    viewBox="0 0 520 400"
    xmlns="http://www.w3.org/2000/svg"
  >
    <defs>
      <linearGradient id="lcGrad" x1="0" y1="0" x2="1" y2="1">
        <stop offset="0" stop-color="#3b82f6" />
        <stop offset="1" stop-color="#8b5cf6" />
      </linearGradient>
      <linearGradient id="lcGradSoft" x1="0" y1="0" x2="1" y2="1">
        <stop offset="0" stop-color="#60a5fa" />
        <stop offset="1" stop-color="#a78bfa" />
      </linearGradient>
      <radialGradient id="lcGlow" cx="0.5" cy="0.5" r="0.5">
        <stop offset="0" stop-color="#3b82f6" stop-opacity="0.3" />
        <stop offset="1" stop-color="#3b82f6" stop-opacity="0" />
      </radialGradient>
    </defs>

    <!-- 背景光晕 -->
    <ellipse cx="260" cy="205" fill="url(#lcGlow)" rx="250" ry="190" />

    <!-- 轨道 -->
    <ellipse
      class="lc-orbit"
      cx="260"
      cy="205"
      fill="none"
      rx="205"
      ry="72"
      stroke="#93c5fd"
      stroke-dasharray="4 10"
      stroke-opacity="0.5"
      stroke-width="1.5"
    />

    <!-- 链路 -->
    <g opacity="0.6" stroke="url(#lcGrad)" stroke-linecap="round" stroke-width="2">
      <path class="lc-flow" d="M222 172 L168 122" />
      <path class="lc-flow lc-flow-reverse" d="M298 172 L352 122" />
      <path class="lc-flow lc-flow-reverse" d="M222 248 L168 296" />
      <path class="lc-flow" d="M298 248 L352 296" />
    </g>

    <!-- 节点：对话气泡 -->
    <g class="lc-node">
      <path d="M128 120 l0 16 18 -16 z" fill="url(#lcGradSoft)" />
      <rect fill="url(#lcGrad)" height="42" rx="13" width="62" x="109" y="79" />
      <circle cx="126" cy="100" fill="#e0f2fe" r="3.5" />
      <circle cx="140" cy="100" fill="#e0f2fe" r="3.5" class="lc-blink" />
      <circle cx="154" cy="100" fill="#e0f2fe" r="3.5" />
    </g>

    <!-- 节点：知识库（数据圆柱） -->
    <g class="lc-node lc-node-delay">
      <path
        d="M352 86 v30 c0 6 12.5 10 28 10 s28 -4 28 -10 v-30"
        fill="url(#lcGradSoft)"
        opacity="0.92"
      />
      <ellipse cx="380" cy="86" fill="#bfdbfe" rx="28" ry="10" />
      <ellipse
        cx="380"
        cy="101"
        fill="none"
        rx="28"
        ry="9"
        stroke="#e0f2fe"
        stroke-opacity="0.7"
        stroke-width="2"
      />
    </g>

    <!-- 节点：能力闪电 -->
    <g class="lc-node lc-node-delay">
      <path
        d="M148 292 l-24 36 h15 l-9 32 28 -40 h-15 l13 -28 z"
        fill="url(#lcGrad)"
      />
    </g>

    <!-- 节点：数据图表 -->
    <g class="lc-node">
      <rect fill="#60a5fa" height="26" rx="3" width="11" x="356" y="308" />
      <rect fill="#3b82f6" height="40" rx="3" width="11" x="373" y="294" />
      <rect fill="#8b5cf6" height="20" rx="3" width="11" x="390" y="314" />
    </g>

    <!-- 中心 AI 芯片 -->
    <g class="lc-chip">
      <circle cx="260" cy="204" fill="url(#lcGlow)" r="92" />
      <g opacity="0.85" stroke="#60a5fa" stroke-linecap="round" stroke-width="4">
        <path d="M236 126 v14" />
        <path d="M260 126 v14" />
        <path d="M284 126 v14" />
        <path d="M236 268 v14" />
        <path d="M260 268 v14" />
        <path d="M284 268 v14" />
        <path d="M182 180 h14" />
        <path d="M182 204 h14" />
        <path d="M182 228 h14" />
        <path d="M324 180 h14" />
        <path d="M324 204 h14" />
        <path d="M324 228 h14" />
      </g>
      <rect fill="url(#lcGrad)" height="128" rx="28" width="128" x="196" y="140" />
      <path
        d="M260 140 v-10"
        stroke="#e0f2fe"
        stroke-linecap="round"
        stroke-width="4"
      />
      <circle class="lc-blink" cx="260" cy="124" fill="#e0f2fe" r="6" />
      <rect fill="#ffffff" height="58" opacity="0.94" rx="16" width="78" x="221" y="174" />
      <circle class="lc-blink" cx="247" cy="198" fill="#2563eb" r="7" />
      <circle class="lc-blink" cx="273" cy="198" fill="#2563eb" r="7" />
      <rect fill="#60a5fa" height="6" rx="3" width="24" x="248" y="212" />
    </g>
  </svg>
</template>

<style scoped>
/* 宽度由使用方的 tailwind 宽度类控制，这里不再强制 width:100% */
.lc-illustration {
  height: auto;
}

.lc-chip {
  animation: lc-float 5s ease-in-out infinite;
  transform-box: fill-box;
  transform-origin: center;
}

.lc-node {
  animation: lc-float 4s ease-in-out infinite;
  transform-box: fill-box;
  transform-origin: center;
}

.lc-node-delay {
  animation-delay: -2s;
}

.lc-orbit {
  animation: lc-dash 30s linear infinite;
}

.lc-flow {
  animation: lc-dash-flow 1.8s linear infinite;
  stroke-dasharray: 6 8;
}

.lc-flow-reverse {
  animation-direction: reverse;
}

.lc-blink {
  animation: lc-blink 3.4s ease-in-out infinite;
  transform-box: fill-box;
  transform-origin: center;
}

@keyframes lc-float {
  0%,
  100% {
    transform: translateY(0);
  }

  50% {
    transform: translateY(-8px);
  }
}

@keyframes lc-dash {
  to {
    stroke-dashoffset: -280;
  }
}

@keyframes lc-dash-flow {
  to {
    stroke-dashoffset: -56;
  }
}

@keyframes lc-blink {
  0%,
  92%,
  100% {
    opacity: 1;
    transform: scaleY(1);
  }

  96% {
    opacity: 0.35;
    transform: scaleY(0.35);
  }
}

@media (prefers-reduced-motion: reduce) {
  .lc-blink,
  .lc-chip,
  .lc-flow,
  .lc-node,
  .lc-orbit {
    animation: none;
  }
}
</style>
