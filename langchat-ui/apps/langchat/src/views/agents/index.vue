<script lang="ts" setup>
import type {AigcAgent} from '#/api/aigc/agent';

import {computed, markRaw, onMounted, reactive, ref, watch} from 'vue';
import {useRouter} from 'vue-router';

import {Page} from '@vben/common-ui';
import {Plus, RefreshCcw} from '@vben/icons';
import {$t} from '@vben/locales';

import {useVbenModal} from '@vben-core/popup-ui';

import {NButton, NPagination} from 'naive-ui';

import {useVbenForm, type VbenFormSchema} from '#/adapter/form';
import {dialog, message} from '#/adapter/naive';
import {agentApi} from '#/api/aigc/agent';
import LcActionCard from '#/components/LcActionCard/index.vue';
import LcIcon from '#/components/LcIcon/index.vue';
import LcListCard from '#/components/LcListCard/index.vue';
import ModelSelector from '#/components/ModelSelector/index.vue';
import {useAigcLookups} from '#/views/shared/aigc/lookups';
import {aigcCommonTagOptions} from '#/views/shared/aigc/options';
import {parseTagList, stringifyTagList} from '#/views/shared/aigc/tags';

import AgentCard from './card.vue';
import AgentEdit from './edit.vue';

const { loadLookups, lookups } = useAigcLookups({
  knowledges: true,
  models: true,
  skills: true,
});

const router = useRouter();

const loading = ref(false);
const saving = ref(false);
const creating = ref(false);
const showEdit = ref(false);
const keyword = ref('');
const selectedTag = ref('ALL');
const currentPage = ref(1);
const pageSize = ref(6);
const items = ref<AigcAgent[]>([]);
const currentItem = ref<null | Partial<AigcAgent>>(null);
const LcIconEditor = markRaw(LcIcon);
const ModelSelectorEditor = markRaw(ModelSelector);
const currentBaseConfigAgent = ref<AigcAgent | null>(null);
const tagFilterOptions = computed(() => [
  { label: $t('common.labels.all'), value: 'ALL' },
  ...aigcCommonTagOptions().map((item) => ({
    label: item.label,
    value: String(item.value),
  })),
]);

const filteredItems = computed(() => {
  const query = keyword.value.trim().toLowerCase();
  return items.value.filter((item) => {
    const matchKeyword =
      !query ||
      [item.agentName, item.status, item.description, item.tags]
        .map((value) => String(value ?? '').toLowerCase())
        .some((value) => value.includes(query));
    const tags = parseTagList(item.tags);
    const matchTag =
      selectedTag.value === 'ALL' || tags.includes(selectedTag.value);
    return matchKeyword && matchTag;
  });
});

const pagedItems = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value;
  return filteredItems.value.slice(start, start + pageSize.value);
});

watch([keyword, selectedTag], () => {
  currentPage.value = 1;
});
const actionItems = computed(() => [
  {
    key: 'refresh',
    label: $t('agents.actions.refreshList'),
    icon: RefreshCcw,
  },
  { key: 'create', label: $t('agents.actions.create'), icon: Plus },
]);
const [BaseConfigModal, baseConfigModalApi] = useVbenModal({
  onCancel() {
    baseConfigModalApi.close();
  },
});

watch(
  () => $t('agents.list.baseConfigTitle'),
  (value) => {
    baseConfigModalApi.setState({ title: value });
  },
  { immediate: true },
);

const baseConfigSchema = computed<VbenFormSchema[]>(() => [
  {
    component: 'Input',
    fieldName: 'agentName',
    label: $t('agents.form.appName'),
    rules: 'required',
  },
  {
    component: LcIconEditor,
    componentProps: {
      editable: true,
      fallbackIcon: 'lucide:bot',
      showSvgTab: false,
      showUrlTab: false,
      size: 64,
    },
    fieldName: 'icon',
    label: $t('agents.form.appIcon'),
    modelPropName: 'modelValue',
  },
  {
    component: 'Input',
    componentProps: {
      autosize: { minRows: 2, maxRows: 6 },
      placeholder: $t('agents.form.appDescriptionPlaceholder'),
      type: 'textarea',
    },
    fieldName: 'description',
    label: $t('agents.form.appDescription'),
  },
]);
const [BaseConfigForm, baseConfigFormApi] = useVbenForm({
  layout: 'vertical',
  schema: baseConfigSchema.value,
  showDefaultActions: false,
  wrapperClass: 'grid-cols-1',
});

