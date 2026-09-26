import type { LabelOption } from '#/views/shared/aigc/options';
import type { AigcModel } from '#/api/aigc/model';

import { computed, ref } from 'vue';

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
 * AIGC 关联下拉数据。
 */
export function useAigcLookups(options: LookupOptions = {}) {
  const modelOptions = ref<LabelOption[]>([]);
  const modelEntities = ref<AigcModel[]>([]);
  const vectorStoreOptions = ref<LabelOption[]>([]);
  const knowledgeOptions = ref<LabelOption[]>([]);
  const mcpOptions = ref<LabelOption[]>([]);
  const skillOptions = ref<LabelOption[]>([]);

  async function loadLookups() {
    const tasks: Promise<void>[] = [];

    if (options.models) {
      tasks.push(
        modelApi.list().then((items) => {
          modelEntities.value = items;
          modelOptions.value = items.map((item) =>
            toOption(
              item.id ?? '',
              `${item.name || item.model || '未命名模型'} · ${item.provider || 'UNKNOWN'}`,
            ),
          );
        }),
      );
    }

    if (options.vectorStores) {
      tasks.push(
        vectorStoreApi.list().then((items) => {
          vectorStoreOptions.value = items.map((item) =>
            toOption(
              item.id ?? '',
              `${item.name || '未命名向量库'} · ${item.provider || 'UNKNOWN'}`,
            ),
          );
        }),
      );
    }

    if (options.knowledges) {
      tasks.push(
        knowledgeApi.list().then((items) => {
          knowledgeOptions.value = items.map((item) =>
            toOption(item.id ?? '', item.name || '未命名知识库'),
          );
        }),
      );
    }

    if (options.mcps) {
      tasks.push(
        mcpApi.list().then((items) => {
          mcpOptions.value = items.map((item) =>
            toOption(item.id ?? '', item.name || '未命名MCP'),
          );
        }),
      );
    }

    if (options.skills) {
      tasks.push(
        skillApi.list().then((items) => {
          skillOptions.value = items.map((item) =>
            toOption(
              item.id ?? '',
              item.title || item.name || '未命名技能',
            ),
          );
        }),
      );
    }

    await Promise.all(tasks);
  }

  return {
    lookups: computed(() => ({
      knowledges: knowledgeOptions.value,
      mcps: mcpOptions.value,
      modelEntities: modelEntities.value,
      models: modelOptions.value,
      skills: skillOptions.value,
      vectorStores: vectorStoreOptions.value,
    })),
    loadLookups,
  };
}
