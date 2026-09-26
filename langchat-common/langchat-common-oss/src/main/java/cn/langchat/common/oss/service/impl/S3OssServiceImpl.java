package cn.langchat.common.oss.service.impl;

import cn.langchat.common.oss.config.OssProperties;
import cn.langchat.common.oss.model.OssObject;
import cn.langchat.common.oss.model.OssUploadRequest;
import cn.langchat.common.oss.service.OssService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.net.URI;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.BucketLocationConstraint;
import software.amazon.awssdk.services.s3.model.CreateBucketConfiguration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

/**
 * 基于 AWS SDK v2 的 S3 兼容对象存储实现。
 *
 * @author LangChat Team
 * @since 2026/9/26
 */
@Slf4j
@Service
@ConditionalOnProperty(prefix = "langchat.storage", name = "type", havingValue = "s3")
public class S3OssServiceImpl implements OssService {

    private static final String PLATFORM = "S3";

    private final OssProperties.S3 properties;
    private final S3Client client;

    public S3OssServiceImpl(OssProperties ossProperties) {
        this.properties = ossProperties.getS3();
        validateProperties(properties);

        S3ClientBuilder builder = S3Client.builder()
                .httpClientBuilder(UrlConnectionHttpClient.builder())
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(
                        properties.getAccessKey(), properties.getSecretKey())))
                .region(Region.of(properties.getRegion()))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(properties.isPathStyleAccessEnabled())
                        .build());
        if (StringUtils.hasText(properties.getEndPoint())) {
            builder.endpointOverride(URI.create(properties.getEndPoint()));
        }
        this.client = builder.build();
    }

    @PostConstruct
    public void initializeBucket() {
        try {
            client.headBucket(HeadBucketRequest.builder()
                    .bucket(properties.getBucketName())
                    .build());
            return;
        } catch (S3Exception exception) {
            if (exception.statusCode() != 404) {
                throw new IllegalStateException(
                        "检查 S3 bucket 失败: " + properties.getBucketName(), exception);
            }
        }

        if (!properties.isAutoCreateBucket()) {
            throw new IllegalStateException("S3 bucket 不存在且未开启自动创建: " + properties.getBucketName());
        }

        try {
            CreateBucketRequest.Builder request = CreateBucketRequest.builder()
                    .bucket(properties.getBucketName());
            if (!Region.US_EAST_1.id().equals(properties.getRegion())) {
                request.createBucketConfiguration(CreateBucketConfiguration.builder()
                        .locationConstraint(BucketLocationConstraint.fromValue(properties.getRegion()))
                        .build());
            }
            client.createBucket(request.build());
            log.info("S3 bucket 创建成功，bucket={}", properties.getBucketName());
        } catch (Exception exception) {
            throw new IllegalStateException("初始化 S3 bucket 失败: " + properties.getBucketName(), exception);
        }
    }

    @PreDestroy
    public void closeClient() {
        client.close();
    }

    @Override
    public OssObject upload(OssUploadRequest request) {
        if (request == null || request.bytes() == null || request.bytes().length == 0) {
            throw new IllegalStateException("上传文件内容不能为空");
        }
        String objectKey = UUID.randomUUID().toString().replace("-", "")
                + "_" + sanitizeFilename(request.filename());
        try {
            PutObjectRequest.Builder builder = PutObjectRequest.builder()
                    .bucket(properties.getBucketName())
                    .key(objectKey);
            if (StringUtils.hasText(request.contentType())) {
                builder.contentType(request.contentType());
            }
            if (request.metadata() != null && !request.metadata().isEmpty()) {
                builder.metadata(request.metadata());
            }
            client.putObject(builder.build(), RequestBody.fromBytes(request.bytes()));
            log.info("S3 OSS 上传成功，bucket={}, objectKey={}", properties.getBucketName(), objectKey);
            return new OssObject(objectKey, buildObjectUrl(objectKey), PLATFORM, (long) request.bytes().length);
        } catch (Exception exception) {
            log.error("S3 OSS 上传失败，bucket={}, objectKey={}", properties.getBucketName(), objectKey, exception);
            throw new IllegalStateException("文件上传失败", exception);
        }
    }

    @Override
    public byte[] download(String objectKey) {
        requireObjectKey(objectKey);
        try {
            ResponseBytes<GetObjectResponse> response = client.getObjectAsBytes(GetObjectRequest.builder()
                    .bucket(properties.getBucketName())
                    .key(objectKey)
                    .build());
            return response.asByteArray();
        } catch (Exception exception) {
            log.error("S3 OSS 下载失败，bucket={}, objectKey={}", properties.getBucketName(), objectKey, exception);
            throw new IllegalStateException("文件下载失败", exception);
        }
    }

    @Override
    public boolean delete(String objectKey) {
        requireObjectKey(objectKey);
        try {
            client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(properties.getBucketName())
                    .key(objectKey)
                    .build());
            log.info("S3 OSS 删除成功，bucket={}, objectKey={}", properties.getBucketName(), objectKey);
            return true;
        } catch (Exception exception) {
            log.error("S3 OSS 删除失败，bucket={}, objectKey={}", properties.getBucketName(), objectKey, exception);
            return false;
        }
    }

    private String buildObjectUrl(String objectKey) {
        if (StringUtils.hasText(properties.getPublicBaseUrl())) {
            return properties.getPublicBaseUrl().replaceAll("/+$", "")
                    + "/" + properties.getBucketName() + "/" + objectKey;
        }
        return "s3://" + properties.getBucketName() + "/" + objectKey;
    }

    private static String sanitizeFilename(String filename) {
        if (!StringUtils.hasText(filename)) {
            return "file";
        }
        String normalized = filename.replace((char) 92, '/');
        return normalized.substring(normalized.lastIndexOf('/') + 1);
    }

    private static void requireObjectKey(String objectKey) {
        if (!StringUtils.hasText(objectKey)) {
            throw new IllegalStateException("OSS objectKey 不能为空");
        }
    }

    private static void validateProperties(OssProperties.S3 properties) {
        if (!StringUtils.hasText(properties.getAccessKey())
                || !StringUtils.hasText(properties.getSecretKey())
                || !StringUtils.hasText(properties.getBucketName())
                || !StringUtils.hasText(properties.getRegion())) {
            throw new IllegalStateException(
                    "S3 OSS 配置不完整，请检查 access-key、secret-key、bucket-name 和 region");
        }
        if (StringUtils.hasText(properties.getEndPoint())) {
            try {
                URI.create(properties.getEndPoint());
            } catch (IllegalArgumentException exception) {
                throw new IllegalStateException("S3 end-point 格式不正确: " + properties.getEndPoint(), exception);
            }
        }
    }
}
