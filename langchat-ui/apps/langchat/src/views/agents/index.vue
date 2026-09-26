<script lang="ts" setup>
import type {AigcAgent} from '#/api/aigc/agent';

import {computed, markRaw, onMounted, reactive, ref, watch} from 'vue';
import {useRouter} from 'vue-router';

import {Page} from '@vben/common-ui';
import {Plus, RefreshCcw} from '@vben/icons';

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
import {AIGC_COMMON_TAG_OPTIONS} from '#/views/shared/aigc/options';
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
  { label: '全部', value: 'ALL' },
  ...AIGC_COMMON_TAG_OPTIONS.map((item) => ({
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
  { key: 'refresh', label: '刷新列表', icon: RefreshCcw },
  { key: 'create', label: '新建智能体', icon: Plus },
]);
const [BaseConfigModal, baseConfigModalApi] = useVbenModal({
  onCancel() {
    baseConfigModalApi.close();
  },
});
const baseConfigSchema: VbenFormSchema[] = [
  {
    component: 'Input',
    fieldName: 'agentName',
    label: '应用名称',
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
    label: '应用图标',
    modelPropName: 'modelValue',
  },
  {
    component: 'Input',
    componentProps: {
      autosize: { minRows: 2, maxRows: 6 },
      placeholder: '请输入应用描述',
      type: 'textarea',
    },
    fieldName: 'description',
    label: '应用描述',
  },
];
const [BaseConfigForm, baseConfigFormApi] = useVbenForm({
  layout: 'vertical',
  schema: baseConfigSchema,
  showDefaultActions: false,
  wrapperClass: 'grid-cols-1',
});

// 新建智能体弹窗：仅包含模型、名称、描述、标签等基础配置，创建后跳转编辑页
const [CreateModal, createModalApi] = useVbenModal({
  onCancel() {
    createModalApi.close();
  },
});
const createModelConfig = reactive({
  maxOutputTokens: 2048,
  temperature: 0.7,
  topP: 0.9,
});
const createFormSchema = computed<VbenFormSchema[]>(() => [
  {
    component: 'Input',
    componentProps: {
      placeholder: '请输入应用名称',
    },
    fieldName: 'agentName',
    label: '应用名称',
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
    label: '选择模型',
    modelPropName: 'modelId',
    rules: 'selectRequired',
  },
  {
    component: 'Input',
    componentProps: {
      autosize: { minRows: 2, maxRows: 6 },
      placeholder: '请输入应用描述',
      type: 'textarea',
    },
    fieldName: 'description',
    label: '应用描述',
  },
  {
    component: 'Select',
    componentProps: {
      clearable: true,
      multiple: true,
      options: AIGC_COMMON_TAG_OPTIONS,
      placeholder: '请选择标签',
    },
    fieldName: 'tags',
    label: '标签',
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
    message.success('智能体已创建');
    createModalApi.close();
    // create 接口仅返回布尔值，按名称查出新记录后跳转编辑页
    const list = await agentApi.list();
    const created = list.find((item) => item.agentName === agentName);
    if (created?.id) {
      await router.push(`/agents/${created.id}/builder`);
    } else {
      await loadList();
    }
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
  message.success('基础配置已更新');
  baseConfigModalApi.close();
  await loadList();
}

async function handleSave(payload: Partial<AigcAgent>) {
  saving.value = true;
  try {
    if (currentItem.value?.id) {
      await agentApi.update(currentItem.value.id, payload);
      message.success('智能体已更新');
    } else {
      await agentApi.create(payload);
      message.success('智能体已创建');
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
    content: `删除后不可恢复，确认删除「${item.agentName || '未命名智能体'}」吗？`,
    negativeText: '取消',
    positiveText: '确认删除',
    title: '删除智能体',
    onPositiveClick: async () => {
      await agentApi.remove(item.id!);
      message.success('智能体已删除');
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
        search-placeholder="按智能体名称、状态搜索"
        :search-value="keyword"
        :tags="tagFilterOptions"
        @update:active-tag="selectedTag = $event"
        @update:search-value="keyword = $event"
      >
        <template #leading-card>
          <LcActionCard
            :actions="actionItems"
            description="常用操作统一放置在首个卡片位。"
            title="智能体操作"
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
          <NButton type="primary" @click="openCreateModal">新建智能体</NButton>
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
        title="应用基础配置"
      >
        <BaseConfigForm />

        <template #footer>
          <div class="flex w-full justify-end gap-2">
            <NButton @click="baseConfigModalApi.close()">取消</NButton>
            <NButton type="primary" @click="handleSaveBaseConfig">保存</NButton>
          </div>
        </template>
      </BaseConfigModal>

      <CreateModal
        class="w-[560px]"
        header-class="border-b"
        title="新建智能体"
      >
        <CreateForm />

        <template #footer>
          <div class="flex w-full justify-end gap-2">
            <NButton @click="createModalApi.close()">取消</NButton>
            <NButton :loading="creating" type="primary" @click="handleCreateAgent">
              创建
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