watch(
  () => baseConfigSchema.value,
  (schema) => {
    baseConfigFormApi.setState({ schema });
  },
  { immediate: true },
);

// 新建智能体弹窗：仅包含模型、名称、描述、标签等基础配置，创建后跳转编辑页
const [CreateModal, createModalApi] = useVbenModal({
  onCancel() {
    createModalApi.close();
  },
});

watch(
  () => $t('agents.title.create'),
  (value) => {
    createModalApi.setState({ title: value });
  },
  { immediate: true },
);

const createModelConfig = reactive({
  maxOutputTokens: 2048,
  temperature: 0.7,
  topP: 0.9,
});
const createFormSchema = computed<VbenFormSchema[]>(() => [
  {
    component: 'Input',
    componentProps: {
      placeholder: $t('agents.form.appNamePlaceholder'),
    },
    fieldName: 'agentName',
    label: $t('agents.form.appName'),
    rules: 'required',
  },
  {
    component: ModelSelectorEditor,
    componentProps: {
      config: createModelConfig,
      'onUpdate:config': (value: Record<string, any>) =>
        Object.assign(createModelConfig, value),
      modelEntities: lookups.value.modelEntities,
    },
    fieldName: 'reasoningModelId',
    label: $t('agents.form.selectModel'),
    modelPropName: 'modelId',
    rules: 'selectRequired',
  },
  {
    component: 'Input',
    componentProps: {
      autosize: { minRows: 2, maxRows: 6 },
      placeholder: $t('agents.form.appDescriptionPlaceholder'),
      type: 'textarea',
    },
    fieldName: 'description',
    label: $t('agents.form.appDescription'),
  },
  {
    component: 'Select',
    componentProps: {
      clearable: true,
      multiple: true,
      options: aigcCommonTagOptions(),
      placeholder: $t('common.placeholder.selectTags'),
    },
    fieldName: 'tags',
    label: $t('common.labels.tags'),
  },
]);
const [CreateForm, createFormApi] = useVbenForm({
  layout: 'vertical',
  schema: createFormSchema.value,
  showDefaultActions: false,
  wrapperClass: 'grid-cols-1',
});

watch(
  () => createFormSchema.value,
  (schema) => {
    createFormApi.setState({ schema });
  },
  { immediate: true },
);

watch(
  () => $t('common.actions.create'),
  (value) => {
    createModalApi.setState({ confirmText: value });
  },
  { immediate: true },
);

async function loadList() {
  loading.value = true;
  try {
    items.value = await agentApi.list();
  } finally {
    loading.value = false;
  }
}

function openDetail(item?: AigcAgent | null) {
  if (item?.id) {
    void router.push(`/agents/${item.id}/builder`);
    return;
  }
  void router.push('/agents/new/builder');
}

function openCreateModal() {
  // 先打开弹窗再重置表单：表单挂在弹窗内，未挂载时调用 resetForm 会一直等待
  createModalApi.open();
  void createFormApi.resetForm();
}

async function handleCreateAgent() {
  const values = await createFormApi.validateAndSubmitForm();
  if (!values) {
    return;
  }
  const payload = values as Record<string, any>;
  const agentName = String(payload.agentName || '').trim();
  creating.value = true;
  try {
    await agentApi.create({
      agentName,
      description: String(payload.description || '').trim(),
      reasoningModelId: String(payload.reasoningModelId || ''),
      status: 'DRAFT',
      tags: stringifyTagList(payload.tags),
    });
    message.success($t('agents.messages.created'));
    createModalApi.close();
    // create 接口仅返回布尔值，按名称查出新记录后跳转编辑页
    const list = await agentApi.list();
    const created = list.find((item) => item.agentName === agentName);
    await (created?.id ? router.push(`/agents/${created.id}/builder`) : loadList());
  } finally {
    creating.value = false;
  }
}

function handleEdit(item: AigcAgent) {
  currentItem.value = item;
  showEdit.value = true;
}

function handleAction(action: { key: string }) {
  if (action.key === 'refresh') {
    void initializePage();
    return;
  }
  if (action.key === 'create') {
    void openCreateModal();
  }
}

async function handleEditBaseConfig(item: AigcAgent) {
  currentBaseConfigAgent.value = item;
  baseConfigModalApi.open();
  await baseConfigFormApi.resetForm();
  await baseConfigFormApi.setValues(
    {
      agentName: String(item.agentName || ''),
      description: String(item.description || ''),
      icon: String(item.icon || item.avatar || ''),
    },
    false,
  );
}

