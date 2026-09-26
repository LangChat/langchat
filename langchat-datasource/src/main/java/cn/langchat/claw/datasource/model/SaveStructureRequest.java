package cn.langchat.claw.datasource.model;

/**
 * 保存自定义表结构请求。
 *
 * @param structureJson 自定义表结构 JSON 文本
 * @author LangChat Team
 * @since 2026/8/27
 */
public record SaveStructureRequest(
        String structureJson
) {
}
