package cn.langchat.common.oss.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * OSS 配置属性。
 *
 * @author LangChat Team
 * @since 2026/3/24
 */
@Data
@Component
@ConfigurationProperties(prefix = "langchat.storage")
public class OssProperties {

    /** 本地存储根目录。 */
    private String localBaseDir = System.getProperty("java.io.tmpdir") + "/langchat-oss";
}
