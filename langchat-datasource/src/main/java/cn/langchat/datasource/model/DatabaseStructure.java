package cn.langchat.datasource.model;

import java.util.List;

/**
 * 表结构信息。
 *
 * @param tables 表列表
 * @author LangChat Team
 * @since 2026/8/27
 */
public record DatabaseStructure(
        List<TableStructure> tables
) {
    /**
     * 空结构。
     */
    public static DatabaseStructure empty() {
        return new DatabaseStructure(List.of());
    }
}
