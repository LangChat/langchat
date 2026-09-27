<script lang="ts" setup>
import type { FormInst, FormRules, SelectOption } from 'naive-ui';

import type { AigcKnowledge } from '#/api/aigc/knowledge';
import type { AigcModel } from '#/api/aigc/model';
import type { AigcVectorStore } from '#/api/aigc/vector-store';

import { computed, reactive, ref, watch } from 'vue';

import {
  DatabaseZap,
  Info,
  RotateCcw,
  Save,
  SlidersHorizontal,
} from '@vben/icons';
import { $t } from '@vben/locales';

import {
  NButton,
  NForm,
  NFormItem,
  NInput,
  NInputNumber,
  NSelect,
} from 'naive-ui';

import ModelSelector from '#/components/ModelSelector/index.vue';
import VectorStoreSelector from '#/components/VectorStoreSelector/index.vue';
import { normalizeModelType } from '#/views/models/model-meta';
import { aigcCommonTagOptions } from '#/views/shared/aigc/options';
import { parseTagList, stringifyTagList } from '#/views/shared/aigc/tags';

interface Props {
  modelEntities: AigcModel[];
  modelValue: AigcKnowledge | null;
  saving?: boolean;
  vectorStoreEntities: AigcVectorStore[];
}

interface KnowledgeSettingsForm {
  description: string;
  maxResults: null | number;
  minScore: null | number;
  name: string;
  tags: string[];
  vectorModelId: null | string;
  vectorStoreId: null | string;
}

const props = withDefaults(defineProps<Props>(), {
  saving: false,
});

const emit = defineEmits<{
  save: [payload: Partial<AigcKnowledge>];
}>();

const formRef = ref<FormInst | null>(null);
const form = reactive<KnowledgeSettingsForm>({
  description: '',
  maxResults: 5,
  minScore: 0.5,
  name: '',
  tags: [],
  vectorModelId: null,
  vectorStoreId: null,
});
const initialSnapshot = ref('');
const vectorModelConfig = {};

const rules = computed<FormRules>(() => ({
  name: {
    message: $t('knowledge.settings.validation.name'),
    required: true,
    trigger: ['blur', 'input'],
  },
  vectorModelId: {
    message: $t('knowledge.settings.validation.vectorModel'),
    required: true,
    trigger: ['blur', 'change'],
  },
  vectorStoreId: {
    message: $t('knowledge.settings.validation.vectorStore'),
    required: true,
    trigger: ['blur', 'change'],
  },
}));

const serializedForm = computed(() =>
  JSON.stringify({
    description: form.description.trim(),
    maxResults: form.maxResults,
    minScore: form.minScore,
    name: form.name.trim(),
    tags: form.tags.toSorted(),
    vectorModelId: form.vectorModelId,
    vectorStoreId: form.vectorStoreId,
  }),
);

const hasChanges = computed(
  () => serializedForm.value !== initialSnapshot.value,
);
const tagOptions = computed<SelectOption[]>(() =>
  aigcCommonTagOptions().map((item) => ({
    label: item.label,
    value: item.value,
  })),
);

function resetForm() {
  const value = props.modelValue;
  const vectorModel = props.modelEntities.find(
    (item) => String(item.id || '') === String(value?.vectorModelId || ''),
  );
  const vectorStore = props.vectorStoreEntities.find(
    (item) => String(item.id || '') === String(value?.vectorStoreId || ''),
  );
  form.description = value?.description ?? '';
  form.maxResults = value?.maxResults ?? 5;
  form.minScore = value?.minScore ?? 0.5;
  form.name = value?.name ?? '';
  form.tags = parseTagList(value?.tags);
  form.vectorModelId =
    vectorModel && normalizeModelType(vectorModel.type) === 'EMBEDDINGS'
      ? (vectorModel.id ?? null)
      : null;
  form.vectorStoreId = vectorStore?.id ?? null;
  initialSnapshot.value = serializedForm.value;
  formRef.value?.restoreValidation();
}

async function handleSave() {
  try {
    await formRef.value?.validate();
  } catch {
    return;
  }

  emit('save', {
    description: form.description.trim(),
    maxResults: form.maxResults ?? 5,
    minScore: form.minScore ?? 0.5,
    name: form.name.trim(),
    tags: stringifyTagList(form.tags),
    vectorModelId: form.vectorModelId ?? undefined,
    vectorStoreId: form.vectorStoreId ?? undefined,
  });
}

watch(
  () => [props.modelValue, props.modelEntities, props.vectorStoreEntities],
  () => resetForm(),
  { immediate: true },
);
</script>

