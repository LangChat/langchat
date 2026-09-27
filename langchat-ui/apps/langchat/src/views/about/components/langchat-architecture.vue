<script lang="ts" setup>
defineOptions({ name: 'LangChatArchitecture' });
</script>

<template>
  <div
    class="architecture-scroll"
    role="region"
    :aria-label="$t('about.architecture.diagramAria')"
  >
    <svg
      aria-labelledby="langchat-architecture-title langchat-architecture-desc"
      class="architecture-diagram"
      role="img"
      viewBox="0 0 1100 450"
      xmlns="http://www.w3.org/2000/svg"
    >
      <title id="langchat-architecture-title">
        {{ $t('about.architecture.diagramTitle') }}
      </title>
      <desc id="langchat-architecture-desc">
        {{ $t('about.architecture.diagramDesc') }}
      </desc>

      <defs>
        <linearGradient id="architecture-hero" x1="0" x2="1" y1="0" y2="1">
          <stop
            offset="0"
            stop-color="var(--architecture-primary)"
            stop-opacity="0.16"
          />
          <stop
            offset="1"
            stop-color="var(--architecture-primary)"
            stop-opacity="0.04"
          />
        </linearGradient>
        <marker
          id="architecture-arrow"
          markerHeight="8"
          markerWidth="8"
          orient="auto"
          refX="7"
          refY="4"
        >
          <path d="M0 0L8 4L0 8Z" fill="var(--architecture-line-strong)" />
        </marker>
      </defs>

      <rect
        class="architecture-surface"
        height="448"
        rx="20"
        width="1098"
        x="1"
        y="1"
      />

      <text class="architecture-eyebrow" x="32" y="32">{{ $t('about.architecture.eyebrowEntry') }}</text>
      <g class="architecture-node architecture-node--entry">
        <rect height="64" rx="14" width="184" x="38" y="48" />
        <text class="architecture-node-title" text-anchor="middle" x="130" y="76">
          {{ $t('about.architecture.userAdmin') }}
        </text>
        <text class="architecture-node-copy" text-anchor="middle" x="130" y="98">
          {{ $t('about.architecture.userAdminDesc') }}
        </text>
      </g>

      <path
        class="architecture-flow"
        d="M222 80H306"
        marker-end="url(#architecture-arrow)"
      />

      <g class="architecture-node architecture-node--primary">
        <rect
          fill="url(#architecture-hero)"
          height="76"
          rx="16"
          width="294"
          x="320"
          y="42"
        />
        <text
          class="architecture-node-title architecture-node-title--large"
          text-anchor="middle"
          x="467"
          y="72"
        >
          LangChat UI
        </text>
        <text class="architecture-node-copy" text-anchor="middle" x="467" y="94">
          Vue 3 · TypeScript · Vben Admin
        </text>
        <text class="architecture-node-meta" text-anchor="middle" x="467" y="109">
          {{ $t('about.architecture.ui') }}
        </text>
      </g>

      <path
        class="architecture-flow"
        d="M614 80H694"
        marker-end="url(#architecture-arrow)"
      />
      <text class="architecture-edge-label" text-anchor="middle" x="654" y="69">
        REST / SSE
      </text>

      <g class="architecture-node architecture-node--primary">
        <rect
          fill="url(#architecture-hero)"
          height="76"
          rx="16"
          width="338"
          x="708"
          y="42"
        />
        <text
          class="architecture-node-title architecture-node-title--large"
          text-anchor="middle"
          x="877"
          y="72"
        >
          LangChat Server
        </text>
        <text class="architecture-node-copy" text-anchor="middle" x="877" y="96">
          {{ $t('about.architecture.server') }}
        </text>
      </g>

      <path class="architecture-line" d="M877 118V144M130 144H970" />
      <path
        class="architecture-line"
        d="M130 144V164M410 144V164M690 144V164M970 144V164"
      />

      <text class="architecture-eyebrow" x="32" y="156">{{ $t('about.architecture.eyebrowCore') }}</text>
      <g class="architecture-node">
        <rect height="76" rx="13" width="238" x="11" y="166" />
        <text class="architecture-node-title" text-anchor="middle" x="130" y="194">
          {{ $t('about.architecture.authModule') }}
        </text>
        <text class="architecture-node-code" text-anchor="middle" x="130" y="214">
          langchat-auth
        </text>
        <text class="architecture-node-copy" text-anchor="middle" x="130" y="233">
          {{ $t('about.architecture.authModuleDesc') }}
        </text>
      </g>
      <g class="architecture-node architecture-node--accent">
        <rect height="76" rx="13" width="238" x="291" y="166" />
        <text class="architecture-node-title" text-anchor="middle" x="410" y="194">
          {{ $t('about.architecture.aigcModule') }}
        </text>
        <text class="architecture-node-code" text-anchor="middle" x="410" y="214">
          langchat-aigc
        </text>
        <text class="architecture-node-copy" text-anchor="middle" x="410" y="233">
          {{ $t('about.architecture.aigcModuleDesc') }}
        </text>
      </g>
      <g class="architecture-node architecture-node--accent">
        <rect height="76" rx="13" width="238" x="571" y="166" />
        <text class="architecture-node-title" text-anchor="middle" x="690" y="194">
          {{ $t('about.architecture.aiRuntime') }}
        </text>
        <text class="architecture-node-code" text-anchor="middle" x="690" y="214">
          langchat-core
        </text>
        <text class="architecture-node-copy" text-anchor="middle" x="690" y="233">
          {{ $t('about.architecture.aiRuntimeDesc') }}
        </text>
      </g>
      <g class="architecture-node">
        <rect height="76" rx="13" width="238" x="851" y="166" />
        <text class="architecture-node-title" text-anchor="middle" x="970" y="194">
          {{ $t('about.architecture.commonServices') }}
        </text>
        <text class="architecture-node-code" text-anchor="middle" x="970" y="214">
          langchat-common
        </text>
        <text class="architecture-node-copy" text-anchor="middle" x="970" y="233">
          {{ $t('about.architecture.commonServicesDesc') }}
        </text>
      </g>

      <path class="architecture-line" d="M130 242V274M970 242V274M130 274H970" />
      <path
        class="architecture-line"
        d="M118 274V296M334 274V296M550 274V296M766 274V296M982 274V296"
      />

      <text class="architecture-eyebrow" x="32" y="288">{{ $t('about.architecture.eyebrowData') }}</text>
      <g class="architecture-node architecture-node--storage">
        <rect height="70" rx="13" width="192" x="22" y="298" />
        <text class="architecture-node-title" text-anchor="middle" x="118" y="328">
          MySQL
        </text>
        <text class="architecture-node-copy" text-anchor="middle" x="118" y="350">
          {{ $t('about.architecture.businessData') }}
        </text>
      </g>
      <g class="architecture-node architecture-node--storage">
        <rect height="70" rx="13" width="192" x="238" y="298" />
        <text class="architecture-node-title" text-anchor="middle" x="334" y="328">
          {{ $t('about.architecture.vectorStore') }}
        </text>
        <text class="architecture-node-copy" text-anchor="middle" x="334" y="350">
          PGVector / Milvus
        </text>
      </g>
      <g class="architecture-node architecture-node--storage">
        <rect height="70" rx="13" width="192" x="454" y="298" />
        <text class="architecture-node-title" text-anchor="middle" x="550" y="328">
          {{ $t('about.architecture.objectStorage') }}
        </text>
        <text class="architecture-node-copy" text-anchor="middle" x="550" y="350">
          {{ $t('about.architecture.objectStorageDesc') }}
        </text>
      </g>
      <g class="architecture-node architecture-node--external">
        <rect height="70" rx="13" width="192" x="670" y="298" />
        <text class="architecture-node-title" text-anchor="middle" x="766" y="328">
          {{ $t('about.architecture.modelProviders') }}
        </text>
        <text class="architecture-node-copy" text-anchor="middle" x="766" y="350">
          {{ $t('about.architecture.modelProvidersDesc') }}
        </text>
      </g>
      <g class="architecture-node architecture-node--external">
        <rect height="70" rx="13" width="192" x="886" y="298" />
        <text class="architecture-node-title" text-anchor="middle" x="982" y="328">
          {{ $t('about.architecture.mcpServices') }}
        </text>
        <text class="architecture-node-copy" text-anchor="middle" x="982" y="350">
          {{ $t('about.architecture.mcpServicesDesc') }}
        </text>
      </g>

      <g class="architecture-summary">
        <rect height="44" rx="10" width="1034" x="33" y="388" />
        <circle cx="54" cy="410" r="4" />
        <text x="68" y="414">
          {{ $t('about.architecture.diagramNote') }}
        </text>
      </g>
    </svg>
  </div>
