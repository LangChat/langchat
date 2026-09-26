package cn.langchat.common.job.service;

import cn.langchat.common.job.model.JobContext;
import cn.langchat.common.job.model.JobResult;

/**
 * 任务分发器接口。
 *
 * @author LangChat Team
 * @since 2026/3/24
 */
public interface JobDispatcher {

    /**
     * 按任务类型分发任务。
     */
    JobResult dispatch(JobContext context, Object payload);
}
