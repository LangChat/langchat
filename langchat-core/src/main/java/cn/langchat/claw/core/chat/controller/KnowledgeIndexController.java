package cn.langchat.claw.core.chat.controller;

import cn.langchat.claw.common.core.ApiResponse;
import cn.langchat.claw.core.chat.model.request.KnowledgeIndexRequest;
import cn.langchat.claw.core.chat.model.request.KnowledgeParsePreviewRequest;
import cn.langchat.claw.core.chat.model.response.KnowledgeIndexResult;
import cn.langchat.claw.core.chat.model.response.KnowledgeIndexStatusResult;
import cn.langchat.claw.core.chat.model.response.KnowledgeParsePreviewResult;
import java.util.List;
import cn.langchat.claw.core.chat.service.KnowledgeIndexRuntimeService;
import org.springframework.web.bind.annotation.GetMapping;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 知识库索引控制器。
 *
 * @author LangChat Team
 * @since 2026/3/25
 */
@RestController
@RequestMapping("/api/v1/core/knowledges")
@RequiredArgsConstructor
@Slf4j
public class KnowledgeIndexController {

    private final KnowledgeIndexRuntimeService knowledgeIndexRuntimeService;

    /**
     * 执行知识库索引。
     */
    @PostMapping("/{knowledgeId}/index")
    public ApiResponse<KnowledgeIndexResult> indexKnowledge(
            @PathVariable("knowledgeId") String knowledgeId,
            @RequestBody(required = false) KnowledgeIndexRequest request
    ) {
        log.info("接收知识库索引请求，knowledgeId={}", knowledgeId);
        return ApiResponse.success(knowledgeIndexRuntimeService.indexKnowledge(
                knowledgeId,
                request == null ? new KnowledgeIndexRequest() : request
        ));
    }

    /**
     * 预览知识库文档解析结果。
     */
    @PostMapping("/{knowledgeId}/parse-preview")
    public ApiResponse<KnowledgeParsePreviewResult> previewKnowledge(
            @PathVariable("knowledgeId") String knowledgeId,
            @RequestBody(required = false) KnowledgeParsePreviewRequest request
    ) {
        log.info("接收知识库解析预览请求，knowledgeId={}", knowledgeId);
        return ApiResponse.success(knowledgeIndexRuntimeService.previewKnowledge(
                knowledgeId,
                request == null ? new KnowledgeParsePreviewRequest() : request
        ));
    }

    /**
     * 查询知识库文档向量化状态。
     */
    @GetMapping("/{knowledgeId}/index-status")
    public ApiResponse<KnowledgeIndexStatusResult> queryIndexStatus(
            @PathVariable("knowledgeId") String knowledgeId,
            @RequestParam(value = "docsIds", required = false) List<String> docsIds
    ) {
        log.info("接收知识库索引状态查询请求，knowledgeId={}, docsCount={}", knowledgeId, docsIds == null ? 0 : docsIds.size());
        return ApiResponse.success(knowledgeIndexRuntimeService.queryIndexStatus(knowledgeId, docsIds));
    }
}
