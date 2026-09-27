package cn.langchat.core.chat.controller;

import cn.langchat.aigc.biz.entity.AigcSegment;
import cn.langchat.common.core.ApiResponse;
import cn.langchat.core.chat.model.request.SegmentContentUpdateRequest;
import cn.langchat.core.chat.model.request.SegmentDeleteRequest;
import cn.langchat.core.chat.model.request.SegmentEnabledRequest;
import cn.langchat.core.chat.service.KnowledgeSegmentService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 知识库分段管理控制器。
 *
 * @author LangChat Team
 * @since 2026/9/27
 */
@RestController
@RequestMapping("/api/v1/core/knowledges/{knowledgeId}/segments")
@RequiredArgsConstructor
@Slf4j
public class KnowledgeSegmentController {

    private final KnowledgeSegmentService knowledgeSegmentService;

    /**
     * 查询知识库下指定文档的分段列表。
     */
    @GetMapping
    public ApiResponse<List<AigcSegment>> listSegments(
            @PathVariable("knowledgeId") String knowledgeId,
            @RequestParam(value = "docsId") String docsId
    ) {
        return ApiResponse.success(knowledgeSegmentService.listSegments(knowledgeId, docsId));
    }

    /**
     * 更新分段内容。
     */
    @PutMapping("/{segmentId}")
    public ApiResponse<Boolean> updateSegmentContent(
            @PathVariable("knowledgeId") String knowledgeId,
            @PathVariable("segmentId") String segmentId,
            @RequestBody(required = false) SegmentContentUpdateRequest request
    ) {
        knowledgeSegmentService.updateSegmentContent(knowledgeId, segmentId, request);
        return ApiResponse.success(Boolean.TRUE);
    }

    /**
     * 启停分段，同步向量库数据。
     */
    @PutMapping("/{segmentId}/enabled")
    public ApiResponse<Boolean> updateSegmentEnabled(
            @PathVariable("knowledgeId") String knowledgeId,
            @PathVariable("segmentId") String segmentId,
            @RequestBody(required = false) SegmentEnabledRequest request
    ) {
        knowledgeSegmentService.updateSegmentEnabled(knowledgeId, segmentId, request);
        return ApiResponse.success(Boolean.TRUE);
    }

    /**
     * 对单个分段重新向量化。
     */
    @PostMapping("/{segmentId}/reindex")
    public ApiResponse<Boolean> reindexSegment(
            @PathVariable("knowledgeId") String knowledgeId,
            @PathVariable("segmentId") String segmentId
    ) {
        knowledgeSegmentService.reindexSegment(knowledgeId, segmentId);
        return ApiResponse.success(Boolean.TRUE);
    }

    /**
     * 删除单个分段并清理向量数据。
     */
    @DeleteMapping("/{segmentId}")
    public ApiResponse<Boolean> deleteSegment(
            @PathVariable("knowledgeId") String knowledgeId,
            @PathVariable("segmentId") String segmentId
    ) {
        knowledgeSegmentService.deleteSegment(knowledgeId, segmentId);
        return ApiResponse.success(Boolean.TRUE);
    }

    /**
     * 批量删除分段并清理向量数据。
     */
    @DeleteMapping
    public ApiResponse<Boolean> deleteSegments(
            @PathVariable("knowledgeId") String knowledgeId,
            @RequestBody(required = false) SegmentDeleteRequest request
    ) {
        knowledgeSegmentService.deleteSegments(knowledgeId, request);
        return ApiResponse.success(Boolean.TRUE);
    }
}
