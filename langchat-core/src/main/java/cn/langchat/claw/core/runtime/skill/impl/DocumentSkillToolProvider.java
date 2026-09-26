package cn.langchat.claw.core.runtime.skill.impl;

import cn.langchat.claw.aigc.biz.entity.AigcSkill;
import cn.langchat.claw.aigc.biz.service.SkillPackageService;
import cn.langchat.claw.core.runtime.skill.AgentToolContext;
import cn.langchat.claw.core.runtime.skill.AgentToolDefinition;
import cn.langchat.claw.core.runtime.skill.AgentToolProvider;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 标准技能包工具提供器。
 *
 * <p>技能包以 SKILL.md 为入口（frontmatter 声明 name/description，正文为标准化的技能指令文档），
 * 运行时基于 LangChain4j 官方 Tool 机制动态注册：把每个启用的技能暴露为一个工具，
 * 模型调用工具后返回技能指令文档（可携带 path 读取包内 references 等附加文档），
 * 由模型按照文档指令继续执行，实现技能文档的动态调用。</p>
 *
 * @author LangChat Team
 * @since 2026/9/26
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DocumentSkillToolProvider implements AgentToolProvider {

    private static final Pattern INVALID_TOOL_NAME = Pattern.compile("[^a-zA-Z0-9_-]+");

    private final SkillPackageService skillPackageService;
    private final ObjectMapper objectMapper;

    @Override
    public boolean supports(AigcSkill skill) {
        return true;
    }

    @Override
    public AgentToolDefinition buildDefinition(AgentToolContext context) {
        AigcSkill skill = context.skill();
        ToolSpecification specification = ToolSpecification.builder()
                .name(resolveToolName(skill))
                .description(resolveDescription(skill))
                .parameters(buildParameters())
                .build();
        return new AgentToolDefinition(specification, (request, memoryId) -> execute(request, skill));
    }

    private String execute(ToolExecutionRequest request, AigcSkill skill) {
        String path = extractPath(request);
        log.info("执行技能文档工具，toolName={}, skillName={}, path={}", request.name(), skill.getName(), path);
        try {
            return skillPackageService.readSkillDocument(skill, path);
        } catch (Exception exception) {
            log.warn("技能文档读取失败，skillName={}, path={}", skill.getName(), path, exception);
            return "技能文档读取失败：" + exception.getMessage();
        }
    }

    private String extractPath(ToolExecutionRequest request) {
        String arguments = request.arguments();
        if (!StringUtils.hasText(arguments)) {
            return null;
        }
        try {
            JsonNode node = objectMapper.readTree(arguments.trim());
            String path = node.path("path").asText(null);
            return StringUtils.hasText(path) ? path : null;
        } catch (Exception exception) {
            log.warn("解析技能工具入参失败，arguments={}", arguments);
            return null;
        }
    }

    private String resolveToolName(AigcSkill skill) {
        String name = StringUtils.hasText(skill.getName()) ? skill.getName() : skill.getId();
        String sanitized = INVALID_TOOL_NAME.matcher(name.toLowerCase()).replaceAll("-");
        while (sanitized.startsWith("-")) {
            sanitized = sanitized.substring(1);
        }
        while (sanitized.endsWith("-")) {
            sanitized = sanitized.substring(0, sanitized.length() - 1);
        }
        if (!StringUtils.hasText(sanitized)) {
            sanitized = "skill-" + (skill.getId() == null ? "unknown" : skill.getId());
        }
        return sanitized.length() > 64 ? sanitized.substring(0, 64) : sanitized;
    }

    private String resolveDescription(AigcSkill skill) {
        String title = StringUtils.hasText(skill.getTitle()) ? skill.getTitle() : skill.getName();
        String description = StringUtils.hasText(skill.getDescription()) ? skill.getDescription() : "";
        String text = "技能文档：" + title + (description.isBlank() ? "" : " —— " + description)
                + "。调用后将返回该技能的标准指令文档，请按文档内容执行。";
        return text.length() > 1024 ? text.substring(0, 1021) + "..." : text;
    }

    private JsonObjectSchema buildParameters() {
        return JsonObjectSchema.builder()
                .description("技能文档读取参数")
                .addStringProperty("path",
                        "可选：技能包内相对路径（如 references/api.md）。留空返回 SKILL.md 主文档，传目录路径返回文件清单")
                .required(java.util.List.of())
                .additionalProperties(Boolean.TRUE)
                .build();
    }
}
