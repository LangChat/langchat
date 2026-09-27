import type { BaseEntity } from './_shared';

import { requestClient } from '#/api/request';

/**
 * 文档分段实体。
 */
export interface AigcSegment extends BaseEntity {
  content?: string;
  docsId?: string;
  enabled?: boolean;
  indexHash?: string;
  knowledgeId?: string;
  name?: string;
  position?: number;
  status?: number;
}

/**
 * 分段内容更新请求。
 */
export interface SegmentContentUpdateRequest {
  content?: string;
}

/**
 * 分段启停请求。
 */
export interface SegmentEnabledRequest {
  enabled?: boolean;
}

/**
 * 分段批量删除请求。
 */
export interface SegmentDeleteRequest {
  segmentIds?: string[];
}

/**
 * 查询知识库下指定文档的分段列表。
 */
export function listKnowledgeSegmentsApi(
  knowledgeId: string,
  docsId: string,
) {
  return requestClient.get<AigcSegment[]>(
    `/v1/core/knowledges/${knowledgeId}/segments`,
    { params: { docsId } },
  );
}

/**
 * 更新分段内容。
 */
export function updateSegmentContentApi(
  knowledgeId: string,
  segmentId: string,
  payload: SegmentContentUpdateRequest,
) {
  return requestClient.put<boolean>(
    `/v1/core/knowledges/${knowledgeId}/segments/${segmentId}`,
    payload,
  );
}

/**
 * 启停分段。
 */
export function updateSegmentEnabledApi(
  knowledgeId: string,
  segmentId: string,
  payload: SegmentEnabledRequest,
) {
  return requestClient.put<boolean>(
    `/v1/core/knowledges/${knowledgeId}/segments/${segmentId}/enabled`,
    payload,
  );
}

/**
 * 重新向量化单个分段。
 */
export function reindexSegmentApi(knowledgeId: string, segmentId: string) {
  return requestClient.post<boolean>(
    `/v1/core/knowledges/${knowledgeId}/segments/${segmentId}/reindex`,
  );
}

/**
 * 删除单个分段。
 */
export function deleteSegmentApi(knowledgeId: string, segmentId: string) {
  return requestClient.delete<boolean>(
    `/v1/core/knowledges/${knowledgeId}/segments/${segmentId}`,
  );
}

/**
 * 批量删除分段。
 */
export function deleteSegmentsApi(
  knowledgeId: string,
  payload: SegmentDeleteRequest,
) {
  return requestClient.delete<boolean>(
    `/v1/core/knowledges/${knowledgeId}/segments`,
    { data: payload },
  );
}
