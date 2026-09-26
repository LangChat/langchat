package cn.langchat.claw.core.runtime.rag;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * 文档分段服务。
 *
 * @author LangChat Team
 * @since 2026/3/25
 */
@Component
public class DocumentChunkingService {

    private static final int DEFAULT_CHUNK_SIZE = 800;
    private static final int DEFAULT_OVERLAP_SIZE = 120;

    /**
     * 对文本执行滑动窗口切片。
     */
    public List<String> chunk(String content, Integer chunkSize, Integer overlapSize) {
        String normalized = normalize(content);
        if (normalized.isBlank()) {
            return List.of();
        }
        int size = chunkSize == null || chunkSize <= 0 ? DEFAULT_CHUNK_SIZE : chunkSize;
        int overlap = overlapSize == null || overlapSize < 0 ? DEFAULT_OVERLAP_SIZE : overlapSize;
        int step = Math.max(1, size - overlap);
        List<String> segments = new ArrayList<>();
        for (int start = 0; start < normalized.length(); start += step) {
            int end = Math.min(normalized.length(), start + size);
            String segment = normalized.substring(start, end).trim();
            if (!segment.isBlank()) {
                segments.add(segment);
            }
            if (end >= normalized.length()) {
                break;
            }
        }
        return segments;
    }

    private String normalize(String content) {
        if (content == null) {
            return "";
        }
        return content.replace("\r\n", "\n")
                .replace('\r', '\n')
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }
}
