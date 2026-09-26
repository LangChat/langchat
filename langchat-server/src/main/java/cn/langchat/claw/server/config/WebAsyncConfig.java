package cn.langchat.claw.server.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring MVC 异步配置。
 *
 * <p>聊天补全等 SSE 接口基于 Spring MVC 异步请求处理，需指定专用线程池，
 * 避免使用默认的 SimpleAsyncTaskExecutor（每次新建线程，不适合生产环境）。
 *
 * @author LangChat Team
 * @since 2026/9/26
 */
@Configuration
public class WebAsyncConfig implements WebMvcConfigurer {

    @Override
    public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(8);
        executor.setMaxPoolSize(32);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("mvc-async-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.initialize();
        // SSE 长连接（流式对话超时上限 3 分钟），默认超时放宽到 5 分钟
        configurer.setDefaultTimeout(300_000L);
        configurer.setTaskExecutor(executor);
    }
}