</template>

<style scoped>
.architecture-scroll {
  width: 100%;
  overflow-x: auto;
  border-radius: 14px;
}

.architecture-diagram {
  --architecture-primary: hsl(var(--primary));
  --architecture-surface: hsl(var(--card));
  --architecture-surface-muted: hsl(var(--muted) / 45%);
  --architecture-border: hsl(var(--border));
  --architecture-line: hsl(var(--border));
  --architecture-line-strong: hsl(var(--muted-foreground) / 70%);
  --architecture-text: hsl(var(--foreground));
  --architecture-text-muted: hsl(var(--muted-foreground));
  display: block;
  min-width: 880px;
  width: 100%;
}

.architecture-surface {
  fill: var(--architecture-surface);
  stroke: var(--architecture-border);
}

.architecture-node rect {
  fill: var(--architecture-surface-muted);
  stroke: var(--architecture-border);
  stroke-width: 1.2;
}

.architecture-node--primary rect,
.architecture-node--accent rect {
  stroke: color-mix(
    in srgb,
    var(--architecture-primary) 42%,
    var(--architecture-border)
  );
}

.architecture-node--entry rect {
  fill: hsl(var(--muted) / 28%);
}

.architecture-node--storage rect {
  fill: hsl(var(--primary) / 5%);
}

