package cn.langchat.common.job.service.impl;

import cn.langchat.common.job.model.JobContext;
import cn.langchat.common.job.model.JobResult;
import cn.langchat.common.job.service.JobDispatcher;
import cn.langchat.common.job.service.JobHandler;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 直接执行型任务分发器。
 *
 * 当前实现同步执行，便于前期快速接入任务能力；
 * 后续如果引入调度框架，可以在此处无缝替换。
 *
 * @author LangChat Team
 * @since 2026/3/24
 */
@Slf4j
@Component
public class DirectJobDispatcher implements JobDispatcher {

    private final Map<String, JobHandler<Object>> handlerMap;

    @SuppressWarnings("unchecked")
    public DirectJobDispatcher(List<JobHandler<?>> jobHandlers) {
        this.handlerMap = jobHandlers.stream()
                .collect(java.util.stream.Collectors.toMap(
                        JobHandler::getJobType,
                        handler -> (JobHandler<Object>) handler,
                        (left, right) -> left));
    }

    @Override
    public JobResult dispatch(JobContext context, Object payload) {
        JobHandler<Object> jobHandler = handlerMap.get(context.jobType());
        if (jobHandler == null) {
            return JobResult.failure("未找到对应的任务处理器");
        }
        log.info("开始分发任务，jobId={}, jobType={}", context.jobId(), context.jobType());
        return jobHandler.handle(context, payload);
    }
}
