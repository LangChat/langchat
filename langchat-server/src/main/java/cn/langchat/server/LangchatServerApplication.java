package cn.langchat.server;

import org.apache.ibatis.annotations.Mapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 服务端启动入口。
 *
 * @author LangChat Team
 * @since 2026/3/24
 */
@SpringBootApplication(scanBasePackages = "cn.langchat")
@ConfigurationPropertiesScan(basePackages = "cn.langchat")
@MapperScan(basePackages = "cn.langchat", annotationClass = Mapper.class)
@EnableAsync
public class LangchatServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(LangchatServerApplication.class, args);
    }
}
