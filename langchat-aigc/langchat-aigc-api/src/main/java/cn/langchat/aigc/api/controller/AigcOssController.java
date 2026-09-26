package cn.langchat.aigc.api.controller;

import cn.langchat.aigc.biz.entity.AigcOss;
import cn.langchat.aigc.biz.service.AigcOssService;
import cn.langchat.common.core.ApiResponse;
import cn.langchat.common.core.CommonErrorCode;
import cn.langchat.common.exception.BizException;
import cn.langchat.common.oss.model.OssObject;
import cn.langchat.common.oss.model.OssUploadRequest;
import cn.langchat.common.oss.service.OssService;
import java.io.IOException;
import java.util.List;
import java.util.Map;
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
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

/**
 * 资源文件管理控制器。
 *
 * @author LangChat Team
 * @since 2026/3/24
 */
@RestController
@RequestMapping("/api/v1/aigc/oss")
@RequiredArgsConstructor
@Slf4j
public class AigcOssController {

    private static final long MAX_UPLOAD_SIZE = 20L * 1024 * 1024;

    private final AigcOssService aigcOssService;
    private final OssService ossService;

    @GetMapping
    public ApiResponse<List<AigcOss>> list() {
        return ApiResponse.success(aigcOssService.lambdaQuery().orderByDesc(AigcOss::getCreateTime).list());
    }

    @GetMapping("/{id}")
    public ApiResponse<AigcOss> detail(@PathVariable("id") String id) {
        return ApiResponse.success(aigcOssService.getById(id));
    }

    /**
     * 上传聊天附件，实际存储位置由 langchat.storage 配置决定。
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<AigcOss> upload(@RequestParam("file") MultipartFile file) throws IOException {
        validateUpload(file);
        String originalFilename = resolveOriginalFilename(file);
        OssObject object = ossService.upload(new OssUploadRequest(
                originalFilename,
                file.getContentType(),
                file.getBytes(),
                Map.of("source", "chat")
        ));

        AigcOss oss = new AigcOss();
        oss.setFilename(object.objectKey());
        oss.setOriginalFilename(originalFilename);
        oss.setUrl(object.url());
        oss.setPath(object.url());
        oss.setSize(safeFileSize(object.size()));
        oss.setExt(resolveExtension(originalFilename));
        oss.setContentType(file.getContentType());
        oss.setPlatform(object.platform());
        aigcOssService.save(oss);
        log.info("上传聊天附件成功，ossId={}, originalFilename={}", oss.getId(), originalFilename);
        return ApiResponse.success(oss);
    }

    @PostMapping
    public ApiResponse<Boolean> create(@RequestBody AigcOss oss) {
        log.info("新增资源文件，filename={}", oss.getFilename());
        return ApiResponse.success(aigcOssService.save(oss));
    }

    @PutMapping("/{id}")
    public ApiResponse<Boolean> update(@PathVariable("id") String id, @RequestBody AigcOss oss) {
        oss.setId(id);
        log.info("更新资源文件，id={}", id);
        return ApiResponse.success(aigcOssService.updateById(oss));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Boolean> remove(@PathVariable("id") String id) {
        log.info("删除资源文件，id={}", id);
        return ApiResponse.success(aigcOssService.removeById(id));
    }

    private void validateUpload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException(CommonErrorCode.BAD_REQUEST.code(), "上传文件不能为空");
        }
        if (file.getSize() > MAX_UPLOAD_SIZE) {
            throw new BizException(CommonErrorCode.BAD_REQUEST.code(), "上传文件不能超过 20 MB");
        }
    }

    private String resolveOriginalFilename(MultipartFile file) {
        String filename = file.getOriginalFilename();
        if (!StringUtils.hasText(filename)) {
            return "attachment";
        }
        String normalized = filename.replace((char) 92, '/');
        return normalized.substring(normalized.lastIndexOf('/') + 1);
    }

    private String resolveExtension(String filename) {
        int index = filename.lastIndexOf('.');
        return index > -1 && index < filename.length() - 1
                ? filename.substring(index + 1).toLowerCase()
                : "";
    }

    private Integer safeFileSize(Long size) {
        if (size == null || size <= 0) {
            return 0;
        }
        return size > Integer.MAX_VALUE ? Integer.MAX_VALUE : size.intValue();
    }
}