<template>
  <div class="mx-auto w-full max-w-6xl pb-4">
    <div
      class="mb-4 flex flex-col gap-3 rounded-lg border border-border bg-muted/30 px-4 py-3 sm:flex-row sm:items-center sm:justify-between"
    >
      <div class="min-w-0">
        <div class="text-sm font-semibold text-foreground">
          {{ $t('knowledge.settings.title') }}
        </div>
        <p class="mt-1 text-xs leading-5 text-muted-foreground">
          {{ $t('knowledge.settings.description') }}
        </p>
      </div>
      <div class="flex shrink-0 items-center gap-2">
        <NButton :disabled="!hasChanges || saving" secondary @click="resetForm">
          <template #icon><RotateCcw class="size-4" /></template>
          {{ $t('common.actions.reset') }}
        </NButton>
        <NButton
          :disabled="!modelValue || !hasChanges"
          :loading="saving"
          type="primary"
          @click="handleSave"
        >
          <template #icon><Save class="size-4" /></template>
          {{ $t('knowledge.settings.save') }}
        </NButton>
      </div>
    </div>

    <NForm
      ref="formRef"
      :model="form"
      :rules="rules"
      label-placement="top"
      require-mark-placement="right-hanging"
    >
      <div
        class="grid gap-4 lg:grid-cols-[minmax(0,1.05fr)_minmax(360px,0.95fr)]"
      >
        <section class="rounded-lg border border-border bg-card p-4">
          <div class="mb-4 flex items-start gap-3 border-b border-border pb-4">
            <div
              class="flex size-9 shrink-0 items-center justify-center rounded-lg border border-primary/20 bg-primary/5 text-primary"
            >
              <SlidersHorizontal class="size-4" />
            </div>
            <div>
              <h2 class="text-sm font-semibold text-foreground">
                {{ $t('knowledge.settings.basicTitle') }}
              </h2>
              <p class="mt-1 text-xs leading-5 text-muted-foreground">
                {{ $t('knowledge.settings.basicDescription') }}
              </p>
            </div>
          </div>

          <NFormItem :label="$t('knowledge.form.name')" path="name">
            <NInput
              v-model:value="form.name"
              :placeholder="$t('knowledge.settings.namePlaceholder')"
            />
          </NFormItem>

          <NFormItem
            :label="$t('knowledge.form.description')"
            path="description"
          >
            <NInput
              v-model:value="form.description"
              :autosize="{ minRows: 5, maxRows: 10 }"
              :placeholder="$t('knowledge.form.descriptionPlaceholder')"
              type="textarea"
            />
          </NFormItem>

          <NFormItem :label="$t('common.labels.tags')" path="tags">
            <NSelect
              v-model:value="form.tags"
              :options="tagOptions"
              :placeholder="$t('common.placeholder.selectTags')"
              clearable
              filterable
              multiple
            />
          </NFormItem>
        </section>

        <section class="rounded-lg border border-border bg-card p-4">
          <div class="mb-4 flex items-start gap-3 border-b border-border pb-4">
            <div
              class="flex size-9 shrink-0 items-center justify-center rounded-lg border border-primary/20 bg-primary/5 text-primary"
            >
              <DatabaseZap class="size-4" />
            </div>
            <div>
              <h2 class="text-sm font-semibold text-foreground">
                {{ $t('knowledge.settings.retrievalTitle') }}
              </h2>
              <p class="mt-1 text-xs leading-5 text-muted-foreground">
                {{ $t('knowledge.settings.retrievalDescription') }}
              </p>
            </div>
          </div>

          <NFormItem
            :label="$t('knowledge.form.vectorStore')"
            path="vectorStoreId"
          >
            <VectorStoreSelector
              :vector-store-entities="vectorStoreEntities"
              :vector-store-id="form.vectorStoreId || ''"
              @update:vector-store-id="form.vectorStoreId = $event"
            />
          </NFormItem>

          <NFormItem
            :label="$t('knowledge.form.vectorModel')"
            path="vectorModelId"
          >
            <ModelSelector
              :allowed-types="['EMBEDDINGS']"
              :config="vectorModelConfig"
              :model-entities="modelEntities"
              :model-id="form.vectorModelId || ''"
              :show-config="false"
              @update:model-id="form.vectorModelId = $event"
            />
          </NFormItem>

          <div class="grid grid-cols-1 gap-x-3 sm:grid-cols-2">
            <NFormItem
              :label="$t('knowledge.form.maxResults')"
              path="maxResults"
            >
              <NInputNumber
                v-model:value="form.maxResults"
                :max="100"
                :min="1"
                class="w-full"
              />
              <template #feedback>
                {{ $t('knowledge.settings.maxResultsHint') }}
              </template>
            </NFormItem>

            <NFormItem :label="$t('knowledge.form.minScore')" path="minScore">
              <NInputNumber
                v-model:value="form.minScore"
                :max="1"
                :min="0"
                :precision="2"
                :step="0.05"
                class="w-full"
              />
              <template #feedback>
                {{ $t('knowledge.settings.minScoreHint') }}
              </template>
            </NFormItem>
          </div>

          <div
            class="mt-1 flex items-start gap-2 rounded-md border border-warning/30 bg-warning/5 px-3 py-2.5 text-xs leading-5 text-muted-foreground"
          >
            <Info class="mt-0.5 size-4 shrink-0 text-warning" />
            <span>{{ $t('knowledge.settings.reindexHint') }}</span>
          </div>
        </section>
      </div>
    </NForm>
  </div>
</template>
