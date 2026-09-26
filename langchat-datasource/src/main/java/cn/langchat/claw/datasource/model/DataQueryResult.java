package cn.langchat.claw.datasource.model;

import java.util.List;
import java.util.Map;

/**
 * 只读数据查询结果。
 *
 * @param columns 结果列
 * @param rows 结果行
 */
public record DataQueryResult(
        List<String> columns,
        List<Map<String, Object>> rows
) {
}
