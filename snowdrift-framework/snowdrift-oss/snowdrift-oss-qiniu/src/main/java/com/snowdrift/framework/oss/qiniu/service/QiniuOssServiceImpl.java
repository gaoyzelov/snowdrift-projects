package com.snowdrift.framework.oss.qiniu.service;

import com.alibaba.fastjson2.JSON;
import com.qiniu.common.QiniuException;
import com.qiniu.http.Response;
import com.qiniu.storage.BucketManager;
import com.qiniu.storage.Configuration;
import com.qiniu.storage.Region;
import com.qiniu.storage.UploadManager;
import com.qiniu.storage.model.DefaultPutRet;
import com.qiniu.storage.model.FetchRet;
import com.qiniu.storage.model.FileInfo;
import com.qiniu.util.Auth;
import com.snowdrift.framework.oss.core.AbstractOssService;
import com.snowdrift.framework.oss.dto.OssConfigDTO;
import com.snowdrift.framework.oss.dto.OssResult;
import com.snowdrift.framework.oss.dto.OssUploadRequest;
import com.snowdrift.framework.oss.enums.OssTypeEnum;
import com.snowdrift.framework.oss.exception.OssException;
import com.snowdrift.framework.oss.util.OssUrlBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.lang.NonNull;

import java.io.InputStream;
import java.time.Duration;

/**
 * 七牛云 Kodo OSS Service 实现
 *
 * @author 83674
 * @date 2026/5/12
 * @description 基于七牛云对象存储的 OSS 实现，适用于 CDN 加速场景
 * @since 1.0.0
 */
@Slf4j
public class QiniuOssServiceImpl extends AbstractOssService {

    /**
     * 七牛云认证对象
     */
    private final Auth auth;

    /**
     * 上传管理器
     */
    private final UploadManager uploadManager;

    /**
     * Bucket 管理器
     */
    private final BucketManager bucketManager;

    /**
     * 构造函数
     * <p>
     * 初始化七牛云 OSS Service
     * 验证必要配置并创建上传管理器和 Bucket 管理器
     *
     * @param config OSS 配置信息，包含 accessKey、secretKey、bucket、domain 等
     * @throws OssException 当配置为空或客户端初始化失败时抛出
     */
    public QiniuOssServiceImpl(OssConfigDTO config) {
        super(config);

        String accessKey = config.getAccessKey();
        String secretKey = config.getSecretKey();
        String region = config.getRegion();

        if (StringUtils.isBlank(accessKey)) {
            throw new OssException("oss.qiniu.accessKey.empty");
        }
        if (StringUtils.isBlank(secretKey)) {
            throw new OssException("oss.qiniu.secretKey.empty");
        }
        if (StringUtils.isBlank(config.getBucket())) {
            throw new OssException("oss.qiniu.bucket.empty");
        }
        if (StringUtils.isBlank(domain)) {
            throw new OssException("oss.qiniu.domain.empty");
        }

        try {
            // 初始化认证对象
            this.auth = Auth.create(accessKey, secretKey);

            // 初始化区域配置
            Region qiniuRegion = StringUtils.isNotBlank(region) ? Region.createWithRegionId(region) : Region.autoRegion();
            Configuration configuration = Configuration.create(qiniuRegion);
            configuration.resumableUploadAPIVersion = Configuration.ResumableUploadAPIVersion.V2;

            // 初始化上传管理器
            this.uploadManager = new UploadManager(configuration);

            // 初始化 Bucket 管理器
            this.bucketManager = new BucketManager(auth, configuration);

            log.info("七牛云 OSS 客户端初始化成功: bucket={}, domain={}, region={}",
                    config.getBucket(), domain, region != null ? region : "auto");
        } catch (Exception e) {
            throw new OssException("oss.qiniu.client.init.failed", new Object[]{e.getMessage()});
        }
    }

