package cn.langchat.core.chat.model.request;

import lombok.Data;

/**
 * 分段内容更新请求。
 *
 * @author LangChat Team
 * @since 2026/9/27
 */
@Data
public class SegmentContentUpdateRequest {

    /** 分段内容。 */
    private String content;
}
