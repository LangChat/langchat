import mysqlSvg from '#/assets/datasource-icons/mysql.svg';
import oracleSvg from '#/assets/datasource-icons/oracle.svg';
import postgresqlSvg from '#/assets/datasource-icons/postgresql.svg';
import sqlserverSvg from '#/assets/datasource-icons/sqlserver.svg';

export const DATASOURCE_TYPE_OPTIONS = [
  {
    description: '轻量、广泛使用的关系型数据库',
    icon: mysqlSvg,
    label: 'MySQL',
    port: 3306,
    value: 'MYSQL',
  },
  {
    description: '功能完整的开源对象关系型数据库',
    icon: postgresqlSvg,
    label: 'PostgreSQL',
    port: 5432,
    value: 'POSTGRESQL',
  },
  {
    description: '面向企业级业务的商业数据库',
    icon: oracleSvg,
    label: 'Oracle',
    port: 1521,
    value: 'ORACLE',
  },
  {
    description: 'Microsoft 企业级关系型数据库',
    icon: sqlserverSvg,
    label: 'SQL Server',
    port: 1433,
    value: 'SQLSERVER',
  },
] as const;

export type DatasourceTypeKey = (typeof DATASOURCE_TYPE_OPTIONS)[number]['value'];

export function getDatasourceTypeMeta(dbType?: string) {
  return (
    DATASOURCE_TYPE_OPTIONS.find((item) => item.value === dbType) ??
    DATASOURCE_TYPE_OPTIONS[0]
  );
}
