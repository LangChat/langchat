import type { BaseEntity } from './_shared';

import { requestClient } from '#/api/request';

export interface AigcOss extends BaseEntity {
  contentType?: string;
  ext?: string;
  filename?: string;
  originalFilename?: string;
  path?: string;
  platform?: string;
  size?: number;
  url?: string;
}

export function uploadChatAttachmentApi(
  file: File,
  onProgress?: (progress: number) => void,
) {
  return requestClient.upload<AigcOss>(
    '/v1/aigc/oss/upload',
    { file },
    {
      onUploadProgress: (event) => {
        const total = event.total || (file.size > 0 ? file.size : 1);
        onProgress?.(Math.min(100, Math.round((event.loaded / total) * 100)));
      },
    },
  );
}
