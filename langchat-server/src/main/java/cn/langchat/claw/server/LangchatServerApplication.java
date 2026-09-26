package cn.langchat.claw.server;

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
@SpringBootApplication(scanBasePackages = "cn.langchat.claw")
@ConfigurationPropertiesScan(basePackages = "cn.langchat.claw")
@MapperScan(basePackages = "cn.langchat.claw", annotationClass = Mapper.class)
@EnableAsync
public class LangchatServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(LangchatServerApplication.class, args);
    }
}
