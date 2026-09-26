package cn.langchat.monitor.controller;

import cn.langchat.common.core.ApiResponse;
import cn.langchat.monitor.model.response.ModelCallOverviewResponse;
import cn.langchat.monitor.service.ModelCallStatisticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 模型调用监控控制器。
 *
 * @author LangChat Team
 * @since 2026/9/26
 */
@RestController
@RequestMapping("/api/v1/monitor/model-calls")
@RequiredArgsConstructor
@Slf4j
public class ModelCallMonitorController {

    private final ModelCallStatisticsService modelCallStatisticsService;

    /**
     * 查询模型调用总览。
     *
     * @param windowDays 统计窗口天数，默认 7 天
     */
    @GetMapping("/overview")
    public ApiResponse<ModelCallOverviewResponse> overview(
            @RequestParam(value = "windowDays", required = false) Integer windowDays
    ) {
        return ApiResponse.success(modelCallStatisticsService.getOverview(windowDays));
    }
}
