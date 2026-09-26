package cn.langchat.monitor.model;

/**
 * 模型调用元信息。
 *
 * <p>由模型工厂在构建模型客户端时绑定，使每个模型实例产生的调用都能追溯到
 * 具体的模型配置与调用场景。</p>
 *
 * @param modelId 模型配置 ID
 * @param modelName 模型名称
 * @param provider 供应商
 * @param scene 调用场景
 * @author LangChat Team
 * @since 2026/9/26
 */
public record ModelCallMetadata(
        String modelId,
        String modelName,
        String provider,
        String scene
) {

    /**
     * 构建元信息，空值统一归一化为 {@code unknown}。
     */
    public static ModelCallMetadata of(String modelId, String modelName, String provider, String scene) {
        return new ModelCallMetadata(
                defaultIfBlank(modelId),
                defaultIfBlank(modelName),
                defaultIfBlank(provider),
                defaultIfBlank(scene)
        );
    }

    /**
     * 替换调用场景，用于同一模型在不同场景下的区分。
     */
    public ModelCallMetadata withScene(String newScene) {
        return new ModelCallMetadata(modelId, modelName, provider, defaultIfBlank(newScene));
    }

    private static String defaultIfBlank(String value) {
        return value == null || value.isBlank() ? "unknown" : value.trim();
    }
}
