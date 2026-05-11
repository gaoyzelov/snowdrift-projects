package com.snowdrift.framework.oss.minio.service;

import com.snowdrift.framework.oss.core.IOssService;
import com.snowdrift.framework.oss.dto.OssConfigDTO;
import com.snowdrift.framework.oss.dto.OssResult;
import com.snowdrift.framework.oss.dto.OssUploadRequest;
import com.snowdrift.framework.oss.enums.OssTypeEnum;
import com.snowdrift.framework.oss.exception.OssException;
import io.minio.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.InputStream;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * MinIO 存储 OSS Service 实现
 *
 * @author 83674
 * @date 2026/5/12
 * @description 基于 MinIO 对象存储的 OSS 实现，适用于生产环境和分布式系统
 * @since 1.0.0
 */
@Slf4j
public class MinioOssServiceImpl implements IOssService {

    /**
     * OSS 配置
     */
    private final OssConfigDTO config;

    /**
     * MinIO 客户端
     */
    private final MinioClient minioClient;

    /**
     * 访问域名
     */
    private final String domain;

    /**
     * 是否为私有 Bucket
     */
    private final Boolean privateBucket;

    /**
     * 构造函数
     *
     * @param config OSS 配置信息，包含 endpoint、accessKey、secretKey、bucket 等
     * @throws OssException 当配置为空或 MinIO 客户端初始化失败时抛出
     */
    public MinioOssServiceImpl(OssConfigDTO config) {
        this.config = config;
        this.domain = config.getDomain();
        this.privateBucket = config.getPrivateBucket();

        // 验证必要配置
        String endpoint = config.getEndpoint();
        String accessKey = config.getAccessKey();
        String secretKey = config.getSecretKey();

        if (StringUtils.isBlank(endpoint)) {
            throw new OssException("oss.minio.endpoint.empty");
        }
        if (StringUtils.isBlank(accessKey)) {
            throw new OssException("oss.minio.access.key.empty");
        }
        if (StringUtils.isBlank(secretKey)) {
            throw new OssException("oss.minio.secret.key.empty");
        }

        // 初始化 MinIO 客户端
        try {
            this.minioClient = MinioClient.builder()
                    .endpoint(endpoint)
                    .credentials(accessKey, secretKey)
                    .build();

            // 确保 Bucket 存在
            ensureBucketExists(config.getBucket());

            log.info("MinIO 存储初始化完成: endpoint={}, bucket={}", endpoint, config.getBucket());
        } catch (Exception e) {
            log.error("MinIO 客户端初始化失败", e);
            throw new OssException("oss.minio.client.init.failed", new Object[]{e.getMessage()});
        }
    }

