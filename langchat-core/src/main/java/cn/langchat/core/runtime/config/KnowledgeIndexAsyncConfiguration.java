package cn.langchat.core.runtime.config;

import java.util.concurrent.ThreadPoolExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * 知识库异步索引线程池配置。
 *
 * @author LangChat Team
 * @since 2026/3/25
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class KnowledgeIndexAsyncConfiguration {

    private final KnowledgeIndexAsyncProperties properties;

    /**
     * 文档向量化异步线程池。
     */
    @Bean(KnowledgeIndexExecutorNames.DOCUMENT_INDEX_TASK_EXECUTOR)
    public ThreadPoolTaskExecutor documentIndexTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(properties.getCorePoolSize());
        executor.setMaxPoolSize(properties.getMaxPoolSize());
        executor.setQueueCapacity(properties.getQueueCapacity());
        executor.setKeepAliveSeconds(properties.getKeepAliveSeconds());
        executor.setThreadNamePrefix(properties.getThreadNamePrefix());
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(Boolean.TRUE);
        executor.setAwaitTerminationSeconds(properties.getKeepAliveSeconds());
        executor.initialize();
        log.info(
                "初始化知识库异步索引线程池，corePoolSize={}, maxPoolSize={}, queueCapacity={}",
                properties.getCorePoolSize(),
                properties.getMaxPoolSize(),
                properties.getQueueCapacity()
        );
        return executor;
    }
}
