package cn.langchat.claw.core.runtime.rag;

import cn.langchat.claw.aigc.biz.entity.AigcKnowledge;
import cn.langchat.claw.core.chat.model.knowledge.KnowledgeSearchHit;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.query.Query;
import java.util.List;

/**
 * 基于切片表的关键词检索器。
 *
 * @author LangChat Team
 * @since 2026/3/25
 */
public class KeywordSegmentContentRetriever implements ContentRetriever {

    private final KnowledgeSearchService knowledgeSearchService;
    private final List<AigcKnowledge> knowledges;
    private final int topK;

    public KeywordSegmentContentRetriever(
            KnowledgeSearchService knowledgeSearchService,
            List<AigcKnowledge> knowledges,
            int topK
    ) {
        this.knowledgeSearchService = knowledgeSearchService;
        this.knowledges = knowledges;
        this.topK = topK;
    }

    @Override
    public List<Content> retrieve(Query query) {
        return knowledgeSearchService.search(knowledges, query.text(), topK).stream()
                .map(this::toContent)
                .toList();
    }

    private Content toContent(KnowledgeSearchHit hit) {
        Metadata metadata = Metadata.from(hit.getMetadata() == null ? java.util.Map.of() : hit.getMetadata());
        return Content.from(TextSegment.from(hit.getContent(), metadata));
    }
}
