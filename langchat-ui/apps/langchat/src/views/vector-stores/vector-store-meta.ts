import { $t } from '@vben/locales';

import milvusSvg from '#/assets/vector-store-icons/milvus.svg';
import pgvectorSvg from '#/assets/vector-store-icons/pgvector.svg';

/** 与后端 VectorStoreSupport.DEFAULT_TABLE 保持一致，PGVector 未填表名时兜底使用。 */
const PGVECTOR_DEFAULT_TABLE = 'embedding_store';

export interface VectorStoreProviderMeta {
  /** 默认端口，切换供应商时端口仍为上一个默认值则跟随更新。 */
  defaultPort: number;
  description: string;
  icon: string;
  label: string;
  /** 集合型存储必须指定集合名，PGVector 的表名可留空由后端兜底。 */
  requireTableName: boolean;
  tableLabel: string;
  tablePlaceholder: string;
  value: string;
}

/** 至少包含一个供应商，保证取首项作为兜底时不会是空。 */
type VectorStoreProviderList = [
  VectorStoreProviderMeta,
  ...VectorStoreProviderMeta[],
];

/** 向量库供应商标识为产品名，保持原文即可。 */
export function vectorStoreProviderOptions(): VectorStoreProviderList {
  return [
    {
      defaultPort: 5432,
      description: $t('vectorStores.meta.pgvector.description'),
      icon: pgvectorSvg,
      label: 'PGVector',
      requireTableName: false,
      tableLabel: $t('vectorStores.meta.pgvector.tableLabel'),
      tablePlaceholder: $t('vectorStores.meta.pgvector.tablePlaceholder', {
        table: PGVECTOR_DEFAULT_TABLE,
      }),
      value: 'PGVECTOR',
    },
    {
      defaultPort: 19_530,
      description: $t('vectorStores.meta.milvus.description'),
      icon: milvusSvg,
      label: 'Milvus',
      requireTableName: true,
      tableLabel: $t('vectorStores.meta.milvus.tableLabel'),
      tablePlaceholder: $t('vectorStores.meta.milvus.tablePlaceholder'),
      value: 'MILVUS',
    },
  ];
}

export function getVectorStoreProviderMeta(
  provider?: string,
): VectorStoreProviderMeta {
  const options = vectorStoreProviderOptions();
  return options.find((item) => item.value === provider) ?? options[0];
}
