import { $t } from '@vben/locales';

import mysqlSvg from '#/assets/datasource-icons/mysql.svg';
import oracleSvg from '#/assets/datasource-icons/oracle.svg';
import postgresqlSvg from '#/assets/datasource-icons/postgresql.svg';
import sqlserverSvg from '#/assets/datasource-icons/sqlserver.svg';

export function datasourceTypeOptions() {
  return [
    {
      description: $t('datasource.meta.mysql.description'),
      icon: mysqlSvg,
      label: 'MySQL',
      port: 3306,
      value: 'MYSQL',
    },
    {
      description: $t('datasource.meta.postgresql.description'),
      icon: postgresqlSvg,
      label: 'PostgreSQL',
      port: 5432,
      value: 'POSTGRESQL',
    },
    {
      description: $t('datasource.meta.oracle.description'),
      icon: oracleSvg,
      label: 'Oracle',
      port: 1521,
      value: 'ORACLE',
    },
    {
      description: $t('datasource.meta.sqlserver.description'),
      icon: sqlserverSvg,
      label: 'SQL Server',
      port: 1433,
      value: 'SQLSERVER',
    },
  ] as const;
}

export type DatasourceTypeKey = 'MYSQL' | 'ORACLE' | 'POSTGRESQL' | 'SQLSERVER';

export function getDatasourceTypeMeta(dbType?: string) {
  const options = datasourceTypeOptions();
  return (
    options.find((item) => item.value === dbType) ?? options[0]
  );
}
