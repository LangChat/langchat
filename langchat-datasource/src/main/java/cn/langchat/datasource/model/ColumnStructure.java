package cn.langchat.datasource.model;

/**
 * 单列的结构信息。
 *
 * @param name 列名
 * @param type 数据类型
 * @param size 长度/精度
 * @param comment 注释（自定义中解释含义）
 * @param primaryKey 是否主键
 * @param nullable 是否可空
 * @author LangChat Team
 * @since 2026/8/27
 */
public record ColumnStructure(
        String name,
        String type,
        Integer size,
        String comment,
        boolean primaryKey,
        boolean nullable
) {
}
