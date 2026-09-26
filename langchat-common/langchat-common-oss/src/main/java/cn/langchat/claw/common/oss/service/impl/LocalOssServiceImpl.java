package cn.langchat.claw.common.oss.service.impl;

import cn.langchat.claw.common.oss.config.OssProperties;
import cn.langchat.claw.common.oss.model.OssObject;
import cn.langchat.claw.common.oss.model.OssUploadRequest;
import cn.langchat.claw.common.oss.service.OssService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 本地文件系统 OSS 实现。
 *
 * @author LangChat Team
 * @since 2026/3/24
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LocalOssServiceImpl implements OssService {

    private final OssProperties ossProperties;

    @Override
    public OssObject upload(OssUploadRequest request) {
        try {
            Path baseDir = Path.of(ossProperties.getLocalBaseDir());
            Files.createDirectories(baseDir);

            String objectKey = UUID.randomUUID().toString().replace("-", "") + "_" + request.filename();
            Path targetPath = baseDir.resolve(objectKey);
            Files.write(targetPath, request.bytes());
            log.info("本地OSS上传成功，objectKey={}", objectKey);
            return new OssObject(objectKey, targetPath.toString(), "LOCAL", (long) request.bytes().length);
        } catch (IOException exception) {
            log.error("本地OSS上传失败", exception);
            throw new IllegalStateException("文件上传失败", exception);
        }
    }

    @Override
    public byte[] download(String objectKey) {
        if (!StringUtils.hasText(objectKey)) {
            throw new IllegalStateException("OSS objectKey 不能为空");
        }
        Path targetPath = resolveObjectPath(objectKey);
        if (!Files.exists(targetPath)) {
            throw new IllegalStateException("OSS 文件不存在，objectKey=" + objectKey);
        }
        try {
            return Files.readAllBytes(targetPath);
        } catch (IOException exception) {
            log.error("本地OSS下载失败，objectKey={}", objectKey, exception);
            throw new IllegalStateException("文件下载失败", exception);
        }
    }

    @Override
    public boolean delete(String objectKey) {
        try {
            Path targetPath = resolveObjectPath(objectKey);
            boolean deleted = Files.deleteIfExists(targetPath);
            log.info("本地OSS删除结果，objectKey={}, deleted={}", objectKey, deleted);
            return deleted;
        } catch (IOException exception) {
            log.error("本地OSS删除失败，objectKey={}", objectKey, exception);
            return false;
        }
    }

    private Path resolveObjectPath(String objectKey) {
        Path baseDir = Path.of(ossProperties.getLocalBaseDir()).toAbsolutePath().normalize();
        Path targetPath = baseDir.resolve(objectKey).normalize();
        if (!targetPath.startsWith(baseDir)) {
            throw new IllegalStateException("非法的 OSS objectKey: " + objectKey);
        }
        return targetPath;
    }
}
