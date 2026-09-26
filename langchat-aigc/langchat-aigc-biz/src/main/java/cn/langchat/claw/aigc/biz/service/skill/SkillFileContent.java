package cn.langchat.claw.aigc.biz.service.skill;

/**
 * 技能包内文件内容。
 *
 * @param path 相对技能包根目录的路径
 * @param content 文本内容（二进制文件为 null）
 * @param binary 是否二进制文件
 * @param size 文件大小（字节）
 * @author LangChat Team
 * @since 2026/9/26
 */
public record SkillFileContent(
        String path,
        String content,
        boolean binary,
        long size
) {
}
