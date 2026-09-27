package cn.langchat.core.chat.model.request;

import java.util.List;
import lombok.Data;

/**
 * 分段批量删除请求。
 *
 * @author LangChat Team
 * @since 2026/9/27
 */
@Data
public class SegmentDeleteRequest {

    /** 待删除的分段ID列表。 */
    private List<String> segmentIds;
}
