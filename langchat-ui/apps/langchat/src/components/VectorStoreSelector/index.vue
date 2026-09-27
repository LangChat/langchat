<script setup lang="ts">
import type { AigcVectorStore } from '#/api/aigc/vector-store';

import { computed, ref } from 'vue';

import { Check, ChevronDown, Search } from '@vben/icons';
import { $t } from '@vben/locales';

import { ScrollArea, VbenPopover } from '@vben-core/shadcn-ui';

import { getVectorStoreProviderMeta } from '#/views/vector-stores/vector-store-meta';

interface Props {
  vectorStoreEntities?: AigcVectorStore[];
  vectorStoreId?: string;
}

const props = withDefaults(defineProps<Props>(), {
  vectorStoreEntities: () => [],
  vectorStoreId: '',
});

const emit = defineEmits<{
  'update:vectorStoreId': [value: string];
}>();

const searchKeyword = ref('');

const selectedVectorStore = computed(() =>
  props.vectorStoreEntities.find(
    (item) => String(item.id || '') === String(props.vectorStoreId || ''),
  ),
);

const providerGroups = computed(() => {
  const groups = new Map<string, AigcVectorStore[]>();
  for (const item of props.vectorStoreEntities) {
    const provider = String(item.provider || 'PGVECTOR').toUpperCase();
    const list = groups.get(provider) ?? [];
    list.push(item);
    groups.set(provider, list);
  }
  return [...groups.entries()]
    .map(([provider, stores]) => ({ provider, stores }))
    .toSorted((left, right) => left.provider.localeCompare(right.provider));
});

const filteredProviderGroups = computed(() => {
  const query = searchKeyword.value.trim().toLowerCase();
  if (!query) {
    return providerGroups.value;
  }
  return providerGroups.value
    .map((group) => ({
      provider: group.provider,
      stores: group.stores.filter((item) =>
        [item.name, item.provider, item.host, item.databaseName, item.tableName]
          .map((value) => String(value || '').toLowerCase())
          .some((value) => value.includes(query)),
      ),
    }))
    .filter((group) => group.stores.length > 0);
});

function selectVectorStore(item: AigcVectorStore) {
  emit('update:vectorStoreId', String(item.id || ''));
  searchKeyword.value = '';
}

function resolveStoreSummary(item: AigcVectorStore) {
  const address = item.host
    ? `${item.host}${item.port ? `:${item.port}` : ''}`
    : $t('common.status.notConfigured');
  const database = item.databaseName || item.tableName;
  return database ? `${address} · ${database}` : address;
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
        class="flex h-[34px] w-full items-center gap-2 rounded-md border border-border bg-background px-2.5 text-xs text-foreground transition-colors hover:border-primary"
        type="button"
      >
        <template v-if="selectedVectorStore">
          <img
            :src="getVectorStoreProviderMeta(selectedVectorStore.provider).icon"
            alt=""
            class="size-4 shrink-0"
          />
          <span class="min-w-0 flex-1 truncate text-left">
            {{ selectedVectorStore.name || $t('vectorStores.card.unnamed') }}
          </span>
          <span
            class="hidden shrink-0 text-[10px] text-muted-foreground sm:inline"
          >
            {{ getVectorStoreProviderMeta(selectedVectorStore.provider).label }}
          </span>
        </template>
        <span v-else class="flex-1 text-left text-muted-foreground">
          {{ $t('components.vectorStoreSelector.placeholder') }}
        </span>
        <ChevronDown class="size-3.5 shrink-0 text-muted-foreground" />
      </button>
    </template>

    <div class="flex h-[330px] flex-col">
      <div class="space-y-2 border-b border-border/40 px-3 pb-3 pt-3">
        <div class="text-xs font-medium text-foreground">
          {{ $t('components.vectorStoreSelector.label') }}
        </div>
        <div
          class="flex h-[34px] items-center gap-2 rounded-md bg-muted px-2.5"
        >
          <Search class="size-3.5 text-muted-foreground" />
          <input
            v-model="searchKeyword"
            :placeholder="
              $t('components.vectorStoreSelector.searchPlaceholder')
            "
            class="h-full flex-1 border-none bg-transparent text-xs text-foreground outline-none placeholder:text-muted-foreground"
            type="text"
          />
        </div>
      </div>

      <ScrollArea class="flex-1 overflow-hidden">
        <div class="p-2">
          <div
            v-if="filteredProviderGroups.length === 0"
            class="py-8 text-center text-xs text-muted-foreground"
          >
            {{ $t('components.vectorStoreSelector.noMatch') }}
          </div>
          <div
            v-for="group in filteredProviderGroups"
            v-else
            :key="group.provider"
            class="mb-1 rounded-md border border-border/70 bg-muted/10 p-1.5"
          >
            <div
              class="mb-1 flex items-center gap-1.5 px-1.5 text-[10px] font-medium text-muted-foreground"
            >
              <img
                :src="getVectorStoreProviderMeta(group.provider).icon"
                alt=""
                class="size-3.5"
              />
              {{ getVectorStoreProviderMeta(group.provider).label }}
            </div>

            <button
              v-for="item in group.stores"
              :key="item.id || `${group.provider}-${item.name}`"
              :class="
                String(vectorStoreId || '') === String(item.id || '') &&
                'bg-accent'
              "
              class="flex min-h-11 w-full cursor-pointer items-center gap-2 rounded-md px-2 py-1.5 text-left transition-colors hover:bg-accent"
              type="button"
              @click="selectVectorStore(item)"
            >
              <img
                :src="getVectorStoreProviderMeta(group.provider).icon"
                alt=""
                class="size-4 shrink-0"
              />
              <span class="min-w-0 flex-1">
                <span
                  class="block truncate text-xs font-medium text-foreground"
                >
                  {{ item.name || $t('vectorStores.card.unnamed') }}
                </span>
                <span
                  class="mt-0.5 block truncate text-[10px] text-muted-foreground"
                >
                  {{ resolveStoreSummary(item) }}
                </span>
              </span>
              <Check
                v-if="String(vectorStoreId || '') === String(item.id || '')"
                class="size-3.5 shrink-0 text-primary"
              />
            </button>
          </div>
        </div>
      </ScrollArea>
    </div>
  </VbenPopover>
</template>
