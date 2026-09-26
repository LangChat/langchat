import type { Ref } from 'vue';

import type { AigcModel } from '#/api/aigc/model';

import { computed, ref, watch } from 'vue';

import { modelApi } from '#/api/aigc/model';

/** LcChat 输入框可选择的模型类型(与系统 ModelSelector 的默认范围保持一致)。 */
const CHAT_MODEL_TYPES = new Set(['CHAT', 'REASONING']);

/**
 * 对话模型选择:接入系统公共模型 API(modelApi)。
 * 加载可用对话模型列表,并维护 v-model:modelId 的当前选中项;
 * 未指定时默认选中列表中的第一个模型。
 */
export function useModelSelector(modelId: Ref<string>) {
  const models = ref<AigcModel[]>([]);
  const loading = ref(false);
  const loaded = ref(false);

  const currentModel = computed(() =>
    models.value.find(
      (item) => String(item.id || '') === String(modelId.value || ''),
    ),
  );

  const currentLabel = computed(() => {
    const current = currentModel.value;
    if (!current) {
      return '选择模型';
    }
    return current.name || current.model || '未命名模型';
  });

  async function loadModels() {
    if (loaded.value || loading.value) {
      return;
    }
    loading.value = true;
    try {
      const items = await modelApi.list();
      models.value = items.filter((item) =>
        CHAT_MODEL_TYPES.has(String(item.type || '').toUpperCase()),
      );
      loaded.value = true;
    } finally {
      loading.value = false;
    }
  }

  // 列表就绪后兜底一个默认选中项
  watch(models, (items) => {
    if (!modelId.value && items.length > 0) {
      modelId.value = String(items[0]?.id || '');
    }
  });

  return { currentLabel, currentModel, loadModels, loading, models };
}
