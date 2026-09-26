package cn.langchat.aigc.biz.service.skill;

import java.util.List;

/**
 * 技能包内文件树节点。
 *
 * @param name 节点名称
 * @param path 相对技能包根目录的路径
 * @param directory 是否目录
 * @param size 文件大小（目录为 0）
 * @param children 子节点
 * @author LangChat Team
 * @since 2026/9/26
 */
public record SkillFileNode(
        String name,
        String path,
        boolean directory,
        long size,
        List<SkillFileNode> children
) {
}