async function handleSaveBaseConfig() {
  const target = currentBaseConfigAgent.value;
  if (!target?.id) {
    return;
  }
  const values = await baseConfigFormApi.validateAndSubmitForm();
  if (!values) {
    return;
  }
  const payload = values as Record<string, any>;
  const normalizedIcon = String(payload.icon || '').trim();
  await agentApi.update(target.id, {
    agentName: String(payload.agentName || '').trim(),
    avatar: /^https?:\/\//.test(normalizedIcon) ? normalizedIcon : '',
    description: String(payload.description || '').trim(),
    icon: normalizedIcon,
  });
  message.success($t('agents.messages.baseConfigUpdated'));
  baseConfigModalApi.close();
  await loadList();
}

async function handleSave(payload: Partial<AigcAgent>) {
  saving.value = true;
  try {
    if (currentItem.value?.id) {
      await agentApi.update(currentItem.value.id, payload);
      message.success($t('agents.messages.updated'));
    } else {
      await agentApi.create(payload);
      message.success($t('agents.messages.created'));
    }
    showEdit.value = false;
    await loadList();
  } finally {
    saving.value = false;
  }
}

async function handleDelete(item: AigcAgent) {
  if (!item.id) {
    return;
  }
  dialog.warning({
    closable: false,
    content: $t('common.messages.deleteConfirmContent', {
      name: item.agentName || $t('agents.card.unnamed'),
    }),
    negativeText: $t('common.actions.cancel'),
    positiveText: $t('common.actions.confirmDelete'),
    title: $t('agents.messages.deleteTitle'),
    onPositiveClick: async () => {
      await agentApi.remove(item.id!);
      message.success($t('agents.messages.deleted'));
      await loadList();
    },
  });
}

async function initializePage() {
  await Promise.all([loadLookups(), loadList()]);
}

onMounted(initializePage);
</script>

<template>
  <Page>
    <div class="flex flex-col gap-2">
      <LcListCard
        :active-tag="selectedTag"
        :items="pagedItems"
        :loading="loading"
        :search-placeholder="$t('agents.list.searchPlaceholder')"
        :search-value="keyword"
        :tags="tagFilterOptions"
        @update:active-tag="selectedTag = $event"
        @update:search-value="keyword = $event"
      >
        <template #leading-card>
          <LcActionCard
            :actions="actionItems"
            :description="$t('common.messages.quickActionsDescription')"
            :title="$t('agents.quickActions.title')"
            @action="handleAction"
          />
        </template>
        <template #item="{ item }">
          <AgentCard
            :item="item"
            :knowledge-options="lookups.knowledges"
            :model-options="lookups.models"
            :skill-options="lookups.skills"
            @builder="openDetail"
            @delete="handleDelete"
            @edit="handleEdit"
            @base-config="handleEditBaseConfig"
          />
        </template>
        <template #empty-extra>
          <NButton type="primary" @click="openCreateModal">
            {{ $t('agents.actions.create') }}
          </NButton>
        </template>
      </LcListCard>

      <div v-if="filteredItems.length > pageSize" class="flex justify-end">
        <NPagination
          v-model:page="currentPage"
          v-model:page-size="pageSize"
          :item-count="filteredItems.length"
          :page-sizes="[6, 9, 12, 18]"
          show-size-picker
        />
      </div>

      <BaseConfigModal
        class="w-[560px]"
        header-class="border-b"
        :title="$t('agents.list.baseConfigTitle')"
      >
        <BaseConfigForm />

        <template #footer>
          <div class="flex w-full justify-end gap-2">
            <NButton @click="baseConfigModalApi.close()">
              {{ $t('common.actions.cancel') }}
            </NButton>
            <NButton type="primary" @click="handleSaveBaseConfig">
              {{ $t('common.actions.save') }}
            </NButton>
          </div>
        </template>
      </BaseConfigModal>

      <CreateModal
        class="w-[560px]"
        header-class="border-b"
        :title="$t('agents.title.create')"
      >
        <CreateForm />

        <template #footer>
          <div class="flex w-full justify-end gap-2">
            <NButton @click="createModalApi.close()">
              {{ $t('common.actions.cancel') }}
            </NButton>
            <NButton :loading="creating" type="primary" @click="handleCreateAgent">
              {{ $t('common.actions.create') }}
            </NButton>
          </div>
        </template>
      </CreateModal>

      <AgentEdit
        v-model:show="showEdit"
        :model-options="lookups.models"
        :model-value="currentItem"
        :saving="saving"
        @save="handleSave"
      />
    </div>
  </Page>
</template>
