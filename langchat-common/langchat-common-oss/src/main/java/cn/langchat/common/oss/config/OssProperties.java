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

    /** 存储类型：local 或 s3。 */
    private String type = "local";

    /** 本地存储根目录。 */
    private String localBaseDir = System.getProperty("java.io.tmpdir") + "/langchat-oss";

    /** S3 兼容对象存储配置。 */
    private S3 s3 = new S3();

    @Data
    public static class S3 {

        /** S3 API 地址，例如 http://rustfs:9000，末尾不要添加斜杠。 */
        private String endPoint;

        /** 访问密钥。 */
        private String accessKey;

        /** 密钥。 */
        private String secretKey;

        /** 对象桶名称。 */
        private String bucketName = "langchat";

        /** 区域标识；S3 兼容服务通常可使用 us-east-1。 */
        private String region = "us-east-1";

        /** 桶不存在时是否自动创建。 */
        private boolean autoCreateBucket = true;

        /** 是否使用 path-style 地址，S3 兼容服务通常需要开启。 */
        private boolean pathStyleAccessEnabled = true;

        /** 可选的公开访问基础地址；为空时返回 s3:// URI。 */
        private String publicBaseUrl;
    }
}
