package cn.langchat.core.chat.service;

import cn.langchat.core.chat.model.request.DataAnalysisRequest;
import cn.langchat.core.chat.model.response.DataAnalysisCatalogItem;
import java.util.List;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;

/**
 * 智能问数服务。
 */
public interface DataAnalysisService {

    List<DataAnalysisCatalogItem> listCatalog();

    Flux<ServerSentEvent<String>> stream(DataAnalysisRequest request);
}
