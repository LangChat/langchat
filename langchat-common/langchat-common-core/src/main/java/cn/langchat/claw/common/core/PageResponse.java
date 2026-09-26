package cn.langchat.claw.common.core;

import java.util.List;

/**
 * 通用分页响应体。
 *
 * @param pageNo 当前页码
 * @param pageSize 每页条数
 * @param total 总记录数
 * @param list 当前页数据
 * @author LangChat Team
 * @since 2026/3/24
 */
public record PageResponse<T>(long pageNo, long pageSize, long total, List<T> list) {
}
