import { requestClient } from '#/api/request';

/**
 * 图片生成请求。
 */
export interface ImageGenerationPayload {
  modelId: string;
  prompt: string;
  quality?: string;
  responseFormat?: string;
  n?: number;
  size?: string;
}

/**
 * 图片生成结果。
 */
export interface ImageGenerationResult {
  base64Data?: string;
  revisedPrompt?: string;
  url?: string;
}

/**
 * 图片识别 (OCR) 请求。
 */
export interface ImageRecognizePayload {
  image: string;
  modelId: string;
  prompt?: string;
}

/**
 * 图片识别 (OCR) 结果。
 */
export interface ImageRecognizeResult {
  text: string;
}

export function generateImage(payload: ImageGenerationPayload) {
  return requestClient.post<ImageGenerationResult>('/v1/image/generate', payload);
}

export function recognizeImage(payload: ImageRecognizePayload) {
  return requestClient.post<ImageRecognizeResult>('/v1/image/recognition', payload);
}