    /**
     * 上传文件到七牛云 Kodo
     * <p>
     * 将文件上传到七牛云对象存储，使用上传 Token 进行认证
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

        String objectKey = buildObjectKey(request.getObjectKey());
        String bucket = config.getBucket();

        // 生成上传 Token（expires 参数是秒）
        Long expireSeconds = config.getUploadTokenExpire() != null ? config.getUploadTokenExpire() * 60L : 3600L;
        String uploadToken = auth.uploadToken(bucket, objectKey, expireSeconds, null);
        try (InputStream inputStream = request.getInputStream()) {
            // 读取文件流为字节数组
            byte[] fileBytes = inputStream.readAllBytes();

            // 上传文件
            Response response = uploadManager.put(fileBytes, objectKey, uploadToken);

            // 解析返回结果（用于日志记录）
            DefaultPutRet putRet = JSON.parseObject(
                    response.bodyString(), DefaultPutRet.class);

            log.debug("文件上传成功: bucket={}, objectKey={}, hash={}, size={}",
                    bucket, putRet.key, putRet.hash, request.getSize());

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
     * 从七牛云 Kodo 下载文件
     * <p>
     * 根据 objectKey 从七牛云下载文件，返回文件输入流
     * 对于私有空间，会生成下载凭证
     * 调用方需要负责关闭输入流
     *
     * @param objectKey 对象键，文件标识，不能为空
     * @return 文件输入流，调用方需要负责关闭
     * @throws OssException 当文件不存在或下载失败时抛出
     */
    @Override
    public InputStream download(@NonNull String objectKey) {
        String bucket = config.getBucket();
        String key = normalizeObjectKey(objectKey);

        try {
            // 生成下载 URL（私有空间会自动添加签名）
            String downloadUrl = getDownloadUrl(key);

            // 使用七牛云的 fetch 方法获取文件
            FetchRet fetchRet = bucketManager.fetch(downloadUrl, bucket, key);

            // 返回文件流（这里需要从七牛云 CDN 下载）
            // 注意：七牛云 SDK 没有直接的 getObject 方法，需要通过 URL 下载
            java.net.URL url = new java.net.URL(downloadUrl);
            return url.openStream();
        } catch (Exception e) {
            log.error("文件下载失败: bucket={}, objectKey={}", bucket, key, e);
            throw new OssException("oss.download.failed", new Object[]{e.getMessage()});
        }
    }

    /**
     * 从七牛云 Kodo 删除文件
     * <p>
     * 根据 objectKey 从七牛云删除文件
     * 如果文件不存在，不会抛出异常
     *
     * @param objectKey 对象键，要删除的文件标识，不能为空
     * @throws OssException 当删除失败时抛出
     */
    @Override
    public void delete(@NonNull String objectKey) {
        String bucket = config.getBucket();
        String key = normalizeObjectKey(objectKey);

        try {
            bucketManager.delete(bucket, key);
            log.debug("文件删除成功: bucket={}, objectKey={}", bucket, key);
        } catch (QiniuException e) {
            // 如果文件不存在（612 错误码），不抛出异常
            if (e.code() == 612) {
                log.debug("文件不存在，无需删除: bucket={}, objectKey={}", bucket, key);
                return;
            }
            log.error("文件删除失败: bucket={}, objectKey={}", bucket, key, e);
            throw new OssException("oss.delete.failed", new Object[]{e.getMessage()});
        }
    }

