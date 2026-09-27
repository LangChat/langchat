<script lang="ts" setup>
import type {AigcModel} from '#/api/aigc/model';

import {computed, onMounted, ref, watch} from 'vue';

import {Page} from '@vben/common-ui';
import {
  Bot,
  BotOff,
  Building2,
  Eraser,
  Factory,
  ListFilter,
  Plus,
  RefreshCcw,
  RotateCcw,
  Search,
  Shapes,
} from '@vben/icons';
import { $t } from '@vben/locales';

import {NButton, NEmpty, NInput, NPagination, NSpin} from 'naive-ui';

import {dialog, message} from '#/adapter/naive';
import {modelApi} from '#/api/aigc/model';

import ModelCard from './card.vue';
import ModelEdit from './edit.vue';
import {
  getAllProviderCapabilities,
  getModelProviderOptions,
  getModelTypeConfigItems,
  getModelTypeIcon,
  getModelTypeLabel,
  getModelTypeMeta,
  getProviderIcon,
  getProviderRecommendedModels,
  getProviderSupportedTypes,
  normalizeModelType,
} from './model-meta';

const loading = ref(false);
const saving = ref(false);
const showEdit = ref(false);
const keyword = ref('');
const selectedProvider = ref('');
const selectedType = ref('');
const currentPage = ref(1);
const pageSize = ref(9);
const items = ref<AigcModel[]>([]);
const currentItem = ref<null | Partial<AigcModel>>(null);

const providerOptions = getModelProviderOptions();

/**
 * 模型类型页签按供应商能力动态派生：
 * 选中供应商时仅展示该供应商已适配的类型，未选中时展示全部供应商能力并集。
 */
const typeOptions = computed(() => {
  const capabilityTypes = selectedProvider.value
    ? getProviderSupportedTypes(selectedProvider.value)
    : getAllProviderCapabilities();
  return capabilityTypes.map((type) => ({
    label: getModelTypeLabel(type),
    value: type,
  }));
});

const providerFilterOptions = computed(() =>
  [
    {
      label: $t('models.list.allProviders'),
      value: '',
    },
    ...providerOptions,
    ...[...new Set(items.value.map((item) => item.provider).filter(Boolean))]
      .filter(
        (provider) =>
          !providerOptions.some((option) => option.value === provider),
      )
      .map((provider) => ({
        label: String(provider),
        value: String(provider),
      })),
  ].map((option) => ({
    ...option,
    count: option.value
      ? items.value.filter((item) => item.provider === option.value).length
      : items.value.length,
    icon:
      option.value === ''
        ? Building2
        : getProviderIcon(String(option.value)) || Bot,
  })),
);

const typeFilterOptions = computed(() => {
  const sorted = [...typeOptions.value].sort((a, b) => {
    if (a.value === 'TEXT2TEXT') return -1;
    if (b.value === 'TEXT2TEXT') return 1;
    return 0;
  });
  return [
    {
      label: $t('common.labels.all'),
      value: '',
      description: $t('models.list.allTypesDescription'),
      icon: Shapes,
    },
    ...sorted.map((option) => ({
      ...option,
      description: getModelTypeMeta(String(option.value)).description,
      icon: getModelTypeIcon(String(option.value ?? '')),
    })),
  ];
});

const normalizedSelectedType = computed(() =>
  selectedType.value ? normalizeModelType(selectedType.value) : '',
);

const filteredItems = computed(() => {
  const query = keyword.value.trim().toLowerCase();
  return items.value.filter((item) => {
    const matchProvider =
      !selectedProvider.value || item.provider === selectedProvider.value;
    const matchType =
      !normalizedSelectedType.value ||
      normalizeModelType(item.type) === normalizedSelectedType.value;
    const matchKeyword =
      !query ||
      [item.name, item.model, item.provider, item.type]
        .map((value) => String(value ?? '').toLowerCase())
        .some((value) => value.includes(query));
    return matchProvider && matchType && matchKeyword;
  });
});

const pagedItems = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value;
  return filteredItems.value.slice(start, start + pageSize.value);
});

