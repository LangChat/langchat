package cn.langchat.claw.core.runtime.rag;

import ai.docling.serve.api.DoclingServeApi;
import cn.langchat.claw.core.runtime.config.DoclingParseProperties;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.parser.docling.DoclingDocumentParser;
import java.io.InputStream;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Docling 文档解析服务。
 *
 * <p>基于 langchain4j 的 Docling 集成，将文件提交给独立部署的 docling-serve 完成
 * 版面分析与结构还原，返回 Markdown 文本。相比内置 Tika 解析，Docling 对
 * 复杂 PDF（多栏、表格、扫描件）的还原质量更高。</p>
 *
 * <p>服务未启用或调用失败时返回 {@code null}，由调用方回退到内置解析链路，
 * 保证文档导入不会因外部服务不可用而中断。</p>
 *
 * @author LangChat Team
 * @since 2026/9/26
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DoclingParseService {

    private static final String DOCLING_PARSER = "docling-parser";

    private final DoclingParseProperties doclingParseProperties;

    /**
     * Docling 解析是否可用：显式启用且配置了服务地址。
     */
    public boolean isAvailable() {
        return Boolean.TRUE.equals(doclingParseProperties.getEnabled())
                && StringUtils.hasText(doclingParseProperties.getBaseUrl());
    }

    /**
     * 解析器标识，写入解析结果用于溯源。
     */
    public String parserName() {
        return DOCLING_PARSER;
    }

    /**
     * 使用 Docling 解析文档为 Markdown 文本。
     *
     * @param inputStream 文档输入流
     * @return Markdown 文本；服务不可用或解析失败时返回 {@code null}
     */
    public String parseToMarkdown(InputStream inputStream) {
        if (!isAvailable()) {
            return null;
        }
        try {
            Document document = buildParser().parse(inputStream);
            String text = document.text();
            if (text == null || text.isBlank()) {
                log.warn("Docling 解析结果为空，baseUrl={}", doclingParseProperties.getBaseUrl());
                return null;
            }
            log.info("Docling 解析完成，textLength={}", text.length());
            return text;
        } catch (Exception exception) {
            log.warn("Docling 解析失败，将回退内置解析链路，error={}", exception.getMessage());
            return null;
        }
    }

    private DoclingDocumentParser buildParser() {
        Duration timeout = Duration.ofSeconds(resolveTimeoutSeconds());
        DoclingServeApi client = DoclingServeApi.builder()
                .baseUrl(doclingParseProperties.getBaseUrl())
                .connectTimeout(timeout)
                .readTimeout(timeout)
                .build();
        return DoclingDocumentParser.builder()
                .doclingClient(client)
                .build();
    }

    private int resolveTimeoutSeconds() {
        Integer timeout = doclingParseProperties.getTimeoutSeconds();
        return timeout == null || timeout <= 0 ? 300 : timeout;
    }
}
