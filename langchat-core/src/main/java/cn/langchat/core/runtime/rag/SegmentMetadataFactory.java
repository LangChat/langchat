package cn.langchat.core.runtime.rag;

import cn.langchat.aigc.biz.entity.AigcDocs;
import cn.langchat.aigc.biz.entity.AigcKnowledge;
import cn.langchat.aigc.biz.entity.AigcSegment;
import dev.langchain4j.data.document.Metadata;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * 分段向量元数据工厂。
 *
 * <p>整文档向量化与单分段重新向量化必须产出完全一致的元数据，
 * 检索阶段才能按知识库、文档、分段正确归属命中结果。</p>
 *
 * @author LangChat Team
 * @since 2026/9/27
 */
@Component
public class SegmentMetadataFactory {

    /**
     * 构建分段写入向量库时携带的元数据。
     */
    public Metadata build(AigcKnowledge knowledge, AigcDocs docs, AigcSegment segment) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("knowledgeId", knowledge.getId());
        metadata.put("knowledgeName", knowledge.getName());
        metadata.put("docsId", docs.getId());
        metadata.put("docsName", docs.getName());
        metadata.put("segmentId", segment.getId());
        metadata.put("position", segment.getPosition());
        return Metadata.from(metadata);
    }
}