const pageDescription = computed(() => {
  const providerLabel = providerFilterOptions.value.find(
    (item) => item.value === selectedProvider.value,
  )?.label;
  const typeLabel = typeFilterOptions.value.find(
    (item) => item.value === selectedType.value,
  )?.label;
  if (selectedProvider.value && selectedType.value) {
    return $t('models.list.breadcrumbBoth', {
      provider: providerLabel,
      type: typeLabel,
    });
  }
  if (selectedProvider.value) {
    return $t('models.list.breadcrumbProvider', { provider: providerLabel });
  }
  if (selectedType.value) {
    return $t('models.list.breadcrumbType', { type: typeLabel });
  }
  return $t('models.list.breadcrumbAll');
});

const activeProviderMeta = computed(() => {
  const selected = providerFilterOptions.value.find(
    (item) => item.value === selectedProvider.value,
  );
  return {
    icon: selectedProvider.value
      ? getProviderIcon(selectedProvider.value)
      : Building2,
    label: selected?.label || $t('models.list.allProviders'),
  };
});

const activeTypeMeta = computed(() => {
  const selected = typeFilterOptions.value.find(
    (item) => item.value === selectedType.value,
  );
  return {
    icon: selectedType.value ? getModelTypeIcon(selectedType.value) : Shapes,
    label: selected?.label || $t('models.list.allTypes'),
  };
});

const activeTypeConfigItems = computed(() => {
  if (!selectedType.value) {
    return [];
  }
  return getModelTypeConfigItems(selectedType.value);
});

const recommendedModels = computed(() => {
  if (!selectedProvider.value || !selectedType.value) {
    return [];
  }
  return getProviderRecommendedModels(
    selectedProvider.value,
    selectedType.value,
  );
});

watch([keyword, selectedProvider, selectedType], () => {
  currentPage.value = 1;
});

// 切换供应商后，若已选类型不在该供应商能力范围内则重置
watch(selectedProvider, () => {
  const available = typeOptions.value.map((option) => String(option.value));
  if (selectedType.value && !available.includes(selectedType.value)) {
    selectedType.value = "";
  }
});

async function loadList() {
  loading.value = true;
  try {
    items.value = await modelApi.list();
  } finally {
    loading.value = false;
  }
}

function handleCreate() {
  currentItem.value = {
    provider: selectedProvider.value || 'OPENAI',
    type: selectedType.value || 'TEXT2TEXT',
  };
  showEdit.value = true;
}

function handleEdit(item: AigcModel) {
  currentItem.value = item;
  showEdit.value = true;
}

async function handleDelete(item: AigcModel) {
  if (!item.id) {
    return;
  }
  dialog.warning({
    closable: false,
    content: $t('common.messages.deleteConfirmContent', {
      name: item.name || item.model || $t('models.card.unnamed'),
    }),
    negativeText: $t('common.actions.cancel'),
    positiveText: $t('common.actions.confirmDelete'),
    title: $t('models.messages.deleteTitle'),
    onPositiveClick: async () => {
      await modelApi.remove(item.id!);
      message.success($t('models.messages.deleted'));
      await loadList();
    },
  });
}

async function handleSave(payload: Partial<AigcModel>) {
  saving.value = true;
  try {
    if (currentItem.value?.id) {
      await modelApi.update(currentItem.value.id, payload);
      message.success($t('models.messages.updated'));
    } else {
      await modelApi.create(payload);
      message.success($t('models.messages.created'));
    }
    showEdit.value = false;
    await loadList();
  } finally {
    saving.value = false;
  }
}

function resetFilters() {
  keyword.value = '';
  selectedProvider.value = '';
  selectedType.value = '';
}

function handleTypeFilter(type: string) {
  selectedType.value = type ? normalizeModelType(type) : '';
  currentPage.value = 1;
}

onMounted(loadList);
</script>

