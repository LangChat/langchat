package cn.langchat.core.chat.model.request;

import java.util.List;
import lombok.Data;

/**
 * 智能问数请求。
 */
@Data
public class DataAnalysisRequest {

    private String conversationId;
    private String datasourceId;
    private String modelId;
    private String question;
    private List<String> tableNames;
}
