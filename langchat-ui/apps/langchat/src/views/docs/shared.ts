import type { RouteLocationRaw } from 'vue-router';

export const DOC_EMBED_STATUS = {
  COMPLETED: 'completed',
  FAILED: 'failed',
  PENDING: 'pending',
  RUNNING: 'running',
} as const;

export interface KnowledgePreviewRouteParams {
  docsId?: string;
  knowledgeId?: string;
}

export function normalizeRouteParam(value: unknown) {
  if (Array.isArray(value)) {
    return typeof value[0] === 'string' ? value[0] : '';
  }
  return typeof value === 'string' ? value : '';
}

export function buildKnowledgeDocsRouteLocation(
  knowledgeId?: string,
): RouteLocationRaw {
  return {
    path: '/knowledge/docs',
    query: knowledgeId ? { knowledgeId } : {},
  };
}

export function buildKnowledgeUploadRouteLocation(
  knowledgeId?: string,
): RouteLocationRaw {
  return {
    path: '/knowledge/docs/upload',
    query: knowledgeId ? { knowledgeId } : {},
  };
}

export function buildKnowledgePreviewRouteLocation(
  params: KnowledgePreviewRouteParams,
): RouteLocationRaw {
  const { docsId, knowledgeId } = params;
  return {
    path: docsId ? `/knowledge/docs/${docsId}/preview` : '/knowledge/docs',
    query: knowledgeId ? { knowledgeId } : {},
  };
}

export function extractDocsFileExtension(filename = '') {
  const index = filename.lastIndexOf('.');
  if (index < 0 || index === filename.length - 1) {
    return '';
  }
  return filename.slice(index + 1).toLowerCase();
}

export function removeDocsFileExtension(filename = '') {
  const index = filename.lastIndexOf('.');
  return index > 0 ? filename.slice(0, index) : filename;
}

export function resolveDocsTypeByFilename(filename = '') {
  const ext = extractDocsFileExtension(filename);
  switch (ext) {
    case 'markdown':
    case 'md': {
      return 'MARKDOWN';
    }
    case 'text':
    case 'txt': {
      return 'TEXT';
    }
    default: {
      return 'FILE';
    }
  }
}

export function buildDocsIngestionConfig(
  chunkSize?: number | null,
  overlapSize?: number | null,
) {
  return JSON.stringify({
    chunkSize: chunkSize ?? undefined,
    overlapSize: overlapSize ?? undefined,
  });
}

export function parseDocsIngestionConfig(ingestionConfig?: null | string) {
  if (!ingestionConfig) {
    return {
      chunkSize: undefined,
      overlapSize: undefined,
    };
  }

  try {
    const parsed = JSON.parse(ingestionConfig) as Record<string, unknown>;
    return {
      chunkSize: toOptionalNumber(parsed.chunkSize ?? parsed.chunk_size),
      overlapSize: toOptionalNumber(parsed.overlapSize ?? parsed.overlap_size),
    };
  } catch {
    return {
      chunkSize: undefined,
      overlapSize: undefined,
    };
  }
}

export function formatDocsFileSize(size?: null | number) {
  if (size === null || size === undefined || Number.isNaN(size)) {
    return '--';
  }
  if (size < 1024) {
    return `${size} B`;
  }
  if (size < 1024 * 1024) {
    return `${(size / 1024).toFixed(1)} KB`;
  }
  if (size < 1024 * 1024 * 1024) {
    return `${(size / 1024 / 1024).toFixed(1)} MB`;
  }
  return `${(size / 1024 / 1024 / 1024).toFixed(1)} GB`;
}

function toOptionalNumber(value: unknown) {
  if (typeof value === 'number' && Number.isFinite(value)) {
    return value;
  }
  if (typeof value === 'string' && value.trim()) {
    const parsed = Number(value);
    return Number.isFinite(parsed) ? parsed : undefined;
  }
  return undefined;
}
