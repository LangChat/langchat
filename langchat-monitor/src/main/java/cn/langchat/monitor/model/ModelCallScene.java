package cn.langchat.monitor.model;

/**
 * 模型调用场景常量。
 *
 * <p>用于区分同一模型在不同业务链路下的调用，报表按场景维度聚合消耗。</p>
 *
 * @author LangChat Team
 * @since 2026/9/26
 */
public final class ModelCallScene {

    /** Agent 对话。 */
    public static final String AGENT_CHAT = "AGENT_CHAT";

    /** 智能问数。 */
    public static final String DATA_ANALYSIS = "DATA_ANALYSIS";

    /** 知识库向量化。 */
    public static final String KNOWLEDGE_INDEX = "KNOWLEDGE_INDEX";

    /** 知识库检索召回。 */
    public static final String KNOWLEDGE_RETRIEVE = "KNOWLEDGE_RETRIEVE";

    /** 文生图。 */
    public static final String IMAGE_GENERATE = "IMAGE_GENERATE";

    /** 图像识别。 */
    public static final String IMAGE_OCR = "IMAGE_OCR";

    private ModelCallScene() {
    }
}
