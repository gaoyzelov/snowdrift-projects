package com.snowdrift.framework.oss.minio.service;

import com.snowdrift.framework.oss.core.AbstractOssService;
import com.snowdrift.framework.oss.dto.OssConfigDTO;
import com.snowdrift.framework.oss.dto.OssResult;
import com.snowdrift.framework.oss.dto.OssUploadRequest;
import com.snowdrift.framework.oss.exception.OssException;
import com.snowdrift.framework.oss.util.OssUrlBuilder;
import io.minio.*;
import io.minio.errors.ErrorResponseException;
import io.minio.messages.DeleteRequest;
import io.minio.messages.DeleteResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.lang.NonNull;

import java.io.InputStream;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * MinIO 存储 OSS Service 实现
 *
 * @author gaoyzelov
 * @date 2026/5/12
 * @description 基于 MinIO 对象存储的 OSS 实现，适用于生产环境和分布式系统
 * @since 1.0.0
 */
@Slf4j
public class MinioOssServiceImpl extends AbstractOssService {

    /**
     * MinIO 客户端
     */
    private final MinioClient minioClient;

    /**
     * 构造函数
     *
     * @param config OSS 配置信息，包含 endpoint、accessKey、secretKey、bucket 等
     * @throws OssException 当配置为空或 MinIO 客户端初始化失败时抛出
     */
    public MinioOssServiceImpl(OssConfigDTO config) {
        super(config);

        // 验证必要配置
        String endpoint = config.getEndpoint();
        String accessKey = config.getAccessKey();
        String secretKey = config.getSecretKey();
        String bucket = config.getBucket();

        if (StringUtils.isBlank(endpoint)) {
            throw new OssException("oss.minio.endpoint.empty");
        }
        if (StringUtils.isBlank(accessKey)) {
            throw new OssException("oss.minio.accessKey.empty");
        }
        if (StringUtils.isBlank(secretKey)) {
            throw new OssException("oss.minio.secretKey.empty");
        }
        if (StringUtils.isBlank(bucket)) {
            throw new OssException("oss.minio.bucket.empty");
        }

        // 初始化 MinIO 客户端
        try {
            this.minioClient = MinioClient.builder()
                    .endpoint(endpoint)
                    .credentials(accessKey, secretKey)
                    .build();

            // 确保 Bucket 存在
            ensureBucketExists(bucket);
        } catch (Exception e) {
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
    public OssResult upload(@NonNull OssUploadRequest request) {
        // 校验请求参数
        request.validate();

        // 构建对象键
        String objectKey = buildObjectKey(request.getObjectKey());
        String bucket = super.getBucket();

        try (InputStream inputStream = request.getInputStream()) {
            // 上传文件到 MinIO
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectKey)
                            .stream(inputStream, request.getSize() != null ? request.getSize() : -1, config.getChunkSize()) // 10MB part size
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

            log.debug("文件上传成功: bucket={}, objectKey={}, size={}", bucket, objectKey, request.getSize());
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
    public InputStream download(@NonNull String objectKey) {
        String bucket = super.getBucket();
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectKey)
                            .build()
            );
        } catch (Exception e) {
            log.error("文件下载失败: bucket={}, objectKey={}", bucket, objectKey, e);
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
    public void delete(@NonNull String objectKey) {
        String bucket = super.getBucket();

        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectKey)
                            .build()
            );
            log.debug("文件删除成功: bucket={}, objectKey={}", bucket, objectKey);
        } catch (Exception e) {
            log.error("文件删除失败: bucket={}, objectKey={}", bucket, objectKey, e);
            throw new OssException("oss.delete.failed", new Object[]{e.getMessage()});
        }
    }

    /**
     * 从 MinIO 批量删除文件
     *
     * @param objectKeys 对象键列表，要删除的文件标识列表
     * @throws OssException 当删除失败时抛出
     */
    @Override
    public void deleteBatch(List<String> objectKeys) {
        if (CollectionUtils.isEmpty(objectKeys)) {
            return;
        }
        String bucket = super.getBucket();
        ListUtils.partition(objectKeys,1000).forEach(partitionKeys -> {
            try {
                List<DeleteRequest.Object> objects = partitionKeys.stream().map(DeleteRequest.Object::new).toList();
                Iterable<Result<DeleteResult.Error>> resultIterable = minioClient.removeObjects(
                        RemoveObjectsArgs.builder()
                                .bucket(bucket)
                                .objects(objects)
                                .build()
                );
                int errorCount = 0;
                for (Result<DeleteResult.Error> result : resultIterable) {
                    errorCount++;
                    DeleteResult.Error error = result.get();
                    log.error("文件批量删除失败: bucket={}, objectKey={}, error={}", bucket, error.objectName(), error.message());
                }
                if (errorCount > 0) {
                    throw new OssException("oss.delete.batch.partial-failed", new Object[]{errorCount});
                }
                log.debug("文件批量删除成功: bucket={}", bucket);
            } catch (Exception e) {
                log.error("文件批量删除失败: bucket={}", bucket, e);
                throw new OssException("oss.delete.failed", new Object[]{e.getMessage()});
            }
        });
    }

    /**
     * 判断 MinIO 文件是否存在
     *
     * @param objectKey 对象键，要检查的文件标识
     * @return true 如果文件存在，false 如果文件不存在
     */
    @Override
    public boolean exists(@NonNull String objectKey) {
        String bucket = super.getBucket();
        try {
            minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectKey)
                            .build()
            );
            return true;
        } catch (ErrorResponseException e) {
            // 404 表示文件不存在，其余为服务端异常
            if ("NoSuchKey".equals(e.errorResponse().code()) ||
                    "NotFound".equals(e.errorResponse().code())) {
                return false;
            }
            throw new OssException("oss.exists.check.failed", new Object[]{objectKey}, e);
        } catch (Exception e) {
            throw new OssException("oss.exists.check.failed", new Object[]{objectKey}, e);
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
    public String getUrl(@NonNull String objectKey, Duration expiry) {
        String bucket = super.getBucket();

        // 如果是私有 Bucket，生成预签名 URL
        if (Boolean.TRUE.equals(config.getPrivateBucket())) {
            Duration validDuration = expiry != null ? expiry : Duration.ofMinutes(config.getSignatureExpiry());
            try {
                return minioClient.getPresignedObjectUrl(
                        GetPresignedObjectUrlArgs.builder()
                                .method(Http.Method.GET)
                                .bucket(bucket)
                                .object(objectKey)
                                .expiry((int) validDuration.toMinutes(), TimeUnit.MINUTES)
                                .build()
                );
            } catch (Exception e) {
                log.error("生成文件访问 URL 失败: bucket={}, objectKey={}", bucket, objectKey, e);
                throw new OssException("oss.url.generate.failed", new Object[]{e.getMessage()});
            }
        }

        // 如果配置了域名，使用域名
        if (StringUtils.isNotBlank(config.getDomain())) {
            return OssUrlBuilder.buildPathStyleUrl(config.getDomain(), bucket, objectKey);
        }

        // 否则返回 MinIO 直接访问 URL
        return OssUrlBuilder.buildPathStyleUrl(config.getEndpoint(), bucket, objectKey);

    }

    /**
     * 关闭 MinIO 客户端，释放资源
     * <p>
     * 该方法在应用关闭时由 OssStrategyFactory 统一调用
     * 此处仅做日志记录，便于追踪资源生命周期
     */
    @Override
    public void close() {
        if (minioClient != null) {
            try {
                minioClient.close();
                log.info("MinIO 客户端已关闭: configKey={}, bucket={}", config.getConfigKey(), config.getBucket());
            } catch (Exception e) {
                log.error("MinIO 客户端关闭失败: configKey={}, bucket={}", config.getConfigKey(), config.getBucket(), e);
            }
        }
    }
}
