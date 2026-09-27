package cn.langchat.aigc.biz.support;

import cn.langchat.aigc.biz.entity.AigcModel;
import java.util.Map;

/**
 * 模型扩展配置读取规则。
 *
 * <p>模型类型差异化参数统一保存在 {@code config_json}，运行时必须通过该类读取，
 * 避免各供应商实现分别处理 JSON 数值类型和字符串输入。</p>
 *
 * @author LangChat Team
 * @since 2026/9/27
 */
public final class ModelConfigSupport {

    public static final String DIMENSION = "dimension";

    private ModelConfigSupport() {}

    /**
     * 获取向量模型输出维度，未配置或非法时返回 {@code null}。
     */
    public static Integer resolveDimension(AigcModel model) {
        return positiveInteger(config(model).get(DIMENSION));
    }

    private static Map<String, Object> config(AigcModel model) {
        if (model == null || model.getConfigJson() == null) {
            return Map.of();
        }
        return model.getConfigJson();
    }

    private static Integer positiveInteger(Object value) {
        Integer result = null;
        if (value instanceof Number number) {
            result = number.intValue();
        } else if (value instanceof String stringValue && !stringValue.isBlank()) {
            try {
                result = Integer.valueOf(stringValue.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return result != null && result > 0 ? result : null;
    }
}
