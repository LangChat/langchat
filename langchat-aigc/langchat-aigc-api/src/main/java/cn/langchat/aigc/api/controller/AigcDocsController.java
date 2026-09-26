package cn.langchat.aigc.api.controller;

import cn.langchat.aigc.biz.entity.AigcDocs;
import cn.langchat.aigc.biz.entity.AigcKnowledge;
import cn.langchat.aigc.biz.entity.AigcOss;
import cn.langchat.aigc.biz.service.AigcDocsService;
import cn.langchat.aigc.biz.service.AigcKnowledgeService;
import cn.langchat.aigc.biz.service.AigcOssService;
import cn.langchat.common.core.ApiResponse;
import cn.langchat.common.core.CommonErrorCode;
import cn.langchat.common.exception.BizException;
import cn.langchat.common.oss.model.OssObject;
import cn.langchat.common.oss.model.OssUploadRequest;
import cn.langchat.common.oss.service.OssService;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
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
 * 文档管理控制器。
 *
 * @author LangChat Team
 * @since 2026/3/24
 */
@RestController
@RequestMapping("/api/v1/aigc/docs")
@RequiredArgsConstructor
@Slf4j
public class AigcDocsController {

    private static final String DEFAULT_DOC_TYPE = "FILE";
    private static final String DEFAULT_OSS_PLATFORM = "LOCAL";

    private final AigcDocsService aigcDocsService;
    private final AigcKnowledgeService aigcKnowledgeService;
    private final AigcOssService aigcOssService;
    private final OssService ossService;

    @GetMapping
    public ApiResponse<List<AigcDocs>> list(@RequestParam(value = "knowledgeId", required = false) String knowledgeId) {
        return ApiResponse.success(aigcDocsService.lambdaQuery()
                .eq(knowledgeId != null && !knowledgeId.isBlank(), AigcDocs::getKnowledgeId, knowledgeId)
                .orderByDesc(AigcDocs::getCreateTime)
                .list());
    }

    @GetMapping("/{id}")
    public ApiResponse<AigcDocs> detail(@PathVariable("id") String id) {
        return ApiResponse.success(aigcDocsService.getById(id));
    }

