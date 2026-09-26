package cn.langchat.core.chat.model.response;

import java.util.List;

/**
 * 智能问数可选数据源及表。
 */
public record DataAnalysisCatalogItem(
        String datasourceId,
        String datasourceName,
        String dbType,
        List<TableItem> tables
) {
    public record TableItem(
            String name,
            String sourceName,
            String comment,
            int columnCount
    ) {
    }
}
