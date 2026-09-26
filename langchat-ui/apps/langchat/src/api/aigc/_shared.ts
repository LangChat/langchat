import { requestClient } from '#/api/request';

/**
 * 基础实体类型。
 */
export interface BaseEntity {
  createTime?: number;
  creator?: string;
  id?: string;
  updateTime?: number;
  updater?: string;
}

export function createCrudApi<T extends BaseEntity>(basePath: string) {
  return {
    create: (payload: Partial<T>) =>
      requestClient.post<boolean>(basePath, payload),
    detail: (id: string) => requestClient.get<T>(`${basePath}/${id}`),
    list: (params?: Record<string, number | string | undefined>) =>
      requestClient.get<T[]>(basePath, { params }),
    remove: (id: string) => requestClient.delete<boolean>(`${basePath}/${id}`),
    update: (id: string, payload: Partial<T>) =>
      requestClient.put<boolean>(`${basePath}/${id}`, payload),
  };
}
