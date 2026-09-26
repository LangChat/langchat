/**
 * ChatComposer 公共类型定义。
 */

/** 附件条目(组件内本地暂存,发送时整体交给调用方处理上传)。 */
export interface attachmentitem {
  /** 原始文件对象 */
  file: file;

  /** 唯一标识 */
  id: string;

  /** 是否图片(决定缩略图展示) */
  isImage: boolean;

  /** 文件名 */
  name: string;

  /** 字节大小 */
  size: number;

  /** 图片预览地址(ObjectURL,移除时自动释放) */
  url?: string;
}

/** send 事件负载。 */
export interface chatcomposersendpayload {
  /** 附件列表 */
  attachments: attachmentitem[];

  /** 当前选中的对话模型 ID */
  modelId: string;

  /** 输入文本(已 trim) */
  text: string;
}
