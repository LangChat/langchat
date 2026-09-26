import type { BaseEntity } from './_shared';

import { requestClient } from '#/api/request';

/**
 * 数据源实体。
 */
export interface AigcDatasource extends BaseEntity {
  dbType?: string;
  databaseName?: string;
  enabled?: boolean;
  host?: string;
  name?: string;
  password?: string;
  port?: number;
  remark?: string;
  schemaName?: string;
  structureJson?: string;
  url?: string;
  username?: string;
}

/**
 * 数据库表结构。
 */
export interface DatabaseStructure {
  tables: TableStructure[];
}

export interface TableStructure {
  columns: ColumnStructure[];
  /** 是否允许 AI 检索该表（自定义结构专用） */
  enabled?: boolean;
  /** 是否已明确导入智能问数配置 */
  imported?: boolean;
  /** 对应实时内省结果中的原始表名 */
  sourceTableName?: string;
  tableComment?: string;
  tableName: string;
}

export interface ColumnStructure {
  comment?: string;
  name: string;
  nullable?: boolean;
  /** 自定义结构中保留的原始字段名 */
  originalName?: string;
  primaryKey?: boolean;
  size?: number;
  type?: string;
}

export interface TestConnectionPayload {
  databaseName?: string;
  dbType?: string;
  host?: string;
  password?: string;
  port?: number;
  username?: string;
}

const basePath = '/v1/datasources';

export function listDataSources() {
  return requestClient.get<AigcDatasource[]>(basePath);
}

export function getDataSource(id: string) {
  return requestClient.get<AigcDatasource>(`${basePath}/${id}`);
}

export function createDataSource(payload: Partial<AigcDatasource>) {
  return requestClient.post<boolean>(basePath, payload);
}

export function updateDataSource(id: string, payload: Partial<AigcDatasource>) {
  return requestClient.put<boolean>(`${basePath}/${id}`, payload);
}

export function deleteDataSource(id: string) {
  return requestClient.delete<boolean>(`${basePath}/${id}`);
}

export function testDataSource(payload: TestConnectionPayload) {
  return requestClient.post<boolean>(`${basePath}/test`, payload);
}

export function introspectDataSource(id: string) {
  return requestClient.get<DatabaseStructure>(`${basePath}/${id}/introspect`);
}

export function getDataSourceStructure(id: string) {
  return requestClient.get<{ structureJson?: string }>(
    `${basePath}/${id}/structure`,
  );
}

export function saveDataSourceStructure(id: string, structureJson: string) {
  return requestClient.put<boolean>(`${basePath}/${id}/structure`, {
    structureJson,
  });
}
