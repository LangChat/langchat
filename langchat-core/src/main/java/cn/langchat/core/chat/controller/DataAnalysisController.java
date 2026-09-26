package cn.langchat.core.chat.controller;

import cn.langchat.common.core.ApiResponse;
import cn.langchat.core.chat.model.request.DataAnalysisRequest;
import cn.langchat.core.chat.model.response.DataAnalysisCatalogItem;
import cn.langchat.core.chat.service.DataAnalysisService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * 智能问数接口。
 */
@RestController
@RequestMapping("/api/v1/core/data-analysis")
@RequiredArgsConstructor
public class DataAnalysisController {

    private final DataAnalysisService dataAnalysisService;

    @GetMapping("/catalog")
    public ApiResponse<List<DataAnalysisCatalogItem>> catalog() {
        return ApiResponse.success(dataAnalysisService.listCatalog());
    }

    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> stream(@RequestBody DataAnalysisRequest request) {
        return dataAnalysisService.stream(request);
    }
}
