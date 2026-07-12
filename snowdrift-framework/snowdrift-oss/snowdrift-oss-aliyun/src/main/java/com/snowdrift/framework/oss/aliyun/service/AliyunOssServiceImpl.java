package com.snowdrift.framework.oss.aliyun.service;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.DeleteObjectsRequest;
import com.aliyun.oss.model.GeneratePresignedUrlRequest;
import com.aliyun.oss.model.ObjectMetadata;
import com.snowdrift.framework.oss.core.AbstractOssService;
import com.snowdrift.framework.oss.dto.OssConfigDTO;
import com.snowdrift.framework.oss.dto.OssResult;
import com.snowdrift.framework.oss.dto.OssUploadRequest;
import com.snowdrift.framework.oss.exception.OssException;
import com.snowdrift.framework.oss.util.OssUrlBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.lang.NonNull;

import java.io.InputStream;
import java.net.URL;
import java.time.Duration;
import java.util.Date;
import java.util.List;

/**
 * 阿里云 OSS Service 实现
 *
 * @author gaoyzelov
 * @date 2026/5/12
 * @description 阿里云对象存储服务实现
 * @since 1.0.0
 */
@Slf4j
public class AliyunOssServiceImpl extends AbstractOssService {

    /**
     * 阿里云 OSS 客户端
     */
    private final OSS ossClient;

    /**
     * 构造函数
     * <p>
     * 初始化阿里云 OSS Service
     * 验证必要配置并创建 OSS 客户端
     *
     * @param config OSS 配置信息，包含 endpoint、accessKey、secretKey、bucket 等
     * @throws OssException 当配置为空或 OSS 客户端初始化失败时抛出
     */
    public AliyunOssServiceImpl(OssConfigDTO config) {
        super(config);

        String endpoint = config.getEndpoint();
        String accessKey = config.getAccessKey();
        String secretKey = config.getSecretKey();
        String bucket = config.getBucket();
        if (StringUtils.isBlank(endpoint)) {
            throw new OssException("oss.aliyun.endpoint.empty");
        }
        if (StringUtils.isBlank(accessKey)) {
            throw new OssException("oss.aliyun.accessKey.empty");
        }
        if (StringUtils.isBlank(secretKey)) {
            throw new OssException("oss.aliyun.secretKey.empty");
        }
        if (StringUtils.isBlank(bucket)) {
            throw new OssException("oss.aliyun.bucket.empty");
        }

        try {
            this.ossClient = new OSSClientBuilder().build(endpoint, accessKey, secretKey);
        } catch (Exception e) {
            throw new OssException("oss.aliyun.client.init.failed", new Object[]{e.getMessage()});
        }
    }

    /**
     * 上传文件到阿里云 OSS
     * <p>
     * 将文件上传到阿里云对象存储，支持自动设置元数据
     * 上传成功后返回文件信息，包括 objectKey、访问 URL、文件大小等
     *
     * @param request 上传请求，包含文件流、objectKey、contentType 等信息，不能为空
     * @return 上传结果，包含 objectKey、URL、文件大小等信息
     * @throws OssException 当 request 为空、文件流为空或上传失败时抛出
     */
    @Override
    public OssResult upload(@NonNull OssUploadRequest request) {
        String objectKey = buildObjectKey(request.getObjectKey());
        String bucket = super.getBucket();

        try (InputStream inputStream = request.getInputStream()) {
            // 校验请求参数
            request.validate();

            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(request.getSize());
            if (StringUtils.isNotBlank(request.getContentType())) {
                metadata.setContentType(request.getContentType());
            }

            ossClient.putObject(bucket, objectKey, inputStream, metadata);
            log.debug("文件上传成功: bucket={}, objectKey={}, size={}", bucket, objectKey, request.getSize());

            return OssResult.builder()
                    .objectKey(objectKey)
                    .url(getUrl(objectKey, null))
                    .bucket(bucket)
                    .size(request.getSize())
                    .build();
        } catch (Exception e) {
            log.error("文件上传失败: bucket={}, objectKey={}", bucket, objectKey, e);
            throw new OssException("oss.upload.failed", new Object[]{e.getMessage()});
        }
    }

    /**
     * 从阿里云 OSS 下载文件
     * <p>
     * 根据 objectKey 从阿里云 OSS 下载文件，返回文件输入流
     * 调用方需要负责关闭输入流
     *
     * @param objectKey 对象键，文件标识，不能为空
     * @return 文件输入流，调用方需要负责关闭
     * @throws OssException 当文件不存在或下载失败时抛出
     */
    @Override
    public InputStream download(@NonNull String objectKey) {
        String bucket = super.getBucket();

        try {
            return ossClient.getObject(bucket, objectKey).getObjectContent();
        } catch (Exception e) {
            log.error("文件下载失败: bucket={}, objectKey={}", bucket, objectKey, e);
            throw new OssException("oss.download.failed", new Object[]{e.getMessage()});
        }
    }

