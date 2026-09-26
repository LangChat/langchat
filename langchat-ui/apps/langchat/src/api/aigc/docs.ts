import type { BaseEntity } from './_shared';

import { requestClient } from '#/api/request';

import { createCrudApi } from './_shared';

/**
 * 文档实体。
 */
export interface AigcDocs extends BaseEntity {
  content?: string;
  embedEndTime?: number;
  embedError?: string;
  embedStartTime?: number;
  embedStatus?: string;
  enabled?: boolean;
  ext?: string;
  indexingStatus?: number;
  ingestionConfig?: string;
  knowledgeId?: string;
  name?: string;
  ossId?: string;
  parentId?: string;
  pdfUrl?: string;
  size?: number;
  type?: string;
  url?: string;
}

/**
 * 单文档索引状态。
 */
export interface KnowledgeDocumentIndexStatus {
  costMs?: number | null;
  docsId?: string;
  embedEndTime?: number | null;
  embedError?: string | null;
  embedStartTime?: number | null;
  embedStatus?: string;
  ext?: string;
  indexingStatus?: number | null;
  name?: string;
  updateTime?: number | null;
}

/**
 * 知识库索引状态结果。
 */
export interface KnowledgeIndexStatusResult {
  completedCount: number;
  docs: KnowledgeDocumentIndexStatus[];
  docsCount: number;
  failedCount: number;
  finished: boolean;
  knowledgeId: string;
  pendingCount: number;
  progressPercent: number;
  runningCount: number;
}

/**
 * 知识库索引请求。
 */
export interface KnowledgeIndexRequest {
  chunkSize?: number;
  docsIds?: string[];
  overlapSize?: number;
}

/**
 * 知识库解析预览请求。
 */
export interface KnowledgeParsePreviewRequest extends KnowledgeIndexRequest {
  chunkLimit?: number;
  sectionLimit?: number;
}

/**
 * 文档解析预览结果。
 */
export interface KnowledgeDocumentPreview {
  chunkCount?: number;
  chunks?: string[];
  contentLength?: number;
  docsId?: string;
  parserName?: string;
  sectionCount?: number;
  sections?: string[];
  title?: string;
}

/**
 * 知识库解析预览响应。
 */
export interface KnowledgeParsePreviewResult {
  docs: KnowledgeDocumentPreview[];
  docsCount: number;
  knowledgeId: string;
}

/**
 * 知识库索引提交结果。
 */
export interface KnowledgeIndexResult {
  async?: boolean;
  docsCount?: number;
  embeddedCount?: number;
  knowledgeId?: string;
  segmentCount?: number;
  submittedCount?: number;
  taskStatus?: string;
}

function buildDocsIdsQuery(docsIds?: string[]) {
  if (!docsIds || docsIds.length === 0) {
    return '';
  }
  const query = new URLSearchParams();
  docsIds.forEach((docsId) => query.append('docsIds', docsId));
  return `?${query.toString()}`;
}

export const docsApi = createCrudApi<AigcDocs>('/v1/aigc/docs');

/**
 * 上传知识库文档。
 */
export async function uploadDocsApi(formData: FormData) {
  return requestClient.post<AigcDocs>('/v1/aigc/docs/upload', formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  });
}

/**
 * 提交知识库索引任务。
 */
export async function indexKnowledgeApi(
  knowledgeId: string,
  payload: KnowledgeIndexRequest,
) {
  return requestClient.post<KnowledgeIndexResult>(
    `/v1/core/knowledges/${knowledgeId}/index`,
    payload,
  );
}

/**
 * 预览知识库文档解析结果。
 */
export async function previewKnowledgeApi(
  knowledgeId: string,
  payload: KnowledgeParsePreviewRequest,
) {
  return requestClient.post<KnowledgeParsePreviewResult>(
    `/v1/core/knowledges/${knowledgeId}/parse-preview`,
    payload,
  );
}

/**
 * 查询知识库文档向量化状态。
 */
export async function getKnowledgeIndexStatusApi(
  knowledgeId: string,
  docsIds?: string[],
) {
  return requestClient.get<KnowledgeIndexStatusResult>(
    `/v1/core/knowledges/${knowledgeId}/index-status${buildDocsIdsQuery(docsIds)}`,
  );
}
