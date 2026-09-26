package cn.langchat.claw.datasource.support;

import java.util.Locale;

/**
 * 数据库类型枚举，用于驱动加载与 URL 构建。
 *
 * @author LangChat Team
 * @since 2026/8/27
 */
public enum DatasourceType {

    /** MySQL。 */
    MYSQL("com.mysql.cj.jdbc.Driver", 3306) {
        @Override
        public String buildUrl(String host, int port, String database, String schema) {
            return "jdbc:mysql://" + host + ":" + port + "/" + database
                    + "?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true";
        }
    },

    /** PostgreSQL。 */
    POSTGRESQL("org.postgresql.Driver", 5432) {
        @Override
        public String buildUrl(String host, int port, String database, String schema) {
            return "jdbc:postgresql://" + host + ":" + port + "/" + database;
        }
    },

    /** Oracle。 */
    ORACLE("oracle.jdbc.OracleDriver", 1521) {
        @Override
        public String buildUrl(String host, int port, String database, String schema) {
            return "jdbc:oracle:thin:@//" + host + ":" + port + "/" + database;
        }
    },

    /** SQL Server。 */
    SQLSERVER("com.microsoft.sqlserver.jdbc.SQLServerDriver", 1433) {
        @Override
        public String buildUrl(String host, int port, String database, String schema) {
            String base = "jdbc:sqlserver://" + host + ":" + port
                    + ";encrypt=false;trustServerCertificate=true";
            if (database != null && !database.isBlank()) {
                base += ";databaseName=" + database;
            }
            return base;
        }
    };

    private final String driverClass;
    private final int defaultPort;

    DatasourceType(String driverClass, int defaultPort) {
        this.driverClass = driverClass;
        this.defaultPort = defaultPort;
    }

    /**
     * 构建连接 URL。
     */
    public abstract String buildUrl(String host, int port, String database, String schema);

    /**
     * 获取驱动类名。
     */
    public String driverClass() {
        return driverClass;
    }

    /**
     * 获取默认端口。
     */
    public int defaultPort() {
        return defaultPort;
    }

    /**
     * 按字符串解析类型，失败返回 null。
     */
    public static DatasourceType fromCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        String normalized = code.trim().replace("-", "").replace("_", "").toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "MYSQL" -> MYSQL;
            case "POSTGRESQL", "POSTGRES", "PG" -> POSTGRESQL;
            case "ORACLE" -> ORACLE;
            case "SQLSERVER", "MSSQL", "SQL_SERVER" -> SQLSERVER;
            default -> null;
        };
    }
}
