import type { AttachmentItem } from './types';

import { ref } from 'vue';

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

  function addFiles(files: File[] | FileList | null) {
    const list = files ? [...files] : [];
    list.forEach((file) => {
      seed += 1;
      const isImage = Boolean(file.type && file.type.startsWith('image/'));
      items.value.push({
        file,
        id: `att-${Date.now()}-${seed}`,
        isImage,
        name: file.name || '未命名文件',
        size: file.size || 0,
        url: isImage ? URL.createObjectURL(file) : undefined,
      });
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
