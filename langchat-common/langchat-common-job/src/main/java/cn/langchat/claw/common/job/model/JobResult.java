package cn.langchat.claw.common.job.model;

/**
 * 任务执行结果。
 *
 * @param success 是否成功
 * @param message 结果消息
 * @author LangChat Team
 * @since 2026/3/24
 */
public record JobResult(
        boolean success,
        String message
) {

    /**
     * 构建成功结果。
     */
    public static JobResult success(String message) {
        return new JobResult(true, message);
    }

    /**
     * 构建失败结果。
     */
    public static JobResult failure(String message) {
        return new JobResult(false, message);
    }
}
