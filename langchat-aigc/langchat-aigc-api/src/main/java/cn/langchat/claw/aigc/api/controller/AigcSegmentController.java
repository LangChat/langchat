package cn.langchat.claw.aigc.api.controller;

import cn.langchat.claw.aigc.biz.entity.AigcSegment;
import cn.langchat.claw.aigc.biz.service.AigcSegmentService;
import cn.langchat.claw.common.core.ApiResponse;
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
 * 文档切片管理控制器。
 *
 * @author LangChat Team
 * @since 2026/3/24
 */
@RestController
@RequestMapping("/api/v1/aigc/segments")
@RequiredArgsConstructor
@Slf4j
public class AigcSegmentController {

    private final AigcSegmentService aigcSegmentService;

    @GetMapping
    public ApiResponse<List<AigcSegment>> list(@RequestParam(value = "docsId", required = false) String docsId) {
        return ApiResponse.success(aigcSegmentService.lambdaQuery()
                .eq(docsId != null && !docsId.isBlank(), AigcSegment::getDocsId, docsId)
                .orderByAsc(AigcSegment::getPosition)
                .list());
    }

    @GetMapping("/{id}")
    public ApiResponse<AigcSegment> detail(@PathVariable("id") String id) {
        return ApiResponse.success(aigcSegmentService.getById(id));
    }

    @PostMapping
    public ApiResponse<Boolean> create(@RequestBody AigcSegment segment) {
        log.info("新增切片，docsId={}, position={}", segment.getDocsId(), segment.getPosition());
        return ApiResponse.success(aigcSegmentService.save(segment));
    }

    @PutMapping("/{id}")
    public ApiResponse<Boolean> update(@PathVariable("id") String id, @RequestBody AigcSegment segment) {
        segment.setId(id);
        log.info("更新切片，id={}", id);
        return ApiResponse.success(aigcSegmentService.updateById(segment));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Boolean> remove(@PathVariable("id") String id) {
        log.info("删除切片，id={}", id);
        return ApiResponse.success(aigcSegmentService.removeById(id));
    }
}