.architecture-node--external rect {
  fill: hsl(var(--muted) / 32%);
  stroke-dasharray: 5 4;
}

.architecture-node-title {
  fill: var(--architecture-text);
  font-size: 14px;
  font-weight: 650;
}

.architecture-node-title--large {
  font-size: 17px;
}

.architecture-node-copy,
.architecture-node-meta,
.architecture-node-code,
.architecture-edge-label,
.architecture-eyebrow {
  fill: var(--architecture-text-muted);
}

.architecture-node-copy {
  font-size: 11px;
}

.architecture-node-meta,
.architecture-node-code {
  font-size: 10px;
}

.architecture-node-code {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  fill: var(--architecture-primary);
}

.architecture-eyebrow {
  font-size: 9.5px;
  font-weight: 650;
  letter-spacing: 0.12em;
}

.architecture-edge-label {
  font-size: 9.5px;
  font-weight: 550;
}

.architecture-line,
.architecture-flow {
  fill: none;
  stroke: var(--architecture-line);
  stroke-linecap: round;
  stroke-linejoin: round;
  stroke-width: 1.3;
}

.architecture-flow {
  stroke: var(--architecture-line-strong);
}

.architecture-summary rect {
  fill: hsl(var(--muted) / 24%);
  stroke: var(--architecture-border);
}

.architecture-summary circle {
  fill: var(--architecture-primary);
}

.architecture-summary text {
  fill: var(--architecture-text-muted);
  font-size: 10.5px;
}
</style>