    /**
     * 从阿里云 OSS 删除文件
     * <p>
     * 根据 objectKey 从阿里云 OSS 删除文件
     * 如果文件不存在，不会抛出异常
     *
     * @param objectKey 对象键，要删除的文件标识，不能为空
     * @throws OssException 当删除失败时抛出
     */
    @Override
    public void delete(@NonNull String objectKey) {
        String bucket = super.getBucket();
        try {
            ossClient.deleteObject(bucket, objectKey);
            log.debug("文件删除成功: bucket={}, objectKey={}", bucket, objectKey);
        } catch (Exception e) {
            log.error("文件删除失败: bucket={}, objectKey={}", bucket, objectKey, e);
            throw new OssException("oss.delete.failed", new Object[]{e.getMessage()});
        }
    }

    /**
     * 从阿里云 OSS 批量删除文件
     * <p>
     * 根据 objectKey 列表从阿里云 OSS 批量删除文件
     * 如果文件不存在，不会抛出异常
     *
     * @param objectKeys 对象键列表，要删除的文件标识，不能为空
     * @throws OssException 当删除失败时抛出
     */
    @Override
    public void deleteBatch(List<String> objectKeys) {
        if (CollectionUtils.isEmpty(objectKeys)) {
            return;
        }
        String bucket = super.getBucket();
        ListUtils.partition(objectKeys, 1000).forEach(partitionKeys -> {
            try {
                DeleteObjectsRequest deleteObjectsRequest = new DeleteObjectsRequest(bucket).withKeys(partitionKeys);
                ossClient.deleteObjects(deleteObjectsRequest);
                log.debug("文件批量删除成功: bucket={}", bucket);
            }catch (Exception e) {
                log.error("文件批量删除失败: bucket={}", bucket, e);
                throw new OssException("oss.delete.failed", new Object[]{e.getMessage()});
            }
        });
    }

    /**
     * 判断文件是否存在于阿里云 OSS
     * <p>
     * 检查指定 objectKey 的文件是否存在于阿里云 OSS 中
     *
     * @param objectKey 对象键，要检查的文件标识，不能为空
     * @return true 如果文件存在，false 如果文件不存在
     * @throws OssException 当检查失败时抛出
     */
    @Override
    public boolean exists(@NonNull String objectKey) {
        String bucket = super.getBucket();
        try {
            return ossClient.doesObjectExist(bucket, objectKey);
        } catch (Exception e) {
            log.error("检查文件存在性失败: bucket={}, objectKey={}", bucket, objectKey, e);
            throw new OssException("oss.exists.check.failed", new Object[]{e.getMessage()});
        }
    }

    /**
     * 获取文件访问 URL
     * <p>
     * 根据 objectKey 生成文件的访问 URL
     * 对于私有 Bucket，会生成带签名的临时访问 URL
     * 对于公开 Bucket，直接返回公开访问 URL 或 CDN 域名
     *
     * @param objectKey 对象键，文件标识，不能为空
     * @param expiry    URL 有效期，仅对私有 Bucket 生效；公开 Bucket 可传 null
     * @return 文件访问 URL
     * @throws OssException 当生成 URL 失败时抛出
     */
    @Override
    public String getUrl(@NonNull String objectKey, Duration expiry) {
        String bucket = super.getBucket();

        // 如果是私有 Bucket，生成预签名 URL
        if (Boolean.TRUE.equals(config.getPrivateBucket())) {
            Duration validDuration = expiry != null ? expiry : Duration.ofMinutes(config.getSignatureExpiry());
            Date expiration = new Date(System.currentTimeMillis() + validDuration.toMillis());

            GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucket, objectKey);
            request.setExpiration(expiration);

            URL url = ossClient.generatePresignedUrl(request);
            return url.toString();
        }

        // 如果配置了域名，使用域名访问
        if (StringUtils.isNotBlank(config.getDomain())) {
            return OssUrlBuilder.buildPathStyleUrl(config.getDomain(), bucket, objectKey);
        }

        return OssUrlBuilder.buildPathStyleUrl(config.getEndpoint(), bucket, objectKey);
    }

    /**
     * 关闭阿里云 OSS 客户端，释放资源
     * <p>
     * 关闭 OSS 客户端，释放底层 HTTP 连接池等资源
     * 该方法在应用关闭时由 OssStrategyFactory 统一调用
     * 关闭后该实例不能再进行任何 OSS 操作
     */
    @Override
    public void close() {
        if (ossClient != null) {
            try {
                ossClient.shutdown();
                log.info("阿里云 OSS 客户端已关闭: configKey={}, bucket={}", config.getConfigKey(), config.getBucket());
            } catch (Exception e) {
                log.error("关闭阿里云 OSS 客户端失败: configKey={}", config.getConfigKey(), e);
            }
        }
    }
}
