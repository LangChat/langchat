package cn.langchat.datasource.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.langchat.common.exception.BizException;
import cn.langchat.datasource.entity.AigcDatasource;
import cn.langchat.datasource.mapper.AigcDatasourceMapper;
import cn.langchat.datasource.model.DataQueryResult;
import cn.langchat.datasource.model.DatabaseStructure;
import cn.langchat.datasource.model.TestConnectionRequest;
import cn.langchat.datasource.service.AigcDatasourceService;
import cn.langchat.datasource.support.DatabaseIntrospector;
import cn.langchat.datasource.support.DatasourceErrorCode;
import cn.langchat.datasource.support.DatasourceType;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.util.TablesNamesFinder;
import org.springframework.stereotype.Service;

/**
 * 数据源 Service 实现。
 *
 * @author LangChat Team
 * @since 2026/8/27
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AigcDatasourceServiceImpl extends ServiceImpl<AigcDatasourceMapper, AigcDatasource>
        implements AigcDatasourceService {

    private final DatabaseIntrospector databaseIntrospector;

    @Override
    public boolean save(AigcDatasource datasource) {
        datasource.setUrl(buildJdbcUrl(
                datasource.getDbType(),
                datasource.getHost(),
                datasource.getPort(),
                datasource.getDatabaseName()
        ));
        return super.save(datasource);
    }

    @Override
    public boolean updateById(AigcDatasource datasource) {
        datasource.setUrl(buildJdbcUrl(
                datasource.getDbType(),
                datasource.getHost(),
                datasource.getPort(),
                datasource.getDatabaseName()
        ));
        return super.updateById(datasource);
    }

    @Override
    public boolean testConnection(TestConnectionRequest request) {
        if (request == null) {
            throw new BizException(DatasourceErrorCode.CONNECTION_FAILED.code(), "连接参数不能为空");
        }
        DatasourceType type = DatasourceType.fromCode(request.dbType());
        String url = buildJdbcUrl(request.dbType(), request.host(), request.port(), request.databaseName());
        try {
            connect(type, url, request.username(), request.password());
            return true;
        } catch (Exception ex) {
            log.warn("测试连接失败，url={}, error={}", url, ex.getMessage());
            throw new BizException(DatasourceErrorCode.CONNECTION_FAILED.code(),
                    "数据库连接失败: " + ex.getMessage());
        }
    }

    @Override
    public DatabaseStructure introspect(String id) throws Exception {
        AigcDatasource datasource = load(id);
        DatasourceType type = DatasourceType.fromCode(datasource.getDbType());
        if (type == null) {
            throw new BizException(DatasourceErrorCode.INTROSPECT_FAILED.code(), "不支持的数据库类型");
        }
        String url = buildJdbcUrl(
                datasource.getDbType(),
                datasource.getHost(),
                datasource.getPort(),
                datasource.getDatabaseName()
        );
        if (!url.equals(datasource.getUrl())) {
            datasource.setUrl(url);
            super.updateById(datasource);
        }
        try {
            return databaseIntrospector.introspect(
                    datasource.getDbType(),
                    url,
                    datasource.getUsername(),
                    datasource.getPassword(),
                    datasource.getSchemaName()
            );
        } catch (Exception ex) {
            log.warn("内省失败，datasourceId={}, error={}", id, ex.getMessage());
            throw new BizException(DatasourceErrorCode.INTROSPECT_FAILED.code(),
                    "表结构内省失败: " + ex.getMessage());
        }
    }

    @Override
    public String getStructure(String id) {
        return load(id).getStructureJson();
    }

    @Override
    public boolean saveStructure(String id, String structureJson) {
        AigcDatasource datasource = load(id);
        datasource.setStructureJson(structureJson);
        return updateById(datasource);
    }

    @Override
    public DataQueryResult executeReadOnlyQuery(String id, String sql, Set<String> allowedTables) {
        AigcDatasource datasource = load(id);
        validateReadOnlySql(sql, allowedTables);
        DatasourceType type = DatasourceType.fromCode(datasource.getDbType());
        String url = buildJdbcUrl(
                datasource.getDbType(),
                datasource.getHost(),
                datasource.getPort(),
                datasource.getDatabaseName()
        );
        try {
            Class.forName(type.driverClass());
            try (Connection connection = DriverManager.getConnection(url, datasource.getUsername(), datasource.getPassword())) {
                connection.setReadOnly(true);
                try (java.sql.Statement statement = connection.createStatement()) {
                    statement.setMaxRows(200);
                    statement.setQueryTimeout(30);
                    try (ResultSet resultSet = statement.executeQuery(sql)) {
                        return readQueryResult(resultSet);
                    }
                }
            }
        } catch (Exception ex) {
            log.warn("智能问数查询失败，datasourceId={}, error={}", id, ex.getMessage());
            throw new BizException(DatasourceErrorCode.QUERY_FAILED.code(), "数据查询失败: " + ex.getMessage());
        }
    }

    private void validateReadOnlySql(String sql, Set<String> allowedTables) {
        if (StrUtil.isBlank(sql) || allowedTables == null || allowedTables.isEmpty()) {
            throw new BizException(DatasourceErrorCode.QUERY_FAILED.code(), "查询语句或授权表不能为空");
        }
        try {
            Statement parsed = CCJSqlParserUtil.parse(sql);
            if (!(parsed instanceof Select)) {
                throw new BizException(DatasourceErrorCode.QUERY_FAILED.code(), "仅允许执行 SELECT 查询");
            }
            Set<String> normalizedAllowed = allowedTables.stream()
                    .map(this::normalizeTableName)
                    .collect(Collectors.toSet());
            List<String> referencedTables = new TablesNamesFinder().getTableList(parsed);
            if (referencedTables.isEmpty()) {
                throw new BizException(DatasourceErrorCode.QUERY_FAILED.code(), "查询未引用任何授权表");
            }
            for (String referencedTable : referencedTables) {
                if (!normalizedAllowed.contains(normalizeTableName(referencedTable))) {
                    throw new BizException(
                            DatasourceErrorCode.QUERY_FAILED.code(),
                            "查询包含未授权表: " + referencedTable
                    );
                }
            }
        } catch (BizException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BizException(DatasourceErrorCode.QUERY_FAILED.code(), "SQL 解析失败: " + ex.getMessage());
        }
    }

    private DataQueryResult readQueryResult(ResultSet resultSet) throws SQLException {
        ResultSetMetaData metaData = resultSet.getMetaData();
        List<String> columns = new ArrayList<>();
        for (int index = 1; index <= metaData.getColumnCount(); index++) {
            columns.add(metaData.getColumnLabel(index));
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        while (resultSet.next()) {
            Map<String, Object> row = new LinkedHashMap<>();
            for (int index = 1; index <= columns.size(); index++) {
                row.put(columns.get(index - 1), resultSet.getObject(index));
            }
            rows.add(row);
        }
        return new DataQueryResult(columns, rows);
    }

    private String normalizeTableName(String tableName) {
        String normalized = StrUtil.blankToDefault(tableName, "").trim();
        int dotIndex = normalized.lastIndexOf('.');
        if (dotIndex >= 0) {
            normalized = normalized.substring(dotIndex + 1);
        }
        return normalized.replace("`", "").replace("\"", "")
                .toLowerCase(Locale.ROOT);
    }

    private void connect(DatasourceType type, String url, String username, String password) throws Exception {
        if (type == null) {
            throw new IllegalArgumentException("不支持的数据库类型");
        }
        Class.forName(type.driverClass());
        try (java.sql.Connection connection = java.sql.DriverManager.getConnection(url, username, password)) {
            if (connection == null || connection.isClosed()) {
                throw new IllegalStateException("连接为空或已关闭");
            }
        }
    }

    private String buildJdbcUrl(String dbType, String host, Integer port, String databaseName) {
        DatasourceType type = DatasourceType.fromCode(dbType);
        if (type == null) {
            throw new BizException(DatasourceErrorCode.CONNECTION_FAILED.code(), "不支持的数据库类型");
        }
        if (StrUtil.isBlank(host)) {
            throw new BizException(DatasourceErrorCode.CONNECTION_FAILED.code(), "主机地址不能为空");
        }
        int resolvedPort = port == null || port <= 0 ? type.defaultPort() : port;
        return type.buildUrl(host.trim(), resolvedPort, StrUtil.blankToDefault(databaseName, ""), null);
    }

    private AigcDatasource load(String id) {
        if (StrUtil.isBlank(id)) {
            throw new BizException(DatasourceErrorCode.DATASOURCE_NOT_FOUND);
        }
        AigcDatasource datasource = getById(id);
        if (datasource == null) {
            throw new BizException(DatasourceErrorCode.DATASOURCE_NOT_FOUND);
        }
        return datasource;
    }
}
