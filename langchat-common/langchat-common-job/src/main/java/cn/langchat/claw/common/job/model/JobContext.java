package cn.langchat.claw.common.job.model;

/**
 * 任务执行上下文。
 *
 * @param jobId 任务ID
 * @param jobType 任务类型
 * @param operator 操作人
 * @author LangChat Team
 * @since 2026/3/24
 */
public record JobContext(
        String jobId,
        String jobType,
        String operator
) {
}
