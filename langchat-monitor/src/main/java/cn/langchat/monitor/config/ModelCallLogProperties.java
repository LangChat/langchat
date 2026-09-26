package cn.langchat.monitor.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 模型调用日志线程池配置。
 *
 * @author LangChat Team
 * @since 2026/9/26
 */
@Data
@Component
@ConfigurationProperties(prefix = "langchat.monitor.model-call-log")
public class ModelCallLogProperties {

    /** 核心线程数。 */
    private Integer corePoolSize = 1;

    /** 最大线程数。 */
    private Integer maxPoolSize = 2;

    /** 队列容量。 */
    private Integer queueCapacity = 500;

    /** 线程空闲存活时间，单位秒。 */
    private Integer keepAliveSeconds = 60;

    /** 线程名前缀。 */
    private String threadNamePrefix = "langchat-model-call-log-";
}
