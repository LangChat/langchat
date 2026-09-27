<script setup lang="ts">
import type { AigcModel } from '#/api/aigc/model';

import { computed, ref } from 'vue';
import { Check, ChevronDown, Search, Settings, X } from '@vben/icons';
import { $t } from '@vben/locales';
import {
  ScrollArea,
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
  VbenPopover,
} from '@vben-core/shadcn-ui';

import {
  getProviderIcon,
  getProviderLabel,
  normalizeModelType,
} from '#/views/models/model-meta';

interface ModelConfig {
  maxOutputTokens?: number;
  temperature?: number;
  topP?: number;
}

function isStringIcon(icon: unknown): icon is string {
  return typeof icon === 'string';
}

interface Props {
  allowedTypes?: string[];
  config: ModelConfig;
  modelEntities?: AigcModel[];
  modelId?: string;
}

const props = withDefaults(defineProps<Props>(), {
  allowedTypes: () => ['REASONING', 'CHAT'],
  modelEntities: () => [],
  modelId: '',
});

const emit = defineEmits<{
  'update:config': [value: ModelConfig];
  'update:modelId': [value: string];
}>();

const searchKeyword = ref('');
const showConfigPanel = ref(false);
const selectedPreset = ref('balanced');

const CONFIG_PRESETS = [
  {
    labelKey: 'components.modelSelector.creative',
    value: 'creative',
    config: { temperature: 1.1, topP: 0.95 },
  },
  {
    labelKey: 'components.modelSelector.balanced',
    value: 'balanced',
    config: { temperature: 0.7, topP: 0.9 },
  },
  {
    labelKey: 'components.modelSelector.precise',
    value: 'precise',
    config: { temperature: 0.2, topP: 0.6 },
  },
];

const filteredModelEntities = computed(() => {
  // 调用方传入的类型可能是旧枚举（CHAT/REASONING/VISION 等），
  // 模型表保存的是规范枚举（TEXT2TEXT 等），两侧统一归一化后再比对
  const allowSet = new Set(
    props.allowedTypes.map((item) => normalizeModelType(String(item))),
  );
  return props.modelEntities.filter((item) => {
    const type = normalizeModelType(String(item.type || ''));
    return allowSet.size === 0 || allowSet.has(type);
  });
});

const providerGroups = computed(() => {
  const map = new Map<string, AigcModel[]>();
  filteredModelEntities.value.forEach((item) => {
    const provider = String(item.provider || 'UNKNOWN');
    const list = map.get(provider) || [];
    list.push(item);
    map.set(provider, list);
  });
  return [...map.entries()]
    .map(([provider, models]) => ({
      models,
      provider,
    }))
    .sort((a, b) => a.provider.localeCompare(b.provider));
});

const filteredProviderGroups = computed(() => {
  const query = searchKeyword.value.trim().toLowerCase();
  if (!query) {
    return providerGroups.value;
  }
  return providerGroups.value
    .map((group) => ({
      provider: group.provider,
      models: group.models.filter((item) =>
        [item.name, item.model, group.provider]
          .map((value) => String(value || '').toLowerCase())
          .some((value) => value.includes(query)),
      ),
    }))
    .filter((group) => group.models.length > 0);
});

const selectedModelEntity = computed(() =>
  props.modelEntities.find(
    (item) => String(item.id || '') === String(props.modelId || ''),
  ),
);

const selectedModelLabel = computed(() => {
  if (!props.modelId) {
    return $t('components.modelSelector.placeholder');
  }
  const selected = selectedModelEntity.value;
  if (!selected) {
    return $t('components.modelSelector.placeholder');
  }
  return selected.name || selected.model || $t('models.card.unnamed');
});

function updateConfig(patch: Partial<ModelConfig>) {
  emit('update:config', {
    ...props.config,
    ...patch,
  });
}

function updateTemperature(value: number) {
  updateConfig({ temperature: Number((value / 100).toFixed(2)) });
}

function updateTopP(value: number) {
  updateConfig({ topP: Number((value / 100).toFixed(2)) });
}

function updateMaxOutputTokens(value: number) {
  updateConfig({ maxOutputTokens: value });
}

function toTemperatureRange(value?: number) {
  return clamp(Math.round((value ?? 0.7) * 100), 0, 100);
}

function toTopPRange(value?: number) {
  return clamp(Math.round((value ?? 0.9) * 100), 0, 100);
}

