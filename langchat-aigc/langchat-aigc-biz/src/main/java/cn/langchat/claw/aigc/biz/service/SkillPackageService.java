package cn.langchat.claw.aigc.biz.service;

import cn.langchat.claw.aigc.biz.entity.AigcSkill;
import cn.langchat.claw.aigc.biz.service.skill.SkillFileContent;
import cn.langchat.claw.aigc.biz.service.skill.SkillFileNode;
import java.util.List;
import java.util.Map;

/**
 * 技能包服务：负责技能包的上传解析、OSS 归档、本地工作区维护与内容读写。
 *
 * <p>存储约定：原始技能包(zip)归档到 OSS，数据库仅保存 OSS objectKey 与本地工作区相对路径，
 * 解压后的技能包存放在服务器本地工作区中，运行时优先读取本地副本，本地缺失时从 OSS 回源。</p>
 *
 * @author LangChat Team
 * @since 2026/9/26
 */
public interface SkillPackageService {

    /**
     * 安装 zip 格式技能包：解析 SKILL.md、归档 OSS、释放到本地工作区。
     *
     * @param zipBytes 原始 zip 字节
     * @param originalFilename 原始文件名
     * @param tags 附加标签（逗号分隔，可为空）
     * @return 已保存的技能实体
     */
    AigcSkill installPackage(byte[] zipBytes, String originalFilename, String tags);

    /**
     * 安装文件夹形式技能包：按相对路径构建 zip 后走统一安装流程。
     *
     * @param files 相对路径 -> 文件内容
     * @param rootName 文件夹名称（用于生成原始包名）
     * @param tags 附加标签（逗号分隔，可为空）
     * @return 已保存的技能实体
     */
    AigcSkill installFolder(Map<String, byte[]> files, String rootName, String tags);

    /**
     * 列出技能包内文件树。
     */
    List<SkillFileNode> listFiles(AigcSkill skill);

    /**
     * 读取技能包内文本文件内容。
     */
    SkillFileContent readFile(AigcSkill skill, String path);

    /**
     * 保存技能包内文本文件。若保存的是入口文档（SKILL.md），
     * 会重新解析 frontmatter 并同步技能元数据。
     *
     * @return 更新后的技能实体
     */
    AigcSkill saveFile(AigcSkill skill, String path, String content);

    /**
     * 供运行时读取技能文档：确保本地工作区存在（缺失则从 OSS 回源），
     * path 为空时返回入口文档正文，指向目录时返回目录清单。
     */
    String readSkillDocument(AigcSkill skill, String path);

    /**
     * 下载原始技能包（从 OSS）。
     */
    byte[] downloadPackage(AigcSkill skill);

    /**
     * 删除本地工作区副本与 OSS 原始包。
     */
    void removePackage(AigcSkill skill);
}
