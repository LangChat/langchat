<script lang="ts" setup>
import type {
  AigcDatasource,
  ColumnStructure,
  TableStructure,
} from '#/api/aigc/datasource';

import { computed, onMounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import { Page } from '@vben/common-ui';
import {
  ArrowLeft,
  Check,
  ChevronDown,
  ChevronRight,
  Database,
  Download,
  Save,
  Trash2,
} from '@vben/icons';
import { $t } from '@vben/locales';

import { NButton, NEmpty, NInput, NSpin, NSwitch, NTag } from 'naive-ui';

import { message } from '#/adapter/naive';
import type { VxeGridPropTypes } from '#/adapter/vxe-table';
import { useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  getDataSource,
  getDataSourceStructure,
  introspectDataSource,
  saveDataSourceStructure,
} from '#/api/aigc/datasource';

import { getDatasourceTypeMeta } from './datasource-meta';

interface ManagedColumn extends ColumnStructure {
  originalName?: string;
}

interface ManagedTable extends TableStructure {
  enabled?: boolean;
  imported?: boolean;
  sourceTableName?: string;
}

interface ColumnRow extends ManagedColumn {
  rowKey: string;
  tableName: string;
}

const route = useRoute();
const router = useRouter();
const datasourceId = computed(() => String(route.params.id || ''));

const loading = ref(false);
const savingStructure = ref(false);
const datasource = ref<AigcDatasource | null>(null);
const structure = ref<TableStructure[]>([]);
const customStructure = ref<ManagedTable[]>([]);
const selectedTable = ref('');
const selectedColumn = ref('');
const expandedTables = ref<Record<string, boolean>>({});
const activeTab = ref<'custom' | 'original'>('original');

const datasourceTypeMeta = computed(() =>
  getDatasourceTypeMeta(datasource.value?.dbType),
);
const importedTableMap = computed(() => {
  const map = new Map<string, ManagedTable>();
  for (const table of customStructure.value) {
    map.set(table.sourceTableName || table.tableName, table);
  }
  return map;
});
const enabledTableCount = computed(
  () => customStructure.value.filter((table) => table.enabled !== false).length,
);
const pendingImportCount = computed(
  () =>
    structure.value.filter(
      (table) => !importedTableMap.value.has(table.tableName),
    ).length,
);
const sourceTables = computed<TableStructure[]>(() =>
  activeTab.value === 'custom' ? customStructure.value : structure.value,
);
const selectedTableMeta = computed(() =>
  sourceTables.value.find((table) => table.tableName === selectedTable.value),
);
const selectedManagedTable = computed(() =>
  activeTab.value === 'custom' && selectedTable.value !== '__all__'
    ? customStructure.value.find(
        (table) => table.tableName === selectedTable.value,
      ) || null
    : null,
);
const selectedLabel = computed(() => {
  if (!selectedTable.value || selectedTable.value === '__all__') {
    return activeTab.value === 'custom'
      ? $t('datasource.detail.allImportedTables')
      : $t('datasource.detail.allOriginalTables');
  }
  return selectedColumn.value
    ? $t('datasource.detail.selectedLabelWithColumn', {
        column: selectedColumn.value,
        table: selectedTable.value,
      })
    : $t('datasource.detail.selectedLabelAllFields', {
        table: selectedTable.value,
      });
});
const currentRows = computed<ColumnRow[]>(() => {
  const tables =
    selectedTable.value === '__all__'
      ? sourceTables.value
      : sourceTables.value.filter(
          (table) => table.tableName === selectedTable.value,
        );
  const rows = tables.flatMap((table) =>
    table.columns.map(
      (column) =>
        Object.assign(column, {
          rowKey: `${table.tableName}.${column.name}`,
          tableName: table.tableName,
        }) as ColumnRow,
    ),
  );
  return selectedColumn.value
    ? rows.filter(
        (row) =>
          row.name === selectedColumn.value ||
          row.originalName === selectedColumn.value,
      )
    : rows;
});

const originalColumns = computed<VxeGridPropTypes.Columns<ColumnRow>>(() => [
  {
    field: 'tableName',
    title: $t('datasource.detail.columns.tableName'),
    minWidth: 180,
  },
  {
    field: 'name',
    title: $t('datasource.detail.columns.originalName'),
    minWidth: 180,
  },
  {
    field: 'type',
    title: $t('datasource.detail.columns.originalType'),
    width: 140,
  },
  {
    field: 'size',
    title: $t('datasource.detail.columns.length'),
    width: 90,
    formatter: ({ cellValue }) => cellValue ?? '--',
  },
  {
    field: 'primaryKey',
    title: $t('datasource.detail.columns.primaryKey'),
    width: 80,
    formatter: ({ cellValue }) =>
      cellValue ? $t('common.status.yes') : $t('common.status.no'),
  },
  {
    field: 'nullable',
    title: $t('datasource.detail.columns.nullable'),
    width: 80,
    formatter: ({ cellValue }) =>
      cellValue ? $t('common.status.yes') : $t('common.status.no'),
  },
  {
    field: 'comment',
    title: $t('datasource.detail.columns.comment'),
    minWidth: 220,
    formatter: ({ cellValue }) => cellValue || '--',
  },
]);
const customColumns = computed<VxeGridPropTypes.Columns<ColumnRow>>(() => [
  {
    field: 'tableName',
    title: $t('datasource.detail.columns.importedTable'),
    minWidth: 160,
  },
  {
    field: 'name',
    title: $t('datasource.detail.columns.aiName'),
    minWidth: 190,
    slots: { default: 'customName' },
  },
  {
    field: 'type',
    title: $t('datasource.detail.columns.aiType'),
    minWidth: 160,
    slots: { default: 'customType' },
  },
  {
    field: 'primaryKey',
    title: $t('datasource.detail.columns.primaryKey'),
    width: 80,
    formatter: ({ cellValue }) =>
      cellValue ? $t('common.status.yes') : $t('common.status.no'),
  },
  {
    field: 'comment',
    title: $t('datasource.detail.columns.aiComment'),
    minWidth: 300,
    slots: { default: 'customComment' },
  },
]);

const [Grid, gridApi] = useVbenVxeGrid<ColumnRow>({
  class: 'bg-transparent shadow-none',
  gridClass: 'px-0 pb-0',
  gridOptions: {
    border: false,
    columns: originalColumns.value,
    data: [],
    minHeight: 260,
    rowConfig: { keyField: 'rowKey' },
    showOverflow: true,
    stripe: true,
  },
});

const gridDataKey = computed(() =>
  sourceTables.value
    .map((table) => `${table.tableName}:${table.columns.length}`)
    .join('|'),
);

watch(
  [
    selectedTable,
    selectedColumn,
    activeTab,
    gridDataKey,
    originalColumns,
    customColumns,
  ],
  () => {
    gridApi.setGridOptions({
      columns:
        activeTab.value === 'custom'
          ? customColumns.value
          : originalColumns.value,
      data: currentRows.value,
    });
  },
  { immediate: true },
);

function normalizeCustomTables(tables: TableStructure[]): ManagedTable[] {
  return tables
    .filter((table) => (table as ManagedTable).imported === true)
    .map((table) => ({
      ...table,
      enabled: (table as ManagedTable).enabled !== false,
      imported: true,
      sourceTableName:
        (table as ManagedTable).sourceTableName || table.tableName,
      columns: table.columns.map((column) => ({
        ...column,
        originalName: (column as ManagedColumn).originalName || column.name,
      })),
    }));
}

function ensureSelection() {
  if (sourceTables.value.length === 0) {
    selectedTable.value = '';
    selectedColumn.value = '';
    return;
  }
  if (
    selectedTable.value !== '__all__' &&
    !sourceTables.value.some((table) => table.tableName === selectedTable.value)
  ) {
    selectedTable.value = sourceTables.value[0]?.tableName || '';
  }
  if (
    selectedColumn.value &&
    !selectedTableMeta.value?.columns.some(
      (column) =>
        column.name === selectedColumn.value ||
        (column as ManagedColumn).originalName === selectedColumn.value,
    )
  ) {
    selectedColumn.value = '';
  }
}

function selectTable(tableName: string, columnName = '') {
  selectedTable.value = tableName;
  selectedColumn.value = columnName;
}

function toggleTable(tableName: string) {
  expandedTables.value[tableName] = !expandedTables.value[tableName];
}

function expandAllTables() {
  for (const table of sourceTables.value) {
    expandedTables.value[table.tableName] = true;
  }
}

function switchTab(tab: 'custom' | 'original') {
  activeTab.value = tab;
  selectedColumn.value = '';
  ensureSelection();
}

function importTable(table: TableStructure) {
  if (importedTableMap.value.has(table.tableName)) {
    const imported = importedTableMap.value.get(table.tableName);
    if (imported) imported.enabled = true;
    switchTab('custom');
    selectTable(imported?.tableName || table.tableName);
    return;
  }
  const imported: ManagedTable = {
    ...JSON.parse(JSON.stringify(table)),
    enabled: true,
    imported: true,
    sourceTableName: table.tableName,
    columns: table.columns.map((column) => ({
      ...JSON.parse(JSON.stringify(column)),
      originalName: column.name,
    })),
  };
  customStructure.value.push(imported);
  switchTab('custom');
  selectTable(imported.tableName);
  message.success(
    $t('datasource.messages.tableImported', { table: table.tableName }),
  );
}

function removeImportedTable(table: TableStructure) {
  customStructure.value = customStructure.value.filter(
    (item) => item !== table,
  );
  ensureSelection();
  message.success(
    $t('datasource.messages.tableRemoved', { table: table.tableName }),
  );
}

function toggleSelectedTableEnabled(enabled: boolean) {
  if (selectedManagedTable.value) {
    selectedManagedTable.value.enabled = enabled;
  }
}

function removeSelectedTable() {
  if (selectedManagedTable.value) {
    removeImportedTable(selectedManagedTable.value);
  }
}

async function loadDatasource() {
  if (!datasourceId.value) return;
  loading.value = true;
  try {
    datasource.value = await getDataSource(datasourceId.value);
  } catch (error) {
    message.error(
      $t('datasource.messages.datasourceLoadFailed', {
        message: (error as Error)?.message || $t('errors.unknown'),
      }),
    );
    loading.value = false;
    return;
  }
  try {
    structure.value =
      (await introspectDataSource(datasourceId.value))?.tables || [];
  } catch (error) {
    message.error(
      $t('datasource.messages.structureLoadFailed', {
        message: (error as Error)?.message || $t('errors.unknown'),
      }),
    );
  }
  try {
    const saved = await getDataSourceStructure(datasourceId.value);
    if (saved?.structureJson) {
      const parsed = JSON.parse(saved.structureJson);
      customStructure.value = normalizeCustomTables(
        Array.isArray(parsed?.tables) ? parsed.tables : [],
      );
    }
  } catch (error) {
    message.error(
      $t('datasource.messages.customStructureLoadFailed', {
        message: (error as Error)?.message || $t('errors.unknown'),
      }),
    );
  } finally {
    ensureSelection();
    loading.value = false;
  }
}

async function handleSaveStructure() {
  if (!datasourceId.value) return;
  savingStructure.value = true;
  try {
    await saveDataSourceStructure(
      datasourceId.value,
      JSON.stringify({ tables: customStructure.value }),
    );
    message.success($t('datasource.messages.structureSaved'));
  } catch (error) {
    message.error(
      $t('common.messages.saveFailed', {
        message: (error as Error)?.message || $t('errors.unknown'),
      }),
    );
  } finally {
    savingStructure.value = false;
  }
}

function goBack() {
  void router.push('/datasources');
}

onMounted(loadDatasource);
</script>

<template>
  <Page>
    <div class="flex h-full min-h-0 flex-col gap-2">
      <div
        class="flex flex-col gap-3 rounded-xl border border-border bg-card p-4 lg:flex-row lg:items-center lg:justify-between"
      >
        <div class="flex items-center gap-3">
          <NButton
            circle
            quaternary
            size="small"
            :aria-label="$t('common.actions.back')"
            @click="goBack"
          >
            <template #icon><ArrowLeft class="size-4" /></template>
          </NButton>
          <div
            class="flex size-10 items-center justify-center rounded-lg border border-border bg-white p-1.5"
          >
            <img
              :alt="`${datasourceTypeMeta.label} logo`"
              class="size-full object-contain"
              :src="datasourceTypeMeta.icon"
            />
          </div>
          <div class="min-w-0">
            <div class="flex items-center gap-2">
              <div class="truncate text-lg font-semibold text-foreground">
                {{ datasource?.name || $t('datasource.detail.fallbackTitle') }}
              </div>
              <NTag
                :bordered="false"
                :type="datasource?.enabled ? 'success' : 'default'"
                round
                >{{
                  datasource?.enabled
                    ? $t('common.status.enabled')
                    : $t('common.status.disabled')
                }}</NTag
              >
            </div>
            <div class="mt-0.5 text-[11px] text-muted-foreground">
              {{ datasourceTypeMeta.label }} · {{ datasource?.host || '--' }} ·
              {{ datasource?.databaseName || '--' }}
            </div>
          </div>
        </div>
      </div>

      <div
        class="flex min-h-0 flex-1 flex-col overflow-hidden rounded-xl border border-border bg-card p-4"
      >
        <div class="mb-4 grid gap-2 border-b border-border pb-3 md:grid-cols-2">
          <button
            class="rounded-lg border p-3 text-left transition-colors"
            :class="
              activeTab === 'original'
                ? 'border-primary/40 bg-primary/5'
                : 'border-transparent bg-muted/30 hover:bg-muted/60'
            "
            type="button"
            @click="switchTab('original')"
          >
            <div class="flex items-center justify-between gap-2">
              <span class="text-sm font-semibold">{{
                $t('datasource.detail.discoveryTitle')
              }}</span
              ><span class="flex items-center gap-1.5"
                ><NTag size="small">{{
                  $t('datasource.detail.tablesCount', {
                    count: structure.length,
                  })
                }}</NTag
                ><NTag
                  v-if="pendingImportCount > 0"
                  size="small"
                  type="warning"
                  >{{
                    $t('datasource.detail.pendingImport', {
                      count: pendingImportCount,
                    })
                  }}</NTag
                ></span
              >
            </div>
            <div class="mt-1 text-[11px] text-muted-foreground">
              {{ $t('datasource.detail.discoveryHint') }}
            </div>
          </button>
          <button
            class="rounded-lg border p-3 text-left transition-colors"
            :class="
              activeTab === 'custom'
                ? 'border-emerald-500/50 bg-emerald-500/5'
                : 'border-transparent bg-muted/30 hover:bg-muted/60'
            "
            type="button"
            @click="switchTab('custom')"
          >
            <div class="flex items-center justify-between gap-2">
              <span class="text-sm font-semibold">{{
                $t('datasource.detail.aiConfigTitle')
              }}</span
              ><span
                class="inline-flex items-center gap-1 text-xs font-semibold text-emerald-600"
                ><Check class="size-3.5" />{{
                  $t('datasource.detail.retrievalCount', {
                    count: enabledTableCount,
                  })
                }}</span
              >
            </div>
            <div class="mt-1 text-[11px] text-muted-foreground">
              {{ $t('datasource.detail.aiConfigHint') }}
            </div>
          </button>
        </div>

        <div
          v-if="activeTab === 'custom'"
          class="mb-3 flex flex-wrap items-center justify-between gap-2 rounded-lg border border-emerald-200 bg-emerald-50 px-3 py-2 text-xs text-emerald-800 dark:border-emerald-900 dark:bg-emerald-950/30 dark:text-emerald-300"
        >
          <span>{{ $t('datasource.detail.legend') }}</span>
          <NButton
            :loading="savingStructure"
            size="small"
            type="primary"
            @click="handleSaveStructure"
            ><template #icon><Save class="size-3.5" /></template
            >{{ $t('datasource.detail.saveConfig') }}</NButton
          >
        </div>

        <NSpin
          :show="loading"
          class="min-h-0 flex-1"
          content-class="h-full overflow-y-auto"
        >
          <div
            v-if="sourceTables.length > 0"
            class="grid min-h-[420px] gap-4 lg:grid-cols-[280px_minmax(0,1fr)]"
          >
            <aside class="rounded-lg border border-border/80 bg-muted/15 p-2">
              <div class="mb-2 flex items-center justify-between px-2 py-1">
                <span class="text-xs font-semibold text-foreground">{{
                  activeTab === 'custom'
                    ? $t('datasource.detail.importedTables')
                    : $t('datasource.detail.originalTables')
                }}</span
                ><NButton quaternary size="tiny" @click="expandAllTables">{{
                  $t('common.actions.expandAll')
                }}</NButton>
              </div>
              <button
                class="mb-1 flex w-full items-center gap-2 rounded-md px-2.5 py-2 text-left text-xs transition-colors"
                :class="
                  selectedTable === '__all__'
                    ? 'bg-primary/10 font-semibold text-primary'
                    : 'text-muted-foreground hover:bg-muted/60'
                "
                type="button"
                @click="selectTable('__all__')"
              >
                <Database class="size-3.5" /><span>{{
                  activeTab === 'custom'
                    ? $t('datasource.detail.allImportedTables')
                    : $t('datasource.detail.allOriginalTables')
                }}</span
                ><span class="ml-auto text-[10px] opacity-60">{{
                  sourceTables.length
                }}</span>
              </button>
              <div
                v-for="table in sourceTables"
                :key="table.tableName"
                class="mb-0.5"
              >
                <div
                  class="grid grid-cols-[28px_minmax(0,1fr)_72px_28px] items-center rounded-md pr-3"
                  :class="
                    selectedTable === table.tableName && !selectedColumn
                      ? 'bg-primary/10 text-primary'
                      : 'text-foreground hover:bg-muted/60'
                  "
                >
                  <button
                    class="flex size-7 shrink-0 items-center justify-center text-muted-foreground"
                    type="button"
                    :aria-label="
                      expandedTables[table.tableName]
                        ? $t('common.actions.collapse')
                        : $t('common.actions.expand')
                    "
                    @click="toggleTable(table.tableName)"
                  >
                    <ChevronDown
                      v-if="expandedTables[table.tableName]"
                      class="size-3.5"
                    /><ChevronRight v-else class="size-3.5" />
                  </button>
                  <button
                    class="min-w-0 flex-1 truncate py-2 text-left text-xs font-medium"
                    type="button"
                    @click="selectTable(table.tableName)"
                  >
                    {{ table.tableName }}
                  </button>
                  <div class="flex w-[72px] justify-end">
                    <NTag
                      v-if="
                        activeTab === 'original' &&
                        importedTableMap.has(table.tableName)
                      "
                      :bordered="false"
                      size="small"
                      type="success"
                      >{{ $t('datasource.detail.imported') }}</NTag
                    >
                    <NButton
                      v-if="
                        activeTab === 'original' &&
                        !importedTableMap.has(table.tableName)
                      "
                      class="w-[62px]"
                      secondary
                      size="tiny"
                      @click="importTable(table)"
                      ><template #icon><Download class="size-3" /></template
                      >{{ $t('datasource.detail.import') }}</NButton
                    >
                    <NTag
                      v-if="activeTab === 'custom'"
                      :bordered="false"
                      size="small"
                      :type="table.enabled === false ? 'default' : 'success'"
                      >{{
                        table.enabled === false
                          ? $t('datasource.detail.retrievalDisabled')
                          : $t('datasource.detail.retrievalEnabled')
                      }}</NTag
                    >
                  </div>
                  <span class="text-right text-[10px] text-muted-foreground">{{
                    table.columns.length
                  }}</span>
                </div>
                <div
                  v-if="expandedTables[table.tableName]"
                  class="ml-7 border-l border-border/70 pl-2"
                >
                  <button
                    class="flex w-full items-center rounded-md px-2 py-1.5 text-left text-[11px] text-muted-foreground hover:bg-muted/60"
                    :class="
                      selectedTable === table.tableName && !selectedColumn
                        ? 'text-primary'
                        : ''
                    "
                    type="button"
                    @click="selectTable(table.tableName)"
                  >
                    {{ $t('datasource.detail.allFields') }}</button
                  ><button
                    v-for="column in table.columns"
                    :key="column.name"
                    class="flex w-full items-center rounded-md px-2 py-1.5 text-left text-[11px] text-muted-foreground hover:bg-muted/60"
                    :class="
                      selectedTable === table.tableName &&
                      (selectedColumn === column.name ||
                        selectedColumn === column.originalName)
                        ? 'bg-primary/10 text-primary'
                        : ''
                    "
                    type="button"
                    @click="selectTable(table.tableName, column.name)"
                  >
                    <span class="truncate">{{ column.name }}</span
                    ><span class="ml-auto pl-2 text-[10px] opacity-60">{{
                      column.type
                    }}</span>
                  </button>
                </div>
              </div>
            </aside>
            <section class="min-w-0">
              <div class="mb-3 flex items-center justify-between gap-3">
                <div>
                  <div class="text-sm font-semibold text-foreground">
                    {{ selectedLabel }}
                  </div>
                  <div class="mt-0.5 text-[11px] text-muted-foreground">
                    {{
                      $t('datasource.detail.fieldsCount', {
                        count: currentRows.length,
                      })
                    }}
                  </div>
                </div>
                <div class="flex items-center gap-3">
                  <NTag
                    v-if="activeTab === 'custom'"
                    :bordered="false"
                    type="info"
                    >{{ $t('datasource.detail.fieldsEditable') }}</NTag
                  >
                  <div
                    v-if="selectedManagedTable"
                    class="flex items-center gap-2 rounded-md border border-border bg-muted/20 px-2.5 py-1.5"
                  >
                    <span class="text-xs text-muted-foreground">{{
                      $t('datasource.detail.allowAiRetrieval')
                    }}</span
                    ><NSwitch
                      :value="selectedManagedTable.enabled !== false"
                      size="small"
                      @update:value="toggleSelectedTableEnabled"
                    /><NButton
                      v-tippy="$t('datasource.detail.removeImportedTable')"
                      circle
                      quaternary
                      size="tiny"
                      type="error"
                      :aria-label="$t('datasource.detail.removeImportedTable')"
                      @click="removeSelectedTable"
                      ><Trash2 class="size-3.5"
                    /></NButton>
                  </div>
                </div>
              </div>
              <Grid
                ><template #customName="{ row }"
                  ><NInput
                    v-model:value="row.name"
                    size="small"
                    :placeholder="
                      $t('datasource.detail.aiFieldNamePlaceholder')
                    " /></template
                ><template #customType="{ row }"
                  ><NInput
                    v-model:value="row.type"
                    size="small"
                    :placeholder="
                      $t('datasource.detail.aiFieldTypePlaceholder')
                    " /></template
                ><template #customComment="{ row }"
                  ><NInput
                    v-model:value="row.comment"
                    size="small"
                    :placeholder="
                      $t('datasource.detail.aiFieldCommentPlaceholder')
                    " /></template
              ></Grid>
            </section>
          </div>
          <NEmpty
            v-else
            :description="
              activeTab === 'custom'
                ? $t('datasource.detail.noImportedTables')
                : $t('datasource.detail.noIntrospectedTables')
            "
          />
        </NSpin>
      </div>
    </div>
  </Page>
</template>