function toTokenRange(value?: number) {
  return clamp(value ?? 2048, 256, 8192);
}

function clamp(value: number, min: number, max: number) {
  return Math.min(max, Math.max(min, value));
}

function selectModel(item: AigcModel) {
  emit('update:modelId', String(item.id || ''));
  searchKeyword.value = '';
}

function applyPreset(value: string) {
  selectedPreset.value = value;
  const preset = CONFIG_PRESETS.find((item) => item.value === value);
  if (!preset) {
    return;
  }
  updateConfig({
    temperature: preset.config.temperature,
    topP: preset.config.topP,
  });
}
</script>

<template>
  <VbenPopover
    :content-props="{ align: 'start', side: 'bottom', sideOffset: 8 }"
    content-class="w-(--reka-popover-trigger-width) rounded-xl border border-border bg-card p-0"
    trigger-class="w-full"
  >
    <template #trigger>
      <button
        class="border-border bg-background hover:border-primary text-foreground flex h-[34px] w-full items-center gap-1.5 rounded-md border px-2.5 text-xs transition-colors"
        type="button"
      >
        <template v-if="selectedModelEntity">
          <template v-if="isStringIcon(getProviderIcon(selectedModelEntity.provider))">
            <img
              :src="getProviderIcon(selectedModelEntity.provider)"
              class="size-4 shrink-0"
              alt=""
            />
          </template>
          <component
            :is="getProviderIcon(selectedModelEntity.provider)"
            v-else
            class="size-4 shrink-0"
          />
          <span class="flex-1 truncate text-left text-xs">{{
            selectedModelLabel
          }}</span>
          <Settings class="text-muted-foreground size-3.5 shrink-0" />
        </template>
        <template v-else>
          <span class="text-muted-foreground flex-1 text-left text-xs">{{
            $t('components.modelSelector.placeholder')
          }}</span>
        </template>
        <ChevronDown class="text-muted-foreground size-3.5 shrink-0" />
      </button>
    </template>

    <div class="relative flex h-[380px] flex-col">
      <div class="border-border/40 space-y-2 border-b px-3 pb-3 pt-3">
        <div class="flex items-center justify-between">
          <span class="text-foreground text-xs font-medium">{{
            $t('components.modelSelector.label')
          }}</span>
          <button
            :class="
              showConfigPanel
                ? 'hover:bg-accent text-foreground'
                : 'hover:bg-accent text-muted-foreground'
            "
            class="flex size-6 items-center justify-center rounded-md transition-colors"
            :title="$t('components.modelSelector.paramsTitle')"
            type="button"
            @click="showConfigPanel = !showConfigPanel"
          >
            <Settings class="size-3.5" />
          </button>
        </div>

        <div
          class="bg-muted flex h-[34px] items-center gap-2 rounded-md px-2.5"
        >
          <Search class="text-muted-foreground size-3.5" />
          <input
            v-model="searchKeyword"
            class="text-foreground placeholder:text-muted-foreground h-full flex-1 border-none bg-transparent text-xs outline-none"
            :placeholder="$t('components.modelSelector.searchPlaceholder')"
            type="text"
          />
        </div>
      </div>

      <ScrollArea class="flex-1 overflow-hidden">
        <div class="p-2">
          <template v-if="filteredProviderGroups.length === 0">
            <div class="text-muted-foreground py-8 text-center text-xs">
              {{ $t('components.modelSelector.noMatch') }}
            </div>
          </template>
          <template v-else>
            <div
              v-for="group in filteredProviderGroups"
              :key="group.provider"
              class="mb-1 rounded-md border border-border/70 bg-muted/10 p-1.5"
            >
              <div
                class="text-muted-foreground mb-1 px-1.5 text-[10px] font-medium"
              >
                <span class="inline-flex items-center gap-1.5">
                  <template v-if="isStringIcon(getProviderIcon(group.provider))">
                    <img
                      :src="getProviderIcon(group.provider)"
                      class="size-3.5"
                      alt=""
                    />
                  </template>
                  <component
                    :is="getProviderIcon(group.provider)"
                    v-else
                    class="size-3.5"
                  />
                  {{ getProviderLabel(group.provider) }}
                </span>
              </div>
              <div
                v-for="item in group.models"
                :key="item.id || `${group.provider}-${item.model}`"
                :class="[
                  String(modelId || '') === String(item.id || '') &&
                    'bg-accent',
                ]"
                class="hover:bg-accent flex h-8 cursor-pointer items-center gap-2 rounded-md px-2 transition-colors"
                @click="selectModel(item)"
              >
                <template v-if="isStringIcon(getProviderIcon(group.provider))">
                  <img
                    :src="getProviderIcon(group.provider)"
                    class="size-3.5 shrink-0"
                    alt=""
                  />
                </template>
                <component
                  :is="getProviderIcon(group.provider)"
                  v-else
                  class="size-3.5 shrink-0"
                />
                <span class="text-foreground flex-1 truncate text-xs">
                  {{ item.name || item.model || $t('models.card.unnamed') }}
                </span>
                <Check
                  v-if="String(modelId || '') === String(item.id || '')"
                  class="text-primary size-3.5 shrink-0"
                />
              </div>
            </div>
          </template>
        </div>
      </ScrollArea>

      <Teleport to="body">
        <div
          v-if="showConfigPanel"
          class="fixed inset-0 z-[2000] cursor-default"
          @click="showConfigPanel = false"
        ></div>
      </Teleport>

      <!-- 参数配置覆盖层 -->
      <div
        v-if="showConfigPanel"
        class="absolute inset-0 z-[2001] flex flex-col rounded-b-xl border border-border bg-card"
        @click.stop
      >
        <div
          class="flex items-center justify-between border-b border-border/70 px-4 py-3"
        >
          <span class="text-foreground text-sm font-medium">{{
            $t('components.modelSelector.paramsTitle')
          }}</span>
          <button
            class="hover:bg-accent text-muted-foreground flex size-6 items-center justify-center rounded-md transition-colors"
            type="button"
            @click="showConfigPanel = false"
          >
            <X class="size-4" />
          </button>
        </div>

        <div class="flex-1 space-y-3 overflow-y-auto px-4 py-3">
          <div class="flex items-center justify-between">
            <span class="text-foreground text-xs font-medium">{{
              $t('components.modelSelector.preset')
            }}</span>
            <Select
              :model-value="selectedPreset"
              @update:model-value="applyPreset(String($event || 'balanced'))"
            >
              <SelectTrigger
                class="bg-muted h-[30px] w-24 border border-border text-xs"
              >
                <SelectValue :placeholder="$t('components.modelSelector.loadingPreset')" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem
                  v-for="preset in CONFIG_PRESETS"
                  :key="preset.value"
                  :value="preset.value"
                  class="text-xs"
                >
                  {{ $t(preset.labelKey) }}
                </SelectItem>
              </SelectContent>
            </Select>
          </div>

          <div class="rounded-lg border border-border bg-background px-3 py-2">
            <div class="mb-2 text-xs text-muted-foreground">
              {{ $t('components.modelSelector.temperature', { value: (config.temperature ?? 0.7).toFixed(2) }) }}
            </div>
            <input
              :value="toTemperatureRange(config.temperature)"
              class="h-2 w-full cursor-pointer appearance-none rounded-full bg-muted accent-primary"
              max="100"
              min="0"
              step="1"
              type="range"
              @input="
                updateTemperature(
                  Number(($event.target as HTMLInputElement).value),
                )
              "
            />
          </div>
          <div class="rounded-lg border border-border bg-background px-3 py-2">
            <div class="mb-2 text-xs text-muted-foreground">
              {{ $t('components.modelSelector.topP', { value: (config.topP ?? 0.9).toFixed(2) }) }}
            </div>
            <input
              :value="toTopPRange(config.topP)"
              class="h-2 w-full cursor-pointer appearance-none rounded-full bg-muted accent-primary"
              max="100"
              min="0"
              step="1"
              type="range"
              @input="
                updateTopP(Number(($event.target as HTMLInputElement).value))
              "
            />
          </div>
          <div class="rounded-lg border border-border bg-background px-3 py-2">
            <div class="mb-2 text-xs text-muted-foreground">
              {{ $t('components.modelSelector.maxOutput', { value: config.maxOutputTokens ?? 2048 }) }}
            </div>
            <input
              :value="toTokenRange(config.maxOutputTokens)"
              class="h-2 w-full cursor-pointer appearance-none rounded-full bg-muted accent-primary"
              max="8192"
              min="256"
              step="128"
              type="range"
              @input="
                updateMaxOutputTokens(
                  Number(($event.target as HTMLInputElement).value),
                )
              "
            />
          </div>
        </div>
      </div>
    </div>
  </VbenPopover>
</template>