<template>
  <Page>
    <div class="flex flex-col gap-2 h-full">
      <div class="flex flex-col gap-3 md:flex-row md:items-stretch h-full">
        <aside
          class="rounded-xl border border-border bg-card p-3 md:w-[236px] md:flex-none md:overflow-y-auto"
        >
          <div
            class="flex items-center gap-2 px-2 pb-2.5 text-sm font-semibold text-foreground"
          >
            <Factory class="size-4 text-primary" />
            {{ $t('common.labels.provider') }}
          </div>

          <div class="space-y-1.5">
            <button
              v-for="option in providerFilterOptions"
              :key="String(option.value)"
              :class="
                selectedProvider === option.value
                  ? 'border-primary/45 bg-primary/8 text-primary'
                  : 'border-border/70 bg-background text-foreground hover:border-primary/30 hover:bg-primary/5 hover:text-primary'
              "
              class="flex w-full cursor-pointer items-center justify-between rounded-lg border px-2.5 py-2 text-left transition-colors"
              type="button"
              @click="selectedProvider = String(option.value)"
            >
              <span class="flex min-w-0 items-center gap-2">
                <span
                  class="inline-flex size-6 shrink-0 items-center justify-center rounded-md border border-border/70 bg-muted/30"
                >
                  <template v-if="typeof option.icon === 'string'">
                    <img
                      :src="option.icon"
                      class="size-3.5 shrink-0"
                      alt=""
                    />
                  </template>
                  <component
                    :is="option.icon"
                    v-else
                    class="size-3.5 shrink-0 text-muted-foreground"
                  />
                </span>
                <span
                  :class="
                    selectedProvider === option.value
                      ? 'bg-primary'
                      : 'bg-border'
                  "
                  class="h-3.5 w-0.5 rounded-full"
                ></span>
                <span class="truncate text-[13px] font-medium leading-5">
                  {{ option.label }}
                </span>
              </span>
              <span
                :class="
                  selectedProvider === option.value
                    ? 'bg-primary/10 text-primary'
                    : 'bg-muted text-muted-foreground'
                "
                class="ml-2 rounded-full px-1.5 py-0 text-[10px]"
              >
                {{ option.count }}
              </span>
            </button>
          </div>
        </aside>

        <section class="min-w-0 md:flex-1">
          <div class="flex min-w-0 flex-col gap-3">
            <div
              class="rounded-xl border border-border bg-card p-2.5"
            >
              <div class="flex flex-col gap-2.5">
                <div
                  class="flex flex-col gap-2.5 xl:flex-row xl:items-center xl:justify-between"
                >
                  <div class="min-w-0">
                    <div
                      class="flex items-center gap-2 text-sm font-semibold text-foreground"
                    >
                      <Bot class="size-5 text-primary" />
                      {{ $t('models.list.title') }}
                    </div>
                    <div
                      class="mt-0.5 flex flex-wrap items-center gap-1.5 text-[10px] leading-4 text-muted-foreground"
                    >
                      <span>{{ pageDescription }}</span>
                      <span class="h-1 w-1 rounded-full bg-border"></span>
                      <span
                        class="inline-flex items-center gap-1 rounded-full bg-muted px-2 py-0.5"
                      >
                        <template v-if="typeof activeProviderMeta.icon === 'string'">
                          <img
                            :src="activeProviderMeta.icon"
                            class="size-3.5"
                            alt=""
                          />
                        </template>
                        <component
                          :is="activeProviderMeta.icon"
                          v-else
                          class="size-3.5"
                        />
                        {{ activeProviderMeta.label }}
                      </span>
                      <span
                        class="inline-flex items-center gap-1 rounded-full bg-muted px-2 py-0.5"
                      >
                        <component :is="activeTypeMeta.icon" class="size-3.5" />
                        {{ activeTypeMeta.label }}
                      </span>
                    </div>
                  </div>

                  <div class="flex w-full items-center gap-2 xl:w-auto">
                    <div class="w-full xl:w-[280px]">
                      <NInput
                        v-model:value="keyword"
                        clearable
                        :placeholder="$t('models.list.searchPlaceholder')"
                      >
                        <template #prefix>
                          <Search class="size-4 text-muted-foreground" />
                        </template>
                      </NInput>
                    </div>
                    <NButton secondary @click="resetFilters">
                      <template #icon>
                        <RotateCcw class="size-4" />
                      </template>
                      {{ $t('common.actions.reset') }}
                    </NButton>
                    <NButton secondary @click="loadList">
                      <template #icon>
                        <RefreshCcw class="size-4" />
                      </template>
                      {{ $t('common.actions.refresh') }}
                    </NButton>
                    <NButton type="primary" @click="handleCreate">
                      <template #icon>
                        <Plus class="size-4" />
                      </template>
                      {{ $t('models.form.createTitle') }}
                    </NButton>
                  </div>
                </div>

                <div class="flex flex-col gap-1.5">
                  <div class="flex flex-wrap gap-2">
                      <button
                        v-for="option in typeFilterOptions"
                        :key="String(option.value)"
                        :class="
                          selectedType === option.value
                            ? 'border-primary bg-primary/10 text-primary shadow-sm'
                            : 'border-border/80 bg-background text-foreground hover:border-primary/30 hover:bg-primary/5'
                        "
                        class="flex h-9 cursor-pointer items-center gap-2 rounded-lg border px-3 text-left transition-colors"
                        type="button"
                        :aria-pressed="selectedType === option.value"
                        @click="handleTypeFilter(String(option.value))"
                      >
                        <span
                          class="inline-flex size-6 items-center justify-center rounded-md border border-border/70 bg-muted/40"
                        >
                          <component :is="option.icon" class="size-3" />
                        </span>
                        <span class="text-xs font-medium">{{ option.label }}</span>
                      </button>
                  </div>

                  <div
                    class="inline-flex items-center gap-1.5 text-[10px] text-muted-foreground"
                  >
                    <ListFilter class="size-3.5" />
                    {{ $t('models.list.total', { count: filteredItems.length }) }}
                  </div>
                </div>

                <div
                  v-if="selectedProvider && selectedType"
                  class="grid gap-1.5 lg:grid-cols-2"
                >
                  <div
                    class="rounded-lg border border-border/70 bg-background/80 p-2"
                  >
                    <div
                      class="mb-1 flex items-center gap-1.5 text-[10px] text-muted-foreground"
                    >
                      <component
                        :is="activeProviderMeta.icon"
                        class="size-3.5"
                      />
                      {{ $t('models.list.recommended') }}
                    </div>
                    <div class="flex flex-wrap gap-1">
                      <span
                        v-for="modelName in recommendedModels"
                        :key="modelName"
                        class="rounded-md border border-border/80 bg-muted/30 px-1.5 py-0.5 text-[10px] text-foreground"
                      >
                        {{ modelName }}
                      </span>
                      <span
                        v-if="recommendedModels.length === 0"
                        class="text-[10px] text-muted-foreground"
                      >
                        {{ $t('models.list.noRecommended') }}
                      </span>
                    </div>
                  </div>
                  <div
                    class="rounded-lg border border-border/70 bg-background/80 p-2"
                  >
                    <div
                      class="mb-1 flex items-center gap-1.5 text-[10px] text-muted-foreground"
                    >
                      <component :is="activeTypeMeta.icon" class="size-3.5" />
                      {{ $t('models.list.suggestedConfig') }}
                    </div>
                    <div class="flex flex-wrap gap-1">
                      <span
                        v-for="configName in activeTypeConfigItems"
                        :key="configName"
                        class="rounded-md border border-border/80 bg-muted/30 px-1.5 py-0.5 text-[10px] text-foreground"
                      >
                        {{ configName }}
                      </span>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <NSpin :show="loading">
              <div
                v-if="pagedItems.length > 0"
                class="grid gap-2.5 md:grid-cols-1 lg:grid-cols-2 xl:grid-cols-3 2xl:grid-cols-4"
              >
                <ModelCard
                  v-for="item in pagedItems"
                  :key="item.id || item.model"
                  :item="item"
                  @delete="handleDelete"
                  @edit="handleEdit"
                />
              </div>
              <div
                v-else
                class="flex min-h-[200px] items-center justify-center rounded-xl border border-dashed border-border bg-card p-5"
              >
                <NEmpty :description="$t('models.list.empty')">
                  <template #icon>
                    <BotOff class="mx-auto size-8 text-muted-foreground" />
                  </template>
                  <template #extra>
                    <NButton secondary @click="resetFilters">
                      <template #icon>
                        <Eraser class="size-4" />
                      </template>
                      {{ $t('common.actions.clearFilter') }}
                    </NButton>
                  </template>
                </NEmpty>
              </div>
            </NSpin>

            <div
              v-if="filteredItems.length > pageSize"
              class="flex justify-end"
            >
              <NPagination
                v-model:page="currentPage"
                v-model:page-size="pageSize"
                :item-count="filteredItems.length"
                :page-sizes="[6, 9, 12, 18]"
                show-size-picker
              />
            </div>
          </div>
        </section>
      </div>

      <ModelEdit
        v-model:show="showEdit"
        :model-value="currentItem"
        :saving="saving"
        @save="handleSave"
      />
    </div>
  </Page>
</template>
