package cn.langchat.server.service;

import cn.langchat.server.model.response.ExploreOverviewResponse;

/**
 * Explore 概览服务接口。
 *
 * @author LangChat Team
 * @since 2026/3/27
 */
public interface ExploreService {

    /**
     * 查询概览指标。
     */
    ExploreOverviewResponse getOverview();
}
