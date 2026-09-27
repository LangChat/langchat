import type { AigcModel } from '#/api/aigc/model';

import type { LabelOption } from '#/views/shared/aigc/options';

import { computed, ref } from 'vue';

import { $t } from '@vben/locales';

import { knowledgeApi } from '#/api/aigc/knowledge';
import { mcpApi } from '#/api/aigc/mcp';
import { modelApi } from '#/api/aigc/model';
import { skillApi } from '#/api/aigc/skill';
import { vectorStoreApi } from '#/api/aigc/vector-store';

interface LookupOptions {
  knowledges?: boolean;
  mcps?: boolean;
  models?: boolean;
  skills?: boolean;
  vectorStores?: boolean;
}

function toOption(value: number | string, label: string): LabelOption {
  return {
    label,
    value,
  };
}

/**
 * AIGC 关联下拉数据。原始数据与展示 label 分离存储，
 * label 在 computed 中按当前语言生成，语言切换时实时更新。
 */
export function useAigcLookups(options: LookupOptions = {}) {
  const models = ref<AigcModel[]>([]);
  const vectorStores = ref<Awaited<ReturnType<typeof vectorStoreApi.list>>>(
    [],
  );
  const knowledges = ref<Awaited<ReturnType<typeof knowledgeApi.list>>>([]);
  const mcps = ref<Awaited<ReturnType<typeof mcpApi.list>>>([]);
  const skills = ref<Awaited<ReturnType<typeof skillApi.list>>>([]);

  async function loadLookups() {
    const tasks: Promise<void>[] = [];

    if (options.models) {
      tasks.push(
        modelApi.list().then((items) => {
          models.value = items;
        }),
      );
    }

    if (options.vectorStores) {
      tasks.push(
        vectorStoreApi.list().then((items) => {
          vectorStores.value = items;
        }),
      );
    }

    if (options.knowledges) {
      tasks.push(
        knowledgeApi.list().then((items) => {
          knowledges.value = items;
        }),
      );
    }

    if (options.mcps) {
      tasks.push(
        mcpApi.list().then((items) => {
          mcps.value = items;
        }),
      );
    }

    if (options.skills) {
      tasks.push(
        skillApi.list().then((items) => {
          skills.value = items;
        }),
      );
    }

    await Promise.all(tasks);
  }

  const modelOptions = computed<LabelOption[]>(() =>
    models.value.map((item) =>
      toOption(
        item.id ?? '',
        `${item.name || item.model || $t('models.card.unnamed')} · ${item.provider || 'UNKNOWN'}`,
      ),
    ),
  );

  const vectorStoreOptions = computed<LabelOption[]>(() =>
    vectorStores.value.map((item) =>
      toOption(
        item.id ?? '',
        `${item.name || $t('vectorStores.card.unnamed')} · ${item.provider || 'UNKNOWN'}`,
      ),
    ),
  );

  const knowledgeOptions = computed<LabelOption[]>(() =>
    knowledges.value.map((item) =>
      toOption(item.id ?? '', item.name || $t('knowledge.card.unnamed')),
    ),
  );

  const mcpOptions = computed<LabelOption[]>(() =>
    mcps.value.map((item) =>
      toOption(item.id ?? '', item.name || $t('mcp.card.unnamed')),
    ),
  );

  const skillOptions = computed<LabelOption[]>(() =>
    skills.value.map((item) =>
      toOption(item.id ?? '', item.title || item.name || $t('skills.empty.unnamed')),
    ),
  );

  return {
    lookups: computed(() => ({
      knowledges: knowledgeOptions.value,
      mcps: mcpOptions.value,
      modelEntities: models.value,
      models: modelOptions.value,
      skills: skillOptions.value,
      vectorStores: vectorStoreOptions.value,
    })),
    loadLookups,
  };
}
