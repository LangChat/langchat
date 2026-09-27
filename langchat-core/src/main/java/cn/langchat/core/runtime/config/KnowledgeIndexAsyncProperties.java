package cn.langchat.core.runtime.config;

import java.time.Duration;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 知识库异步索引线程池配置。
 *
 * @author LangChat Team
 * @since 2026/3/25
 */
@Data
@Component
@ConfigurationProperties(prefix = "langchat.core.knowledge.index")
public class KnowledgeIndexAsyncProperties {

    /** 核心线程数。 */
    private Integer corePoolSize = 4;

    /** 最大线程数。 */
    private Integer maxPoolSize = 8;

    /** 队列容量。 */
    private Integer queueCapacity = 200;

    /** 线程空闲存活时间，单位秒。 */
    private Integer keepAliveSeconds = 60;

    /** 线程名前缀。 */
    private String threadNamePrefix = "langchat-doc-index-";

    /** 单文档向量化任务最大执行时间。 */
    private Duration taskTimeout = Duration.ofMinutes(30);

    /** 超时任务后台扫描间隔。 */
    private Duration staleTaskSweepInterval = Duration.ofMinutes(1);
}
