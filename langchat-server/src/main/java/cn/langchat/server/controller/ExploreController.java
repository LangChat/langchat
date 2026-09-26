package cn.langchat.server.controller;

import cn.langchat.common.core.ApiResponse;
import cn.langchat.server.model.response.ExploreOverviewResponse;
import cn.langchat.server.service.ExploreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Explore 概览控制器。
 *
 * @author LangChat Team
 * @since 2026/3/27
 */
@RestController
@RequestMapping("/api/v1/explore")
@RequiredArgsConstructor
@Slf4j
public class ExploreController {

    private final ExploreService exploreService;

    /**
     * 查询控台概览数据。
     */
    @GetMapping("/overview")
    public ApiResponse<ExploreOverviewResponse> overview() {
        return ApiResponse.success(exploreService.getOverview());
    }
}