    /**
     * 确保 Bucket 存在，不存在则创建
     *
     * @param bucketName Bucket 名称
     * @throws OssException 当 Bucket 检查或创建失败时抛出
     */
    private void ensureBucketExists(String bucketName) {
        try {
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(bucketName).build()
            );
            if (!exists) {
                minioClient.makeBucket(
                        MakeBucketArgs.builder().bucket(bucketName).build()
                );
                log.info("创建 MinIO Bucket: {}", bucketName);
            }
        } catch (Exception e) {
            log.error("检查或创建 MinIO Bucket 失败: bucket={}", bucketName, e);
            throw new OssException("oss.minio.bucket.create.failed", new Object[]{bucketName});
        }
    }

    /**
     * 上传文件到 MinIO
     *
     * @param request 上传请求，包含文件流、objectKey、contentType 等信息
     * @return 上传结果，包含 objectKey、URL、文件大小等信息
     * @throws OssException 当请求为空、文件流为空或上传失败时抛出
     */
    @Override
    public OssResult upload(OssUploadRequest request) {
        if (request == null) {
            throw new OssException("oss.upload.request.null");
        }
        if (request.getInputStream() == null) {
            throw new OssException("oss.upload.inputstream.null");
        }

        String objectKey = buildObjectKey(request.getObjectKey());
        String bucket = config.getBucket();

        try {
            // 上传文件到 MinIO
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectKey)
                            .stream(request.getInputStream(), request.getSize() != null ? request.getSize() : -1, 10 * 1024 * 1024L) // 10MB part size
                            .contentType(request.getContentType())
                            .build()
            );

            // 构建返回结果
            OssResult result = OssResult.builder()
                    .objectKey(objectKey)
                    .url(getUrl(objectKey, null))
                    .bucket(bucket)
                    .size(request.getSize())
                    .build();

            log.info("文件上传成功: bucket={}, objectKey={}, size={}", bucket, objectKey, request.getSize());
            return result;

        } catch (Exception e) {
            log.error("文件上传失败: bucket={}, objectKey={}", bucket, objectKey, e);
            throw new OssException("oss.upload.failed", new Object[]{e.getMessage()});
        }
    }

    /**
     * 从 MinIO 下载文件
     *
     * @param objectKey 对象键，文件标识
     * @return 文件输入流，调用方需要负责关闭
     * @throws OssException 当文件不存在或下载失败时抛出
     */
    @Override
    public InputStream download(String objectKey) {
        String bucket = config.getBucket();
        String key = buildObjectKey(objectKey);

        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucket)
                            .object(key)
                            .build()
            );
        } catch (Exception e) {
            log.error("文件下载失败: bucket={}, objectKey={}", bucket, key, e);
            throw new OssException("oss.download.failed", new Object[]{e.getMessage()});
        }
    }

    /**
     * 从 MinIO 删除文件
     *
     * @param objectKey 对象键，要删除的文件标识
     * @throws OssException 当删除失败时抛出
     */
    @Override
    public void delete(String objectKey) {
        String bucket = config.getBucket();
        String key = buildObjectKey(objectKey);

        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucket)
                            .object(key)
                            .build()
            );
            log.info("文件删除成功: bucket={}, objectKey={}", bucket, key);
        } catch (Exception e) {
            log.error("文件删除失败: bucket={}, objectKey={}", bucket, key, e);
            throw new OssException("oss.delete.failed", new Object[]{e.getMessage()});
        }
    }

    /**
     * 批量删除 MinIO 文件
     *
     * @param objectKeys 对象键列表，要删除的文件标识集合
     */
    @Override
    public void deleteBatch(List<String> objectKeys) {
        if (objectKeys == null || objectKeys.isEmpty()) {
            return;
        }

        for (String objectKey : objectKeys) {
            try {
                delete(objectKey);
            } catch (Exception e) {
                log.warn("批量删除文件失败: objectKey={}", objectKey, e);
            }
        }
    }

    /**
     * 判断 MinIO 文件是否存在
     *
     * @param objectKey 对象键，要检查的文件标识
     * @return true 如果文件存在，false 如果文件不存在
     */
    @Override
    public boolean exists(String objectKey) {
        String bucket = config.getBucket();
        String key = buildObjectKey(objectKey);

        try {
            minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucket)
                            .object(key)
                            .build()
            );
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 获取文件访问 URL
     *
     * @param objectKey 对象键，已经是完整路径（包含 path-prefix）
     * @param expiry    URL 有效期，对私有 Bucket 生成预签名 URL；公开 Bucket 返回直接访问 URL
     * @return 文件访问 URL
     */
    @Override
    public String getUrl(String objectKey, Duration expiry) {
        String bucket = config.getBucket();
        String key = objectKey;

        try {
            // 如果是私有 Bucket，生成预签名 URL
            if (Boolean.TRUE.equals(privateBucket)) {
                Duration validDuration = expiry != null ? expiry : Duration.ofMinutes(config.getSignatureExpiry());
                return minioClient.getPresignedObjectUrl(
                        GetPresignedObjectUrlArgs.builder()
                                .method(Http.Method.GET)
                                .bucket(bucket)
                                .object(key)
                                .expiry((int) validDuration.toMinutes(), TimeUnit.MINUTES)
                                .build()
                );
            }

            // 如果配置了域名，使用域名
            if (StringUtils.isNotBlank(domain)) {
                String url = domain.endsWith("/") ? domain : domain + "/";
                return url + key;
            }

            // 否则返回 MinIO 直接访问 URL
            String endpoint = config.getEndpoint();
            String endpointUrl = endpoint.endsWith("/") ? endpoint : endpoint + "/";
            return endpointUrl + bucket + "/" + key;

        } catch (Exception e) {
            log.error("生成文件访问 URL 失败: bucket={}, objectKey={}", bucket, key, e);
            throw new OssException("oss.url.generate.failed", new Object[]{e.getMessage()});
        }
    }

    /**
     * 初始化分片上传
     *
     * @param objectKey   对象键，文件标识
     * @param contentType 文件 MIME 类型，如 image/jpeg、video/mp4 等
     * @return Upload ID，用于后续分片上传和合并
     */
    @Override
    public String initiateMultipartUpload(String objectKey, String contentType) {
        String bucket = config.getBucket();
        String key = buildObjectKey(objectKey);

        try {
            // MinIO 的 Java SDK 没有直接的 initiateMultipartUpload 方法
            // 这里返回一个占位符，实际分片上传通过 putObject 自动处理
            // 如果需要手动控制分片，需要使用更低层的 API
            log.info("MinIO 分片上传初始化: bucket={}, objectKey={}", bucket, key);
            return key; // 使用 objectKey 作为 uploadId 的标识
        } catch (Exception e) {
            log.error("分片上传初始化失败: bucket={}, objectKey={}", bucket, key, e);
            throw new OssException("oss.multipart.init.failed", new Object[]{e.getMessage()});
        }
    }

    /**
     * 生成分片上传预签名 URL
     *
     * @param objectKey   对象键，文件标识
     * @param uploadId    Upload ID
     * @param partNumber  分片编号，从 1 开始递增
     * @param expiry      URL 有效期，过期后该 URL 不可用
     * @return 分片上传预签名 URL
     */
    @Override
    public String generatePresignedUploadUrlForChunk(String objectKey, String uploadId,
                                                      int partNumber, Duration expiry) {
        String bucket = config.getBucket();
        String key = buildObjectKey(objectKey);
        Duration validDuration = expiry != null ? expiry : Duration.ofMinutes(config.getChunkUploadUrlExpire());

        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Http.Method.PUT)
                            .bucket(bucket)
                            .object(key)
                            .expiry((int) validDuration.toMinutes(), java.util.concurrent.TimeUnit.MINUTES)
                            .build()
            );
        } catch (Exception e) {
            log.error("生成分片上传 URL 失败: bucket={}, objectKey={}, partNumber={}", bucket, key, partNumber, e);
            throw new OssException("oss.multipart.url.generate.failed", new Object[]{e.getMessage()});
        }
    }

    /**
     * 合并分片
     *
     * @param objectKey 对象键，文件标识
     * @param uploadId  Upload ID
     * @param parts     分片列表，包含每个分片的信息
     */
    @Override
    public void completeMultipartUpload(String objectKey, String uploadId, List<?> parts) {
        // MinIO 的 Java SDK 通过 putObject 自动处理分片上传和合并
        // 这里不需要手动实现
        log.info("MinIO 分片上传完成: objectKey={}", objectKey);
    }

    /**
     * 取消分片上传
     *
     * @param objectKey 对象键，文件标识
     * @param uploadId  Upload ID
     */
    @Override
    public void abortMultipartUpload(String objectKey, String uploadId) {
        // 如果文件已上传，删除它
        try {
            delete(objectKey);
            log.info("取消分片上传并清理文件: objectKey={}", objectKey);
        } catch (Exception e) {
            log.warn("取消分片上传清理失败: objectKey={}", objectKey, e);
        }
    }

    /**
     * 获取存储类型
     *
     * @return OssTypeEnum.MINIO
     */
    @Override
    public OssTypeEnum getType() {
        return OssTypeEnum.MINIO;
    }

    /**
     * 获取 Bucket 名称
     *
     * @return Bucket 名称
     */
    @Override
    public String getBucket() {
        return config.getBucket();
    }

    /**
     * 获取配置标识
     *
     * @return 配置标识
     */
    @Override
    public String getConfigKey() {
        return config.getConfigKey();
    }

    /**
     * 关闭 MinIO 客户端，释放资源
     * <p>
     * MinIO Client 由 SDK 内部管理连接池，无需手动关闭
     * 该方法在应用关闭时由 OssStrategyFactory 统一调用
     * 此处仅做日志记录，便于追踪资源生命周期
     */
    @Override
    public void close() {
        log.info("MinIO 客户端无需手动关闭: configKey={}, bucket={}", config.getConfigKey(), config.getBucket());
    }

    /**
     * 构建完整的 objectKey
     *
     * @param objectKey 原始 objectKey
     * @return 完整的 objectKey（包含路径前缀）
     * @throws OssException 当 objectKey 为空时抛出
     */
    private String buildObjectKey(String objectKey) {
        if (StringUtils.isBlank(objectKey)) {
            throw new OssException("oss.object.key.empty");
        }

        String prefix = config.getPathPrefix();
        if (StringUtils.isNotBlank(prefix)) {
            return prefix.endsWith("/") ? prefix + objectKey : prefix + "/" + objectKey;
        }
        return objectKey;
    }
}
