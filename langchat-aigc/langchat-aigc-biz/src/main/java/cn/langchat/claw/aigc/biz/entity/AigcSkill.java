package cn.langchat.claw.aigc.biz.entity;

import cn.langchat.claw.common.persistence.BaseDO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Skill 主表实体。技能以标准化技能包（根目录含 SKILL.md）形式存在，
 * 原始包存储于 OSS，解压后的工作副本存储于服务器本地工作区。
 *
 * @author LangChat Team
 * @since 2026/3/24
 */
@Data
@TableName("aigc_skill")
@EqualsAndHashCode(callSuper = true)
public class AigcSkill extends BaseDO {

    /** 主键。 */
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    /** 技能标识（取自 SKILL.md frontmatter name），运行时作为工具名。 */
    private String name;

    /** 技能标题（frontmatter title，缺省回退 name）。 */
    private String title;

    /** 技能描述（frontmatter description）。 */
    private String description;

    /** 技能版本（frontmatter version）。 */
    private String version;

    /** 入口文档相对路径，默认 SKILL.md。 */
    private String entryFile;

    /** 开源协议（frontmatter license）。 */
    private String license;

    /** 标签，逗号分隔。 */
    private String tags;

    /** 是否启用。 */
    private Boolean enabled;

    /** 原始技能包大小（字节）。 */
    private Long packageSize;

    /** 包内文件数量。 */
    private Integer fileCount;

    /** 原始包在 OSS 中的 objectKey。 */
    private String ossObjectKey;

    /** 原始包文件名（zip 名或目录名）。 */
    private String ossFilename;

    /** 本地工作区相对路径（相对技能工作区根目录）。 */
    private String localPath;
}
