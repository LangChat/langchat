package cn.langchat.monitor.config;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * 模型调用日志线程池配置。
 *
 * <p>监控写入必须与主业务隔离：模型调用发生在对话与向量化等关键路径上，
 * 独立线程池可避免监控写库与业务线程互相争抢。</p>
 *
 * @author LangChat Team
 * @since 2026/9/26
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class ModelCallLogConfiguration {

    /** 线程池 Bean 名称。 */
    public static final String EXECUTOR_NAME = "modelCallLogExecutor";

    private final ModelCallLogProperties properties;

    @Bean(EXECUTOR_NAME)
    public Executor modelCallLogExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(properties.getCorePoolSize());
        executor.setMaxPoolSize(properties.getMaxPoolSize());
        executor.setQueueCapacity(properties.getQueueCapacity());
        executor.setKeepAliveSeconds(properties.getKeepAliveSeconds());
        executor.setThreadNamePrefix(properties.getThreadNamePrefix());
        // 队列满时由调用线程兜底执行，保证监控记录不丢失
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(Boolean.TRUE);
        executor.setAwaitTerminationSeconds(properties.getKeepAliveSeconds());
        executor.initialize();
        log.info("初始化模型调用日志线程池，corePoolSize={}, maxPoolSize={}, queueCapacity={}",
                properties.getCorePoolSize(), properties.getMaxPoolSize(), properties.getQueueCapacity());
        return executor;
    }
}
