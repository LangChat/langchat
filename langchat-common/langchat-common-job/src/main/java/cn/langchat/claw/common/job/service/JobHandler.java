package cn.langchat.claw.common.job.service;

import cn.langchat.claw.common.job.model.JobContext;
import cn.langchat.claw.common.job.model.JobResult;

/**
 * 通用任务处理器接口。
 *
 * @param payload 任务负载类型
 * @author LangChat Team
 * @since 2026/3/24
 */
public interface JobHandler<T> {

    /**
     * 获取任务类型编码。
     */
    String getJobType();

    /**
     * 执行任务。
     */
    JobResult handle(JobContext context, T payload);
}
