package cn.langchat.core.runtime;

import cn.langchat.aigc.biz.entity.AigcAgent;
import cn.langchat.aigc.biz.entity.AigcKnowledge;
import cn.langchat.aigc.biz.entity.AigcMcp;
import cn.langchat.aigc.biz.entity.AigcModel;
import cn.langchat.aigc.biz.entity.AigcSkill;
import cn.langchat.aigc.biz.service.AigcAgentService;
import cn.langchat.aigc.biz.service.AigcKnowledgeService;
import cn.langchat.aigc.biz.service.AigcMcpService;
import cn.langchat.aigc.biz.service.AigcModelService;
import cn.langchat.aigc.biz.service.AigcSkillService;
import cn.langchat.common.exception.BizException;
import cn.langchat.core.runtime.model.AgentRuntimeDefinition;
import cn.langchat.core.support.CoreErrorCode;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Agent 运行时配置加载器。
 *
 * @author LangChat Team
 * @since 2026/3/24
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AgentRuntimeLoader {

    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {
    };

    private final AigcAgentService aigcAgentService;
    private final AigcKnowledgeService aigcKnowledgeService;
    private final AigcSkillService aigcSkillService;
    private final AigcMcpService aigcMcpService;
    private final AigcModelService aigcModelService;
    private final ObjectMapper objectMapper;

    /**
     * 按 Agent ID 加载完整运行时配置。
     */
    public AgentRuntimeDefinition load(String agentId) {
        AigcAgent agent = aigcAgentService.getById(agentId);
        if (agent == null) {
            throw new BizException(CoreErrorCode.AGENT_NOT_FOUND);
        }

        AigcModel chatModel = loadChatModel(agent);
        List<AigcKnowledge> knowledges = loadKnowledges(agent);
        List<AigcSkill> skills = loadSkills(agent);
        List<AigcMcp> mcps = loadMcps(agent);

        log.info(
                "加载 Agent 运行时完成，agentId={}, chatModelId={}, knowledgeSize={}, skillSize={}, mcpSize={}",
                agentId,
                chatModel.getId(),
                knowledges.size(),
                skills.size(),
                mcps.size()
        );
        return new AgentRuntimeDefinition(agent, chatModel, knowledges, skills, mcps);
    }

    private AigcModel loadChatModel(AigcAgent agent) {
        String modelId = firstNonBlank(agent.getReasoningModelId(), "");
        if (modelId == null) {
            throw new BizException(CoreErrorCode.MODEL_NOT_FOUND);
        }
        AigcModel model = aigcModelService.getById(modelId);
        if (model == null) {
            throw new BizException(CoreErrorCode.MODEL_NOT_FOUND);
        }
        return model;
    }

    private List<AigcKnowledge> loadKnowledges(AigcAgent agent) {
        List<String> knowledgeIds = parseIdList(agent.getKnowledgeIds(), "knowledgeIds", agent.getId());
        if (knowledgeIds.isEmpty()) {
            return List.of();
        }
        Map<String, AigcKnowledge> knowledgeMap = aigcKnowledgeService.lambdaQuery()
                .in(AigcKnowledge::getId, knowledgeIds)
                .list()
                .stream()
                .collect(Collectors.toMap(AigcKnowledge::getId, Function.identity(), (left, right) -> left));
        return knowledgeIds.stream()
                .map(knowledgeMap::get)
                .filter(Objects::nonNull)
                .toList();
    }

    private List<AigcSkill> loadSkills(AigcAgent agent) {
        List<String> skillIds = parseIdList(agent.getSkillIds(), "skillIds", agent.getId());
        if (skillIds.isEmpty()) {
            return List.of();
        }
        Map<String, AigcSkill> skillMap = aigcSkillService.lambdaQuery()
                .in(AigcSkill::getId, skillIds)
                .eq(AigcSkill::getEnabled, Boolean.TRUE)
                .list()
                .stream()
                .collect(Collectors.toMap(AigcSkill::getId, Function.identity(), (left, right) -> left));
        return skillIds.stream()
                .map(skillMap::get)
                .filter(Objects::nonNull)
                .toList();
    }

    private List<AigcMcp> loadMcps(AigcAgent agent) {
        List<String> mcpIds = parseIdList(agent.getMcpIds(), "mcpIds", agent.getId());
        if (mcpIds.isEmpty()) {
            return List.of();
        }
        Map<String, AigcMcp> mcpMap = aigcMcpService.lambdaQuery()
                .in(AigcMcp::getId, mcpIds)
                .list()
                .stream()
                .collect(Collectors.toMap(AigcMcp::getId, Function.identity(), (left, right) -> left));
        return mcpIds.stream()
                .map(mcpMap::get)
                .filter(Objects::nonNull)
                .toList();
    }

    private List<String> parseIdList(String rawValue, String fieldName, String agentId) {
        if (rawValue == null || rawValue.isBlank()) {
            return List.of();
        }
        String value = rawValue.trim();
        try {
            if (value.startsWith("[")) {
                return objectMapper.readValue(value, STRING_LIST_TYPE).stream()
                        .filter(Objects::nonNull)
                        .map(String::trim)
                        .filter(item -> !item.isBlank())
                        .distinct()
                        .toList();
            }
        } catch (Exception ex) {
            log.warn("解析 Agent 字段失败，将按分隔字符串兜底处理，agentId={}, fieldName={}", agentId, fieldName, ex);
        }
        return List.of(value.split("[,\\n\\r]")).stream()
                .map(String::trim)
                .map(this::stripQuote)
                .filter(item -> !item.isBlank())
                .distinct()
                .toList();
    }

    private String stripQuote(String value) {
        String normalized = value;
        if (normalized.startsWith("\"")) {
            normalized = normalized.substring(1);
        }
        if (normalized.endsWith("\"")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized.trim();
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        if (second != null && !second.isBlank()) {
            return second;
        }
        return null;
    }
}
