package cn.langchat.claw.server.health;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * MySQL 与 Pgvector 存储健康检查。
 *
 * @author LangChat Team
 * @since 2026/3/24
 */
@Component("storage")
public class DualDataSourceHealthIndicator implements HealthIndicator {

    private final DataSource mysqlDataSource;
    private final DataSource pgvectorDataSource;

    public DualDataSourceHealthIndicator(
            @Qualifier("mysqlDataSource") ObjectProvider<DataSource> mysqlDataSourceProvider,
            @Qualifier("pgvectorDataSource") ObjectProvider<DataSource> pgvectorDataSourceProvider) {
        this.mysqlDataSource = mysqlDataSourceProvider.getIfAvailable();
        this.pgvectorDataSource = pgvectorDataSourceProvider.getIfAvailable();
    }

    @Override
    public Health health() {
        Health.Builder builder = Health.up();
        boolean healthy = true;

        healthy &= appendDataSourceHealth(builder, "mysql", mysqlDataSource);
        healthy &= appendDataSourceHealth(builder, "pgvector", pgvectorDataSource);

        if (!healthy) {
            builder.down();
        }
        return builder.build();
    }

    private boolean appendDataSourceHealth(Health.Builder builder, String name, DataSource dataSource) {
        if (dataSource == null) {
            builder.withDetail(name, "not configured");
            return false;
        }

        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            builder.withDetail(name, metaData.getDatabaseProductName() + "@" + metaData.getURL());
            return true;
        } catch (SQLException exception) {
            builder.withDetail(name, exception.getMessage());
            return false;
        }
    }
}
