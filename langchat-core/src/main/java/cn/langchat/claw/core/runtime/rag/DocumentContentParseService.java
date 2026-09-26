package cn.langchat.claw.core.runtime.rag;

import cn.langchat.claw.aigc.biz.entity.AigcDocs;
import cn.langchat.claw.aigc.biz.entity.AigcOss;
import cn.langchat.claw.aigc.biz.service.AigcOssService;
import cn.langchat.claw.common.exception.BizException;
import cn.langchat.claw.core.chat.enums.KnowledgeDocumentTypeEnum;
import cn.langchat.claw.core.chat.enums.KnowledgeFileTypeEnum;
import cn.langchat.claw.core.chat.model.parse.ParsedDocumentContent;
import cn.langchat.claw.core.support.CoreErrorCode;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.tika.Tika;
import org.springframework.stereotype.Component;

/**
 * 文档内容解析服务。
 *
 * @author LangChat Team
 * @since 2026/3/25
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DocumentContentParseService {

    private static final TypeReference<List<Map<String, Object>>> QA_LIST_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };
    private static final String QA_QUESTION_KEY = "question";
    private static final String QA_ANSWER_KEY = "answer";
    private static final String DATA_URI_PREFIX = "data:";
    private static final String DATA_BASE64_FLAG = ";base64";
    private static final String FILE_URI_SCHEME = "file:";
    private static final String HTTP_URI_SCHEME = "http://";
    private static final String HTTPS_URI_SCHEME = "https://";
    private static final String PARSER_QA = "qa-parser";
    private static final String PARSER_MARKDOWN = "markdown-parser";
    private static final String PARSER_TEXT = "text-parser";
    private static final String PARSER_CONTENT = "content-parser";
    private static final String PARSER_TIKA = "tika-parser";
    private static final String PARSER_PDFBOX = "pdfbox-parser";

    private final AigcOssService aigcOssService;
    private final ObjectMapper objectMapper;
    private final Tika tika = new Tika();

    /**
     * 解析知识文档内容。
     */
    public ParsedDocumentContent parse(AigcDocs docs) {
        KnowledgeDocumentTypeEnum documentType = resolveDocumentType(docs);
        ParsedDocumentContent result = switch (documentType) {
            case QA -> parseQaDocument(docs);
            case TEXT, MARKDOWN -> parsePlainTextDocument(docs, documentType);
            case FILE, UNKNOWN -> parseFileDocument(docs);
        };
        log.info("完成文档解析，docsId={}, parserName={}, sectionSize={}", docs.getId(), result.getParserName(), result.getSections().size());
        return result;
    }

    private ParsedDocumentContent parseQaDocument(AigcDocs docs) {
        String rawContent = resolveRawContent(docs);
        List<String> sections = parseQaSections(rawContent);
        return buildResult(docs, sections, PARSER_QA);
    }

    private ParsedDocumentContent parsePlainTextDocument(AigcDocs docs, KnowledgeDocumentTypeEnum documentType) {
        String rawContent = resolveRawContent(docs);
        List<String> sections = List.of(normalizeText(rawContent));
        return buildResult(docs, sections, documentType == KnowledgeDocumentTypeEnum.MARKDOWN ? PARSER_MARKDOWN : PARSER_TEXT);
    }

    private ParsedDocumentContent parseFileDocument(AigcDocs docs) {
        String rawContent = resolveRawContent(docs);
        if (rawContent != null && !rawContent.isBlank()) {
            return buildResult(docs, List.of(normalizeText(rawContent)), PARSER_CONTENT);
        }
        AigcOss oss = loadOss(docs);
        ParsedFileContent parsedFileContent = parseFileText(docs, oss);
        return buildResult(docs, List.of(normalizeText(parsedFileContent.content())), parsedFileContent.parserName());
    }

    private ParsedDocumentContent buildResult(AigcDocs docs, List<String> sections, String parserName) {
        List<String> normalizedSections = sections.stream()
                .map(this::normalizeText)
                .filter(section -> !section.isBlank())
                .toList();
        ParsedDocumentContent result = new ParsedDocumentContent();
        result.setDocsId(docs.getId());
        result.setTitle(docs.getName());
        result.setSections(normalizedSections);
        result.setPlainText(String.join("\n\n", normalizedSections));
        result.setParserName(parserName);
        return result;
    }

    private String resolveRawContent(AigcDocs docs) {
        if (docs.getContent() != null && !docs.getContent().isBlank()) {
            return decodeDataUriIfNecessary(docs.getContent());
        }
        if (docs.getUrl() != null && docs.getUrl().startsWith(DATA_URI_PREFIX)) {
            return decodeDataUriIfNecessary(docs.getUrl());
        }
        if (docs.getPdfUrl() != null && docs.getPdfUrl().startsWith(DATA_URI_PREFIX)) {
            return decodeDataUriIfNecessary(docs.getPdfUrl());
        }
        return "";
    }

    private KnowledgeDocumentTypeEnum resolveDocumentType(AigcDocs docs) {
        KnowledgeDocumentTypeEnum type = KnowledgeDocumentTypeEnum.fromCode(docs.getType());
        if (type != KnowledgeDocumentTypeEnum.UNKNOWN) {
            return type;
        }
        KnowledgeFileTypeEnum fileType = KnowledgeFileTypeEnum.fromExtension(docs.getExt());
        return switch (fileType) {
            case MARKDOWN -> KnowledgeDocumentTypeEnum.MARKDOWN;
            case TEXT -> KnowledgeDocumentTypeEnum.TEXT;
            case PDF, WORD, EXCEL, PPT -> KnowledgeDocumentTypeEnum.FILE;
            case UNKNOWN -> KnowledgeDocumentTypeEnum.UNKNOWN;
        };
    }

    private List<String> parseQaSections(String rawContent) {
        if (rawContent == null || rawContent.isBlank()) {
            return List.of();
        }
        List<String> sections = parseQaJson(rawContent);
        if (!sections.isEmpty()) {
            return sections;
        }
        return parseQaLines(rawContent);
    }

    private List<String> parseQaJson(String rawContent) {
        try {
            List<Map<String, Object>> rows = objectMapper.readValue(rawContent, QA_LIST_TYPE);
            return buildQaSections(rows);
        } catch (Exception ex) {
            try {
                Map<String, Object> wrapper = objectMapper.readValue(rawContent, MAP_TYPE);
                Object items = wrapper.getOrDefault("items", wrapper.getOrDefault("data", wrapper.get("list")));
                if (items instanceof List<?> rawList) {
                    List<Map<String, Object>> rows = new ArrayList<>();
                    for (Object item : rawList) {
                        if (item instanceof Map<?, ?> itemMap) {
                            Map<String, Object> row = new java.util.LinkedHashMap<>();
                            itemMap.forEach((key, value) -> row.put(String.valueOf(key), value));
                            rows.add(row);
                        }
                    }
                    return buildQaSections(rows);
                }
                return Collections.emptyList();
            } catch (Exception nestedEx) {
                return Collections.emptyList();
            }
        }
    }

    private List<String> parseQaLines(String rawContent) {
        List<String> sections = new ArrayList<>();
        String[] lines = normalizeText(rawContent).split("\n");
        String question = null;
        StringBuilder answer = new StringBuilder();
        for (String line : lines) {
            String trimmed = line.trim();
            if (isQuestionLine(trimmed)) {
                if (question != null && answer.length() > 0) {
                    sections.add(formatQa(question, answer.toString()));
                }
                question = trimQaPrefix(trimmed);
                answer = new StringBuilder();
                continue;
            }
            if (isAnswerLine(trimmed)) {
                if (answer.length() > 0) {
                    answer.append('\n');
                }
                answer.append(trimAaPrefix(trimmed));
                continue;
            }
            if (answer.length() > 0) {
                answer.append('\n').append(trimmed);
            }
        }
        if (question != null && answer.length() > 0) {
            sections.add(formatQa(question, answer.toString()));
        }
        return sections.isEmpty() ? List.of(normalizeText(rawContent)) : sections;
    }

    private List<String> buildQaSections(List<Map<String, Object>> rows) {
        List<String> sections = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            String question = firstNonBlank(
                    stringValue(row.get(QA_QUESTION_KEY)),
                    stringValue(row.get("q")),
                    stringValue(row.get("title")),
                    stringValue(row.get("ask"))
            );
            String answer = firstNonBlank(
                    stringValue(row.get(QA_ANSWER_KEY)),
                    stringValue(row.get("a")),
                    stringValue(row.get("content")),
                    stringValue(row.get("reply"))
            );
            if (question != null && answer != null) {
                sections.add(formatQa(question, answer));
            }
        }
        return sections;
    }

    private ParsedFileContent parseFileText(AigcDocs docs, AigcOss oss) {
        String source = resolveFileSource(docs, oss);
        KnowledgeFileTypeEnum fileType = KnowledgeFileTypeEnum.fromExtension(firstNonBlank(docs.getExt(), oss == null ? null : oss.getExt()));
        try {
            log.info("开始解析文件文档，docsId={}, fileType={}, source={}", docs.getId(), fileType, source);
            return switch (fileType) {
                case PDF -> parsePdf(source);
                case MARKDOWN -> new ParsedFileContent(parsePlainTextFile(source), PARSER_MARKDOWN);
                case TEXT -> new ParsedFileContent(parsePlainTextFile(source), PARSER_TEXT);
                case WORD, EXCEL, PPT, UNKNOWN -> new ParsedFileContent(parseWithTika(source), PARSER_TIKA);
            };
        } catch (Exception ex) {
            log.warn("Tika/PDF 解析失败，docsId={}, source={}", docs.getId(), source, ex);
            throw new IllegalStateException("文档解析失败: " + ex.getMessage(), ex);
        }
    }

    private String parseWithTika(String source) throws Exception {
        try (InputStream inputStream = openSourceStream(source)) {
            return tika.parseToString(inputStream);
        }
    }

    private ParsedFileContent parsePdf(String source) throws Exception {
        try {
            return new ParsedFileContent(parseWithTika(source), PARSER_TIKA);
        } catch (Exception ex) {
            try (PDDocument document = loadPdfDocument(source)) {
                return new ParsedFileContent(new PDFTextStripper().getText(document), PARSER_PDFBOX);
            }
        }
    }

    private String parsePlainTextFile(String source) throws Exception {
        try (InputStream inputStream = openSourceStream(source)) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private AigcOss loadOss(AigcDocs docs) {
        if (docs.getOssId() == null || docs.getOssId().isBlank()) {
            return null;
        }
        return aigcOssService.getById(docs.getOssId());
    }

    private String resolveFileSource(AigcDocs docs, AigcOss oss) {
        String source = firstNonBlank(
                oss == null ? null : oss.getPath(),
                oss == null ? null : oss.getUrl(),
                docs.getUrl(),
                docs.getPdfUrl()
        );
        if (source == null || source.isBlank()) {
            throw new BizException(CoreErrorCode.KNOWLEDGE_NOT_FOUND.code(), "文档缺少可解析的文件路径");
        }
        return source;
    }

    private String formatQa(String question, String answer) {
        return "问题：" + normalizeText(question) + "\n答案：" + normalizeText(answer);
    }

    private String trimQaPrefix(String value) {
        return value.replaceFirst("^(Q:|Q：|问题:|问题：|问:|问：)", "").trim();
    }

    private String trimAaPrefix(String value) {
        return value.replaceFirst("^(A:|A：|答案:|答案：|答:|答：)", "").trim();
    }

    private boolean isQuestionLine(String value) {
        return value.startsWith("Q:") || value.startsWith("Q：")
                || value.startsWith("问题:") || value.startsWith("问题：")
                || value.startsWith("问:") || value.startsWith("问：");
    }

    private boolean isAnswerLine(String value) {
        return value.startsWith("A:") || value.startsWith("A：")
                || value.startsWith("答案:") || value.startsWith("答案：")
                || value.startsWith("答:") || value.startsWith("答：");
    }

    private String decodeDataUriIfNecessary(String value) {
        if (value == null || !value.startsWith(DATA_URI_PREFIX)) {
            return value;
        }
        int commaIndex = value.indexOf(',');
        if (commaIndex < 0) {
            return value;
        }
        String metadata = value.substring(0, commaIndex);
        String payload = value.substring(commaIndex + 1);
        if (metadata.contains(DATA_BASE64_FLAG)) {
            return new String(Base64.getDecoder().decode(payload), StandardCharsets.UTF_8);
        }
        return URLDecoder.decode(payload, StandardCharsets.UTF_8);
    }

    private InputStream openSourceStream(String source) throws Exception {
        if (source.startsWith(FILE_URI_SCHEME)) {
            return Files.newInputStream(Path.of(URI.create(source)));
        }
        if (source.startsWith(HTTP_URI_SCHEME) || source.startsWith(HTTPS_URI_SCHEME)) {
            return URI.create(source).toURL().openStream();
        }
        return Files.newInputStream(Path.of(source));
    }

    private PDDocument loadPdfDocument(String source) throws Exception {
        if (source.startsWith(FILE_URI_SCHEME)) {
            return PDDocument.load(Path.of(URI.create(source)).toFile());
        }
        if (source.startsWith(HTTP_URI_SCHEME) || source.startsWith(HTTPS_URI_SCHEME)) {
            try (InputStream inputStream = openSourceStream(source)) {
                return PDDocument.load(inputStream.readAllBytes());
            }
        }
        Path path = Path.of(source);
        if (Files.exists(path)) {
            return PDDocument.load(path.toFile());
        }
        try (InputStream inputStream = openSourceStream(source)) {
            return PDDocument.load(inputStream.readAllBytes());
        }
    }

    private String normalizeText(String content) {
        if (content == null) {
            return "";
        }
        return content.replace("\r\n", "\n")
                .replace('\r', '\n')
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    /**
     * 文件解析结果。
     */
    private record ParsedFileContent(String content, String parserName) {
    }
}
