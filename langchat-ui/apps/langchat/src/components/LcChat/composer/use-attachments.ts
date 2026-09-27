import type { AttachmentItem } from './types';

import { ref } from 'vue';

import { $t } from '@vben/locales';

import { uploadChatAttachmentApi } from '#/api/aigc/oss';

let seed = 0;

/** 格式化文件大小展示。 */
export function formatFileSize(size: number): string {
  if (size < 1024) {
    return `${size} B`;
  }
  if (size < 1024 * 1024) {
    return `${(size / 1024).toFixed(1)} KB`;
  }
  return `${(size / 1024 / 1024).toFixed(1)} MB`;
}

/**
 * 附件 chips 状态:添加 / 移除 / 清空。
 * 图片自动生成 ObjectURL 预览,并在移除或清空时释放,避免内存泄漏。
 */
export function useAttachments() {
  const items = ref<AttachmentItem[]>([]);

  async function upload(item: AttachmentItem) {
    try {
      const result = await uploadChatAttachmentApi(item.file, (progress) => {
        item.progress = progress;
      });
      item.ossId = String(result.id || '');
      item.remoteUrl = String(result.url || '');
      item.progress = 100;
      item.status = 'uploaded';
    } catch (error) {
      item.error = error instanceof Error ? error.message : $t('chat.composer.uploadFailed');
      item.status = 'error';
    }
  }

  function addFiles(files: File[] | FileList | null) {
    const list = files ? [...files] : [];
    list.forEach((file) => {
      seed += 1;
      const isImage = Boolean(file.type && file.type.startsWith('image/'));
      const item: AttachmentItem = {
        file,
        id: `att-${Date.now()}-${seed}`,
        isImage,
        name: file.name || $t('chat.composer.unnamedFile'),
        progress: 0,
        size: file.size || 0,
        status: 'uploading',
        url: isImage ? URL.createObjectURL(file) : undefined,
      };
      items.value.push(item);
      void upload(item);
    });
  }

  function remove(id: string) {
    const index = items.value.findIndex((item) => item.id === id);
    if (index === -1) {
      return;
    }
    const [removed] = items.value.splice(index, 1);
    if (removed?.url) {
      URL.revokeObjectURL(removed.url);
    }
  }

  function clear() {
    items.value.forEach((item) => {
      if (item.url) {
        URL.revokeObjectURL(item.url);
      }
    });
    items.value = [];
  }

  return { addFiles, clear, items, remove };
}
