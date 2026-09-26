package cn.langchat.claw.aigc.api.controller;

import cn.langchat.claw.aigc.biz.entity.AigcSkill;
import cn.langchat.claw.aigc.biz.service.AigcSkillService;
import cn.langchat.claw.aigc.biz.service.SkillPackageService;
import cn.langchat.claw.aigc.biz.service.skill.SkillFileContent;
import cn.langchat.claw.common.exception.BizException;
import cn.langchat.claw.aigc.biz.service.skill.SkillFileNode;
import cn.langchat.claw.common.core.ApiResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 技能包管理接口。技能以标准化技能包（根目录含 SKILL.md）形式管理，
 * 原始包归档 OSS，本地工作区存放解压副本供运行时直接调用。
 *
 * @author LangChat Team
 * @since 2026/3/24
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/aigc/skills")
public class AigcSkillController {

    private final AigcSkillService aigcSkillService;
    private final SkillPackageService skillPackageService;

    /**
     * 技能列表。
     */
    @GetMapping
    public ApiResponse<List<AigcSkill>> list() {
        return ApiResponse.success(aigcSkillService.lambdaQuery()
                .orderByDesc(AigcSkill::getCreateTime)
                .list());
    }

    /**
     * 技能详情。
     */
    @GetMapping("/{id}")
    public ApiResponse<AigcSkill> detail(@PathVariable("id") String id) {
        return ApiResponse.success(aigcSkillService.getById(id));
    }

    /**
     * 更新技能元数据（标签、启用状态）。
     */
    @PutMapping("/{id}")
    public ApiResponse<Boolean> update(@PathVariable("id") String id, @RequestBody AigcSkill payload) {
        AigcSkill skill = requireSkill(id);
        skill.setTags(payload.getTags());
        skill.setEnabled(payload.getEnabled() == null || payload.getEnabled());
        return ApiResponse.success(aigcSkillService.updateById(skill));
    }

    /**
     * 删除技能（本地工作区 + OSS 原始包 + 记录）。
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Boolean> delete(@PathVariable("id") String id) {
        AigcSkill skill = requireSkill(id);
        skillPackageService.removePackage(skill);
        return ApiResponse.success(aigcSkillService.removeById(id));
    }

    /**
     * 上传 zip 技能包。
     */
    @PostMapping(value = "/upload/package", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<AigcSkill> uploadPackage(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "tags", required = false) String tags) throws IOException {
        String filename = StringUtils.hasText(file.getOriginalFilename())
                ? file.getOriginalFilename()
                : "skill-package.zip";
        AigcSkill skill = skillPackageService.installPackage(file.getBytes(), filename, tags);
        return ApiResponse.success(skill);
    }

    /**
     * 上传文件夹技能包：files 与 paths 一一对应，paths 为浏览器提供的相对路径。
     */
    @PostMapping(value = "/upload/folder", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<AigcSkill> uploadFolder(
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam("paths") List<String> paths,
            @RequestParam(value = "tags", required = false) String tags) throws IOException {
        if (files.size() != paths.size()) {
            throw new BizException("SKILL_FOLDER_MISMATCH", "文件数量与路径数量不一致");
        }
        Map<String, byte[]> fileMap = new HashMap<>();
        String rootName = null;
        for (int index = 0; index < files.size(); index++) {
            String path = paths.get(index);
            if (!StringUtils.hasText(path)) {
                continue;
            }
            fileMap.put(path, files.get(index).getBytes());
            String normalized = path.replace('\\', '/');
            int separator = normalized.indexOf('/');
            if (separator > 0 && rootName == null) {
                rootName = normalized.substring(0, separator);
            }
        }
        AigcSkill skill = skillPackageService.installFolder(fileMap, rootName, tags);
        return ApiResponse.success(skill);
    }

    /**
     * 技能包文件树。
     */
    @GetMapping("/{id}/files")
    public ApiResponse<List<SkillFileNode>> files(@PathVariable("id") String id) {
        return ApiResponse.success(skillPackageService.listFiles(requireSkill(id)));
    }

    /**
     * 读取技能包内文件内容。
     */
    @GetMapping("/{id}/file")
    public ApiResponse<SkillFileContent> readFile(
            @PathVariable("id") String id,
            @RequestParam("path") String path) {
        return ApiResponse.success(skillPackageService.readFile(requireSkill(id), path));
    }

    /**
     * 保存技能包内文件内容（SKILL.md 会同步 frontmatter 元数据）。
     */
    @PutMapping("/{id}/file")
    public ApiResponse<AigcSkill> saveFile(
            @PathVariable("id") String id,
            @RequestBody Map<String, String> payload) {
        AigcSkill skill = requireSkill(id);
        String path = payload.getOrDefault("path", "");
        String content = payload.getOrDefault("content", "");
        AigcSkill updated = skillPackageService.saveFile(skill, path, content);
        aigcSkillService.updateById(updated);
        return ApiResponse.success(updated);
    }

    /**
     * 下载原始技能包（从 OSS）。
     */
    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> download(@PathVariable("id") String id) {
        AigcSkill skill = requireSkill(id);
        byte[] bytes = skillPackageService.downloadPackage(skill);
        String filename = StringUtils.hasText(skill.getOssFilename()) ? skill.getOssFilename() : "skill-package.zip";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition
                        .attachment()
                        .filename(filename, StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .body(bytes);
    }

    private AigcSkill requireSkill(String id) {
        AigcSkill skill = aigcSkillService.getById(id);
        if (skill == null) {
            throw new BizException("SKILL_NOT_FOUND", "技能不存在");
        }
        return skill;
    }
}