    /**
     * 批量删除七牛云 Kodo 文件
     * <p>
     * 批量删除多个文件，内部会逐个删除
     * 如果某个文件删除失败，会记录警告日志但继续删除其他文件
     *
     * @param objectKeys 对象键列表，要删除的文件标识集合
     */
    @Override
    public void deleteBatch(java.util.List<String> objectKeys) {
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
     * 判断文件是否存在于七牛云 Kodo
     * <p>
     * 检查指定 objectKey 的文件是否存在于七牛云中
     *
     * @param objectKey 对象键，要检查的文件标识，不能为空
     * @return true 如果文件存在，false 如果文件不存在
     * @throws OssException 当检查失败时抛出
     */
    @Override
    public boolean exists(@NonNull String objectKey) {
        String bucket = config.getBucket();
        String key = normalizeObjectKey(objectKey);

        try {
            FileInfo fileInfo = bucketManager.stat(bucket, key);
            return fileInfo != null && !StringUtils.isBlank(fileInfo.hash);
        } catch (QiniuException e) {
            // 文件不存在时返回 612 错误码
            if (e.code() == 612) {
                return false;
            }
            log.error("检查文件存在性失败: bucket={}, objectKey={}", bucket, key, e);
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
        String key = normalizeObjectKey(objectKey);

        if (Boolean.TRUE.equals(privateBucket)) {
            // 私有空间：生成带签名的 URL
            Duration validDuration = expiry != null ? expiry : Duration.ofMinutes(config.getSignatureExpiry());
            return auth.privateDownloadUrl(domain + "/" + key, (int) validDuration.toSeconds());
        }

        // 公开空间：直接返回 CDN 域名 + objectKey
        return buildUrl(key);
    }

    /**
     * 初始化分片上传
     * <p>
     * 七牛云不支持传统的分片上传初始化
     * 七牛云使用上传 Token 控制上传权限
     *
     * @param objectKey   对象键，文件标识，不能为空
     * @param contentType 文件 MIME 类型，如 image/jpeg、video/mp4 等，可为 null
     * @return Upload ID（七牛云返回空字符串）
     * @throws UnsupportedOperationException 七牛云不支持此操作
     */
    @Override
    public String initiateMultipartUpload(String objectKey, String contentType) {
        throw new UnsupportedOperationException("七牛云不支持分片上传初始化，请使用普通上传");
    }

    /**
     * 生成分片上传预签名 URL
     * <p>
     * 七牛云不支持分片上传
     *
     * @param objectKey  对象键，文件标识
     * @param uploadId   Upload ID
     * @param partNumber 分片编号
     * @param expiry     URL 有效期
     * @return 分片上传预签名 URL
     * @throws UnsupportedOperationException 七牛云不支持此操作
     */
    @Override
    public String generatePresignedUploadUrlForChunk(String objectKey, String uploadId, int partNumber, Duration expiry) {
        throw new UnsupportedOperationException("七牛云不支持分片上传");
    }

    /**
     * 合并分片
     * <p>
     * 七牛云不支持分片上传
     *
     * @param objectKey 对象键，文件标识
     * @param uploadId  Upload ID
     * @param parts     分片列表
     * @throws UnsupportedOperationException 七牛云不支持此操作
     */
    @Override
    public void completeMultipartUpload(String objectKey, String uploadId, java.util.List<?> parts) {
        throw new UnsupportedOperationException("七牛云不支持分片上传");
    }

    /**
     * 取消分片上传
     * <p>
     * 七牛云不支持分片上传
     *
     * @param objectKey 对象键，文件标识
     * @param uploadId  Upload ID
     * @throws UnsupportedOperationException 七牛云不支持此操作
     */
    @Override
    public void abortMultipartUpload(String objectKey, String uploadId) {
        throw new UnsupportedOperationException("七牛云不支持分片上传");
    }

    /**
     * 获取存储类型
     *
     * @return OSS 存储类型枚举（QINIU）
     */
    @Override
    public OssTypeEnum getType() {
        return OssTypeEnum.QINIU;
    }

    /**
     * 关闭七牛云 OSS 客户端
     * <p>
     * 七牛云 SDK 无需手动关闭连接池
     * 该方法在应用关闭时由 OssStrategyFactory 统一调用
     * 此处仅做日志记录，便于追踪资源生命周期
     */
    @Override
    public void close() {
        log.info("七牛云客户端无需手动关闭: configKey={}, bucket={}", config.getConfigKey(), config.getBucket());
    }

    /**
     * 获取下载 URL（私有空间会自动添加签名）
     *
     * @param objectKey 对象键
     * @return 下载 URL
     */
    private String getDownloadUrl(String objectKey) {
        String url = OssUrlBuilder.buildUrl(domain, objectKey);

        if (Boolean.TRUE.equals(privateBucket)) {
            // 私有空间生成下载凭证
            return auth.privateDownloadUrl(url, config.getSignatureExpiry() * 60L);
        }

        return url;
    }
}

