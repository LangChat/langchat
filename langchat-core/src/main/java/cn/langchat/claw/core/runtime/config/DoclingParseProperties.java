package cn.langchat.claw.core.runtime.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Docling 解析配置。
 *
 * <p>Docling 是独立部署的文档解析服务（docling-serve），通过 REST 调用完成
 * 版面分析、表格还原与 Markdown 输出，适合对解析质量要求较高的 PDF/Office 文档。
 * 未配置 {@code base-url} 时视为未启用，文档导入仍走内置的 Tika/PDFBox 解析链路。</p>
 *
 * @author LangChat Team
 * @since 2026/9/26
 */
@Data
@Component
@ConfigurationProperties(prefix = "langchat.core.docling")
public class DoclingParseProperties {

    /** 是否启用 Docling 解析。 */
    private Boolean enabled = false;

    /** docling-serve 服务地址，例如 {@code http://127.0.0.1:5001}。 */
    private String baseUrl;

    /** 单次解析超时时间，单位秒。 */
    private Integer timeoutSeconds = 300;
}
