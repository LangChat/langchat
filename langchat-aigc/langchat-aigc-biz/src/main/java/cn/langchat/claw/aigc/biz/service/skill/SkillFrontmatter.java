package cn.langchat.claw.aigc.biz.service.skill;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;

/**
 * SKILL.md frontmatter 解析结果。
 *
 * @author LangChat Team
 * @since 2026/9/26
 */
@Data
@Builder
public class SkillFrontmatter {

    private static final String DELIMITER = "---";

    /** 技能标识（必填）。 */
    private String name;

    /** 技能标题。 */
    private String title;

    /** 技能描述。 */
    private String description;

    /** 版本。 */
    private String version;

    /** 开源协议。 */
    private String license;

    /** 标签，逗号分隔。 */
    private String tags;

    /** 是否解析到 frontmatter 块。 */
    private boolean present;

    /**
     * 解析 SKILL.md 的 YAML frontmatter。仅支持扁平 key: value 与简单列表。
     */
    public static SkillFrontmatter parse(String markdown) {
        if (markdown == null) {
            return SkillFrontmatter.builder().build();
        }
        List<String> lines = markdown.lines().toList();
        int index = 0;
        while (index < lines.size() && lines.get(index).isBlank()) {
            index++;
        }
        if (index >= lines.size() || !DELIMITER.equals(lines.get(index).trim())) {
            return SkillFrontmatter.builder().build();
        }
        index++;
        Map<String, String> values = new LinkedHashMap<>();
        String lastKey = null;
        while (index < lines.size() && !DELIMITER.equals(lines.get(index).trim())) {
            String line = lines.get(index);
            index++;
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            if ((trimmed.startsWith("- ") || trimmed.startsWith("-\t")) && lastKey != null) {
                values.merge(lastKey, trimmed.substring(2).trim(), (old, add) -> old + "," + add);
                continue;
            }
            int separator = trimmed.indexOf(':');
            if (separator <= 0) {
                continue;
            }
            String key = trimmed.substring(0, separator).trim().toLowerCase();
            String value = stripQuotes(trimmed.substring(separator + 1).trim());
            if (value.startsWith("[") && value.endsWith("]")) {
                value = value.substring(1, value.length() - 1)
                        .replace("\"", "")
                        .replace("'", "")
                        .trim();
            }
            values.put(key, value);
            lastKey = key;
        }
        return SkillFrontmatter.builder()
                .present(true)
                .name(values.get("name"))
                .title(values.get("title"))
                .description(values.get("description"))
                .version(values.get("version"))
                .license(values.get("license"))
                .tags(values.get("tags"))
                .build();
    }

    /**
     * 去掉 frontmatter 块，返回 Markdown 正文。
     */
    public static String stripFrontmatter(String markdown) {
        if (markdown == null) {
            return "";
        }
        List<String> lines = markdown.lines().toList();
        int index = 0;
        while (index < lines.size() && lines.get(index).isBlank()) {
            index++;
        }
        if (index >= lines.size() || !DELIMITER.equals(lines.get(index).trim())) {
            return markdown;
        }
        index++;
        int start = -1;
        while (index < lines.size()) {
            if (DELIMITER.equals(lines.get(index).trim())) {
                start = index + 1;
                break;
            }
            index++;
        }
        if (start < 0) {
            return markdown;
        }
        List<String> body = new ArrayList<>(lines.subList(start, lines.size()));
        while (!body.isEmpty() && body.get(0).isBlank()) {
            body.remove(0);
        }
        return String.join("\n", body);
    }

    private static String stripQuotes(String value) {
        if (value.length() >= 2) {
            char first = value.charAt(0);
            char last = value.charAt(value.length() - 1);
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                return value.substring(1, value.length() - 1).trim();
            }
        }
        return value;
    }
}
