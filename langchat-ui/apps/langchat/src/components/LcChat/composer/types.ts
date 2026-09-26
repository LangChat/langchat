/**
 * LcChatComposer 公共类型定义。
 */

/** 附件条目(组件内本地暂存,发送时整体交给调用方处理上传)。 */
export interface AttachmentItem {
  /** 上传失败原因 */
  error?: string;

  /** 原始文件对象 */
  file: File;

  /** 唯一标识 */
  id: string;

  /** 是否图片(决定缩略图展示) */
  isImage: boolean;

  /** OSS 资源 ID */
  ossId?: string;

  /** 上传进度 0-100 */
  progress: number;

  /** 上传完成后的资源地址 */
  remoteUrl?: string;

  /** 上传状态 */
  status: 'error' | 'uploaded' | 'uploading';

  /** 文件名 */
  name: string;

  /** 字节大小 */
  size: number;

  /** 图片预览地址(ObjectURL,移除时自动释放) */
  url?: string;
}

/** send 事件负载。 */
export interface ChatComposerSendPayload {
  /** 附件列表 */
  attachments: AttachmentItem[];

  /** 当前选中的对话模型 ID */
  modelId: string;

  /** 输入文本(已 trim) */
  text: string;
}
