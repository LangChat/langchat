package cn.langchat.core.chat.model.request;

import lombok.Data;

/**
 * 分段启停请求。
 *
 * @author LangChat Team
 * @since 2026/9/27
 */
@Data
public class SegmentEnabledRequest {

    /** 是否启用该分段。 */
    private Boolean enabled;
}
