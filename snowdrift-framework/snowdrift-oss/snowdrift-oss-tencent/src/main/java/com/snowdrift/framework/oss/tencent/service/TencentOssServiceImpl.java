package com.snowdrift.framework.oss.tencent.service;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.exception.CosServiceException;
import com.qcloud.cos.model.*;
import com.qcloud.cos.region.Region;
import com.snowdrift.framework.oss.core.AbstractOssService;
import com.snowdrift.framework.oss.dto.OssConfigDTO;
import com.snowdrift.framework.oss.dto.OssResult;
import com.snowdrift.framework.oss.dto.OssUploadRequest;
import com.snowdrift.framework.oss.exception.OssException;
import com.snowdrift.framework.oss.util.OssUrlBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.lang.NonNull;

import java.io.InputStream;
import java.time.Duration;
import java.util.Date;

/**
 * 腾讯云 COS OSS Service 实现
 *
 * @author 83674
 * @date 2026/5/11
 * @description 基于腾讯云对象存储的 OSS 实现，适用于腾讯云生态场景
 * @since 1.0.0
 */
@Slf4j
public class TencentOssServiceImpl extends AbstractOssService {

    /**
     * 腾讯云 COS 客户端
     */
    private final COSClient cosClient;

    /**
     * 构造函数
     * <p>
     * 初始化腾讯云 COS OSS Service
     * 验证必要配置并创建 COS 客户端
     *
     * @param config OSS 配置信息，包含 accessKey、secretKey、bucket、domain、region 等
     * @throws OssException 当配置为空或客户端初始化失败时抛出
     */
    public TencentOssServiceImpl(OssConfigDTO config) {
        super(config);

        String accessKey = config.getAccessKey();
        String secretKey = config.getSecretKey();
        String region = config.getRegion();
        String domain = config.getDomain();

        if (StringUtils.isBlank(accessKey)) {
            throw new OssException("oss.tencent.accessKey.empty");
        }
        if (StringUtils.isBlank(secretKey)) {
            throw new OssException("oss.tencent.secretKey.empty");
        }
        if (StringUtils.isBlank(config.getBucket())) {
            throw new OssException("oss.tencent.bucket.empty");
        }
        if (StringUtils.isBlank(domain)) {
            throw new OssException("oss.tencent.domain.empty");
        }
        if (StringUtils.isBlank(region)) {
            throw new OssException("oss.tencent.region.empty");
        }

        try {
            // 初始化认证信息
            COSCredentials cred = new BasicCOSCredentials(accessKey, secretKey);

            // 初始化客户端配置
            ClientConfig clientConfig = new ClientConfig(new Region(region));

            // 创建 COS 客户端
            this.cosClient = new COSClient(cred, clientConfig);

            log.info("腾讯云 COS 客户端初始化成功: bucket={}, domain={}, region={}",
                    config.getBucket(), domain, region);
        } catch (Exception e) {
            throw new OssException("oss.tencent.client.init.failed", new Object[]{e.getMessage()});
        }
    }

    /**
     * 上传文件到腾讯云 COS
     * <p>
     * 将文件上传到腾讯云对象存储
     * 上传成功后返回文件信息，包括 objectKey、访问 URL、文件大小等
     *
     * @param request 上传请求，包含文件流、objectKey、contentType 等信息，不能为空
     * @return 上传结果，包含 objectKey、URL、文件大小等信息
     * @throws OssException 当 request 为空、文件流为空或上传失败时抛出
     */
    @Override
    public OssResult upload(@NonNull OssUploadRequest request) {
        // 校验请求参数
        request.validate();

        String bucket = super.getBucket();
        String objectKey = buildObjectKey(request.getObjectKey());

        try (InputStream inputStream = request.getInputStream()) {
            // 创建上传请求
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(request.getSize());
            if (StringUtils.isNotBlank(request.getContentType())) {
                metadata.setContentType(request.getContentType());
            }
            PutObjectRequest putObjectRequest = new PutObjectRequest(
                    bucket, objectKey, inputStream, metadata);

            // 上传文件
            PutObjectResult putObjectResult = cosClient.putObject(putObjectRequest);

            log.debug("文件上传成功: bucket={}, objectKey={}, ETag={}, size={}",
                    bucket, objectKey, putObjectResult.getETag(), request.getSize());

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
     * 从腾讯云 COS 下载文件
     * <p>
     * 根据 objectKey 从腾讯云下载文件，返回文件输入流
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
            // 获取对象
            COSObject cosObject = cosClient.getObject(bucket, objectKey);
            return cosObject.getObjectContent();
        } catch (CosServiceException e) {
            if (e.getStatusCode() == 404) {
                throw new OssException("oss.file.not.exists", new Object[]{objectKey});
            }
            log.error("文件下载失败: bucket={}, objectKey={}", bucket, objectKey, e);
            throw new OssException("oss.download.failed", new Object[]{e.getMessage()});
        } catch (Exception e) {
            log.error("文件下载失败: bucket={}, objectKey={}", bucket, objectKey, e);
            throw new OssException("oss.download.failed", new Object[]{e.getMessage()});
        }
    }

    /**
     * 从腾讯云 COS 删除文件
     * <p>
     * 根据 objectKey 从腾讯云删除文件
     * 如果文件不存在，不会抛出异常
     *
     * @param objectKey 对象键，要删除的文件标识，不能为空
     * @throws OssException 当删除失败时抛出
     */
    @Override
    public void delete(@NonNull String objectKey) {
        String bucket = super.getBucket();

        try {
            cosClient.deleteObject(bucket, objectKey);
            log.debug("文件删除成功: bucket={}, objectKey={}", bucket, objectKey);
        } catch (CosServiceException e) {
            // 如果文件不存在（404），不抛出异常
            if (e.getStatusCode() == 404) {
                log.debug("文件不存在，无需删除: bucket={}, objectKey={}", bucket, objectKey);
                return;
            }
            log.error("文件删除失败: bucket={}, objectKey={}", bucket, objectKey, e);
            throw new OssException("oss.delete.failed", new Object[]{e.getMessage()});
        }
    }

    /**
     * 判断文件是否存在于腾讯云 COS
     * <p>
     * 检查指定 objectKey 的文件是否存在于腾讯云中
     *
     * @param objectKey 对象键，要检查的文件标识，不能为空
     * @return true 如果文件存在，false 如果文件不存在
     * @throws OssException 当检查失败时抛出
     */
    @Override
    public boolean exists(@NonNull String objectKey) {
        String bucket = super.getBucket();
        try {
            return cosClient.doesObjectExist(bucket, objectKey);
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
        if (Boolean.TRUE.equals(config.getPrivateBucket())) {
            // 私有 Bucket：生成预签名 URL
            GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucket, objectKey);
            Duration validDuration = expiry != null ? expiry : Duration.ofMinutes(config.getSignatureExpiry());
            request.setExpiration(new Date(System.currentTimeMillis() + validDuration.toMillis()));
            return cosClient.generatePresignedUrl(request).toString();
        }

        // 公开 Bucket：直接返回域名 + objectKey
        return OssUrlBuilder.buildPathStyleUrl(config.getDomain(),bucket, objectKey);
    }

    /**
     * 关闭腾讯云 COS 客户端
     * <p>
     * 释放客户端连接池和相关资源
     * 该方法在应用关闭时由 OssStrategyFactory 统一调用
     */
    @Override
    public void close() {
        if (cosClient != null) {
            cosClient.shutdown();
            log.info("腾讯云 COS 客户端已关闭: configKey={}, bucket={}", config.getConfigKey(), config.getBucket());
        }
    }
}

