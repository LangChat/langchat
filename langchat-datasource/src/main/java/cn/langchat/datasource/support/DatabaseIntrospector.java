package cn.langchat.datasource.support;

import cn.langchat.datasource.model.ColumnStructure;
import cn.langchat.datasource.model.DatabaseStructure;
import cn.langchat.datasource.model.TableStructure;
import cn.hutool.core.util.StrUtil;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * 数据库表结构内省器。
 *
 * <p>使用 {@link DriverManager} 打开一次性连接，并通过
 * {@link DatabaseMetaData} 读取表、列、主键与注释。</p>
 *
 * @author LangChat Team
 * @since 2026/8/27
 */
@Component
public class DatabaseIntrospector {

    /**
     * 内省数据库表结构。
     *
     * @param dbType 数据库类型
     * @param url 连接 URL
     * @param username 用户名
     * @param password 密码
     * @param schema 目标 schema（Oracle / PostgreSQL 使用，可为空）
     */
    public DatabaseStructure introspect(String dbType, String url, String username, String password, String schema)
            throws SQLException {
        DatasourceType type = DatasourceType.fromCode(dbType);
        if (type == null) {
            throw new SQLException("不支持的数据库类型: " + dbType);
        }

        try (Connection connection = open(url, username, password, type)) {
            DatabaseMetaData metaData = connection.getMetaData();
            String catalog = resolveCatalog(connection, type);
            String schemaPattern = resolveSchema(type, schema);

            List<TableStructure> tables = new ArrayList<>();
            try (ResultSet tableRs = metaData.getTables(catalog, schemaPattern, "%", new String[]{"TABLE"})) {
                while (tableRs.next()) {
                    String tableName = tableRs.getString("TABLE_NAME");
                    String tableComment = tableRs.getString("REMARKS");
                    List<ColumnStructure> columns = readColumns(metaData, catalog, schemaPattern, tableName, type);
                    if (columns.isEmpty()) {
                        continue;
                    }
                    tables.add(new TableStructure(tableName, tableComment, columns));
                }
            }
            return new DatabaseStructure(tables);
        }
    }

    private List<ColumnStructure> readColumns(
            DatabaseMetaData metaData,
            String catalog,
            String schemaPattern,
            String tableName,
            DatasourceType type) throws SQLException {
        Set<String> primaryKeys = readPrimaryKeys(metaData, catalog, schemaPattern, tableName);
        Map<String, ColumnStructure> columnMap = new LinkedHashMap<>();
        try (ResultSet columnRs = metaData.getColumns(catalog, schemaPattern, tableName, "%")) {
            while (columnRs.next()) {
                String columnName = columnRs.getString("COLUMN_NAME");
                String dbType = columnRs.getString("TYPE_NAME");
                int size = columnRs.getInt("COLUMN_SIZE");
                String comment = columnRs.getString("REMARKS");
                int nullable = columnRs.getInt("NULLABLE");
                columnMap.put(columnName, new ColumnStructure(
                        columnName,
                        normalizeType(dbType, size),
                        size,
                        comment,
                        primaryKeys.contains(columnName),
                        nullable != DatabaseMetaData.columnNoNulls
                ));
            }
        }
        return new ArrayList<>(columnMap.values());
    }

    private Set<String> readPrimaryKeys(
            DatabaseMetaData metaData,
            String catalog,
            String schemaPattern,
            String tableName) throws SQLException {
        Set<String> primaryKeys = new HashSet<>();
        try (ResultSet rs = metaData.getPrimaryKeys(catalog, schemaPattern, tableName)) {
            while (rs.next()) {
                primaryKeys.add(rs.getString("COLUMN_NAME"));
            }
        }
        return primaryKeys;
    }

    /**
     * 打开连接。
     */
    private Connection open(String url, String username, String password, DatasourceType type) throws SQLException {
        try {
            Class.forName(type.driverClass());
        } catch (ClassNotFoundException ex) {
            throw new SQLException("未找到 JDBC 驱动: " + type.driverClass(), ex);
        }
        return DriverManager.getConnection(url, username, password);
    }

    private String resolveCatalog(Connection connection, DatasourceType type) throws SQLException {
        return switch (type) {
            case MYSQL, POSTGRESQL, SQLSERVER -> connection.getCatalog();
            case ORACLE -> null;
        };
    }

    private String resolveSchema(DatasourceType type, String schema) {
        if (StrUtil.isNotBlank(schema)) {
            return schema;
        }
        return switch (type) {
            case MYSQL -> null;
            case POSTGRESQL -> "public";
            case ORACLE -> null;
            case SQLSERVER -> "dbo";
        };
    }

    /**
     * 归一化数据类型，保留长度。
     */
    private String normalizeType(String typeName, int size) {
        if (StrUtil.isBlank(typeName)) {
            return "UNKNOWN";
        }
        String upper = typeName.toUpperCase(Locale.ROOT);
        // 常见字符类型附加长度，便于前端展示
        if (size > 0 && isLengthType(upper)) {
            return upper + "(" + size + ")";
        }
        return upper;
    }

    private boolean isLengthType(String type) {
        return type.equals("VARCHAR")
                || type.equals("CHAR")
                || type.equals("NVARCHAR")
                || type.equals("NCHAR")
                || type.equals("VARCHAR2")
                || type.equals("NVARCHAR2")
                || type.equals("VARBINARY")
                || type.equals("BINARY");
    }
}
