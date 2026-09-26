package cn.langchat.monitor.model;

import lombok.Builder;
import lombok.Getter;

/**
 * 一次模型调用的记录（待落库）。
 *
 * @author LangChat Team
 * @since 2026/9/26
 */
@Getter
@Builder
public class ModelCallRecord {

    /** 模型元信息。 */
    private final ModelCallMetadata metadata;

    /** 调用类型。 */
    private final ModelCallType callType;

    /** 是否成功。 */
    private final boolean success;

    /** 输入 Token 数。 */
    private final Integer inputToken;

    /** 输出 Token 数。 */
    private final Integer outputToken;

    /** 总 Token 数。 */
    private final Integer totalToken;

    /** 耗时，单位毫秒。 */
    private final Long duration;

    /** 处理条目数。 */
    private final Integer itemCount;

    /** 错误信息。 */
    private final String errorMessage;
}