    /**
     * 上传知识库文档。
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<AigcDocs> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("knowledgeId") String knowledgeId,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "parentId", required = false) String parentId,
            @RequestParam(value = "enabled", required = false) Boolean enabled,
            @RequestParam(value = "ingestionConfig", required = false) String ingestionConfig
    ) throws IOException {
        validateUploadRequest(file, knowledgeId);
        requireKnowledge(knowledgeId);

        String originalFilename = resolveOriginalFilename(file);
        String extension = resolveExtension(originalFilename);
        OssObject ossObject = ossService.upload(new OssUploadRequest(
                originalFilename,
                file.getContentType(),
                file.getBytes(),
                java.util.Map.of("knowledgeId", knowledgeId)
        ));

        AigcOss oss = buildOssEntity(file, originalFilename, extension, ossObject);
        aigcOssService.save(oss);

        AigcDocs docs = buildDocsEntity(
                knowledgeId,
                oss,
                file,
                extension,
                name,
                type,
                parentId,
                enabled,
                ingestionConfig
        );
        aigcDocsService.save(docs);
        log.info(
                "上传知识库文档成功，knowledgeId={}, docsId={}, ossId={}, originalFilename={}",
                knowledgeId,
                docs.getId(),
                oss.getId(),
                originalFilename
        );
        return ApiResponse.success(docs);
    }

    @PostMapping
    public ApiResponse<Boolean> create(@RequestBody AigcDocs docs) {
        log.info("新增文档，name={}", docs.getName());
        return ApiResponse.success(aigcDocsService.save(docs));
    }

    @PutMapping("/{id}")
    public ApiResponse<Boolean> update(@PathVariable("id") String id, @RequestBody AigcDocs docs) {
        docs.setId(id);
        log.info("更新文档，id={}", id);
        return ApiResponse.success(aigcDocsService.updateById(docs));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Boolean> remove(@PathVariable("id") String id) {
        log.info("删除文档，id={}", id);
        return ApiResponse.success(aigcDocsService.removeById(id));
    }

    private void validateUploadRequest(MultipartFile file, String knowledgeId) {
        if (file == null || file.isEmpty()) {
            throw new BizException(CommonErrorCode.BAD_REQUEST.code(), "上传文件不能为空");
        }
        if (!StringUtils.hasText(knowledgeId)) {
            throw new BizException(CommonErrorCode.BAD_REQUEST.code(), "知识库 ID 不能为空");
        }
    }

    private void requireKnowledge(String knowledgeId) {
        AigcKnowledge knowledge = aigcKnowledgeService.getById(knowledgeId);
        if (knowledge == null) {
            throw new BizException(CommonErrorCode.BAD_REQUEST.code(), "知识库不存在");
        }
    }

    private AigcOss buildOssEntity(
            MultipartFile file,
            String originalFilename,
            String extension,
            OssObject ossObject
    ) {
        AigcOss oss = new AigcOss();
        oss.setFilename(ossObject.objectKey());
        oss.setOriginalFilename(originalFilename);
        oss.setUrl(ossObject.url());
        oss.setPath(ossObject.url());
        oss.setSize(safeFileSize(file.getSize()));
        oss.setExt(extension);
        oss.setContentType(file.getContentType());
        oss.setPlatform(ossObject.platform() == null ? DEFAULT_OSS_PLATFORM : ossObject.platform());
        return oss;
    }

    private AigcDocs buildDocsEntity(
            String knowledgeId,
            AigcOss oss,
            MultipartFile file,
            String extension,
            String name,
            String type,
            String parentId,
            Boolean enabled,
            String ingestionConfig
    ) {
        AigcDocs docs = new AigcDocs();
        docs.setKnowledgeId(knowledgeId);
        docs.setOssId(oss.getId());
        docs.setParentId(parentId);
        docs.setEnabled(enabled == null ? Boolean.TRUE : enabled);
        docs.setIndexingStatus(0);
        docs.setEmbedStatus("pending");
        docs.setEmbedError(null);
        docs.setEmbedStartTime(null);
        docs.setEmbedEndTime(null);
        docs.setIngestionConfig(ingestionConfig);
        docs.setType(resolveDocsType(type, extension));
        docs.setName(StringUtils.hasText(name) ? name : resolveDisplayName(file));
        docs.setExt(extension);
        docs.setUrl(oss.getPath());
        docs.setPdfUrl("pdf".equalsIgnoreCase(extension) ? oss.getPath() : null);
        docs.setContent(null);
        docs.setSize(safeFileSize(file.getSize()));
        return docs;
    }

    private String resolveDocsType(String type, String extension) {
        if (StringUtils.hasText(type)) {
            return type.trim().toUpperCase();
        }
        return switch (extension.toLowerCase()) {
            case "md", "markdown" -> "MARKDOWN";
            case "txt", "text" -> "TEXT";
            default -> DEFAULT_DOC_TYPE;
        };
    }

    private String resolveOriginalFilename(MultipartFile file) {
        return StringUtils.hasText(file.getOriginalFilename()) ? file.getOriginalFilename().trim() : "upload.bin";
    }

    private String resolveDisplayName(MultipartFile file) {
        String originalFilename = resolveOriginalFilename(file);
        int index = originalFilename.lastIndexOf('.');
        return index > 0 ? originalFilename.substring(0, index) : originalFilename;
    }

    private String resolveExtension(String originalFilename) {
        int index = originalFilename.lastIndexOf('.');
        if (index < 0 || index == originalFilename.length() - 1) {
            return "";
        }
        return originalFilename.substring(index + 1).toLowerCase();
    }

    private int safeFileSize(long fileSize) {
        return fileSize > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) fileSize;
    }
}
