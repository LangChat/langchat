package cn.langchat.claw.datasource.model;

import java.util.List;

/**
 * 单张表的结构信息。
 *
 * @param tableName 表名
 * @param tableComment 表注释
 * @param columns 列列表
 * @author LangChat Team
 * @since 2026/8/27
 */
public record TableStructure(
        String tableName,
        String tableComment,
        List<ColumnStructure> columns
) {
}
