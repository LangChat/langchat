package cn.langchat.aigc.biz.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 技能包存储配置。
 *
 * @author LangChat Team
 * @since 2026/9/26
 */
@Data
@Component
@ConfigurationProperties(prefix = "langchat.skill")
public class SkillProperties {

    /**
     * 技能工作区根目录（相对项目运行目录或绝对路径），
     * 解压后的技能包存放于此，运行时直接读取本地副本，缺失时从 OSS 回源。
     */
    private String workspaceDir = "./langchat-workspace";
}
