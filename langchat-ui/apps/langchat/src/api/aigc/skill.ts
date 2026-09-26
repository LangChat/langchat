import { requestClient } from '#/api/request';

import type { BaseEntity } from './_shared';

/**
 * 技能实体。技能以标准化技能包（根目录含 SKILL.md）形式管理，
 * 原始包归档 OSS，解压副本存于服务器本地工作区。
 */
export interface AigcSkill extends BaseEntity {
  description?: string;
  enabled?: boolean;
  entryFile?: string;
  fileCount?: number;
  license?: string;
  localPath?: string;
  name?: string;
  ossFilename?: string;
  ossObjectKey?: string;
  packageSize?: number;
  tags?: string;
  title?: string;
  version?: string;
}

/** 技能包内文件树节点。 */
export interface SkillFileNode {
  children: SkillFileNode[];
  directory: boolean;
  name: string;
  path: string;
  size: number;
}

/** 技能包内文件内容。 */
export interface SkillFileContent {
  binary: boolean;
  content?: null | string;
  path: string;
  size: number;
}

export const skillApi = {
  detail: (id: string) => requestClient.get<AigcSkill>(`/v1/aigc/skills/${id}`),
  downloadPackage: (id: string) =>
    requestClient.get<Blob>(`/v1/aigc/skills/${id}/download`, {
      responseType: 'blob',
    }),

  list: () => requestClient.get<AigcSkill[]>('/v1/aigc/skills'),
  listFiles: (id: string) =>
    requestClient.get<SkillFileNode[]>(`/v1/aigc/skills/${id}/files`),
  readFile: (id: string, path: string) =>
    requestClient.get<SkillFileContent>(`/v1/aigc/skills/${id}/file`, {
      params: { path },
    }),
  remove: (id: string) =>
    requestClient.delete<boolean>(`/v1/aigc/skills/${id}`),
  saveFile: (id: string, path: string, content: string) =>
    requestClient.put<AigcSkill>(`/v1/aigc/skills/${id}/file`, {
      content,
      path,
    }),
  update: (id: string, payload: Partial<AigcSkill>) =>
    requestClient.put<boolean>(`/v1/aigc/skills/${id}`, payload),
  uploadFolder: (formData: FormData) =>
    requestClient.post<AigcSkill>('/v1/aigc/skills/upload/folder', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    }),
  uploadPackage: (formData: FormData) =>
    requestClient.post<AigcSkill>('/v1/aigc/skills/upload/package', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    }),
};
