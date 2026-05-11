package com.snowdrift.framework.oss.aliyun.service;

import com.aliyun.oss.HttpMethod;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.GeneratePresignedUrlRequest;
import com.aliyun.oss.model.ObjectMetadata;
import com.snowdrift.framework.oss.core.AbstractOssService;
import com.snowdrift.framework.oss.dto.OssConfigDTO;
import com.snowdrift.framework.oss.dto.OssResult;
import com.snowdrift.framework.oss.dto.OssUploadRequest;
import com.snowdrift.framework.oss.enums.OssTypeEnum;
import com.snowdrift.framework.oss.exception.OssException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.InputStream;
import java.net.URL;
import java.time.Duration;
import java.util.Date;
import java.util.List;

/**
 * 阿里云 OSS Service 实现
 *
 * @author 83674
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

        if (StringUtils.isBlank(endpoint)) {
            throw new OssException("oss.aliyun.endpoint.empty");
        }
        if (StringUtils.isBlank(accessKey)) {
            throw new OssException("oss.aliyun.accessKey.empty");
        }
        if (StringUtils.isBlank(secretKey)) {
            throw new OssException("oss.aliyun.secretKey.empty");
        }
        if (StringUtils.isBlank(config.getBucket())) {
            throw new OssException("oss.aliyun.bucket.empty");
        }

        try {
            this.ossClient = new OSSClientBuilder().build(endpoint, accessKey, secretKey);
            log.info("阿里云 OSS 客户端初始化成功: endpoint={}, bucket={}", endpoint, config.getBucket());
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
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(request.getSize());
            if (StringUtils.isNotBlank(request.getContentType())) {
                metadata.setContentType(request.getContentType());
            }

            ossClient.putObject(bucket, objectKey, request.getInputStream(), metadata);

            log.info("文件上传成功: bucket={}, objectKey={}, size={}", bucket, objectKey, request.getSize());

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
    public InputStream download(String objectKey) {
        String bucket = config.getBucket();
        String key = normalizeObjectKey(objectKey);

        try {
            return ossClient.getObject(bucket, key).getObjectContent();
        } catch (Exception e) {
            log.error("文件下载失败: bucket={}, objectKey={}", bucket, key, e);
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
    public void delete(String objectKey) {
        String bucket = config.getBucket();
        String key = normalizeObjectKey(objectKey);

        try {
            ossClient.deleteObject(bucket, key);
            log.info("文件删除成功: bucket={}, objectKey={}", bucket, key);
        } catch (Exception e) {
            log.error("文件删除失败: bucket={}, objectKey={}", bucket, key, e);
            throw new OssException("oss.delete.failed", new Object[]{e.getMessage()});
        }
    }

    /**
     * 批量删除阿里云 OSS 文件
     * <p>
     * 批量删除多个文件，内部会逐个删除
     * 如果某个文件删除失败，会记录警告日志但继续删除其他文件
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
     * 判断文件是否存在于阿里云 OSS
     * <p>
     * 检查指定 objectKey 的文件是否存在于阿里云 OSS 中
     *
     * @param objectKey 对象键，要检查的文件标识，不能为空
     * @return true 如果文件存在，false 如果文件不存在
     * @throws OssException 当检查失败时抛出
     */
    @Override
    public boolean exists(String objectKey) {
        String bucket = config.getBucket();
        String key = normalizeObjectKey(objectKey);

        try {
            return ossClient.doesObjectExist(bucket, key);
        } catch (Exception e) {
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
    public String getUrl(String objectKey, Duration expiry) {
        String bucket = config.getBucket();
        String key = normalizeObjectKey(objectKey);

        if (Boolean.TRUE.equals(privateBucket)) {
            Duration validDuration = expiry != null ? expiry : Duration.ofMinutes(config.getSignatureExpiry());
            Date expiration = new Date(System.currentTimeMillis() + validDuration.toMillis());

            GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucket, key);
            request.setExpiration(expiration);

            URL url = ossClient.generatePresignedUrl(request);
            return url.toString();
        }

        if (StringUtils.isNotBlank(domain)) {
            String domainUrl = domain.endsWith("/") ? domain : domain + "/";
            return domainUrl + key;
        }

        String endpoint = config.getEndpoint();
        if (endpoint.startsWith("http://")) {
            endpoint = endpoint.substring(7);
        } else if (endpoint.startsWith("https://")) {
            endpoint = endpoint.substring(8);
        }
        return "https://" + bucket + "." + endpoint + "/" + key;
    }

    /**
     * 初始化分片上传
     * <p>
     * 开始一个分片上传任务，返回 Upload ID
     * 后续使用该 Upload ID 上传各个分片
     *
     * @param objectKey   对象键，文件标识，不能为空
     * @param contentType 文件 MIME 类型，如 image/jpeg、video/mp4 等，可为 null
     * @return Upload ID，用于后续分片上传和合并
     * @throws OssException 当初始化失败时抛出
     */
    @Override
    public String initiateMultipartUpload(String objectKey, String contentType) {
        String bucket = config.getBucket();
        String key = buildObjectKey(objectKey);

        try {
            com.aliyun.oss.model.InitiateMultipartUploadRequest request =
                    new com.aliyun.oss.model.InitiateMultipartUploadRequest(bucket, key);
            
            if (StringUtils.isNotBlank(contentType)) {
                ObjectMetadata metadata = new ObjectMetadata();
                metadata.setContentType(contentType);
                request.setObjectMetadata(metadata);
            }
            
            com.aliyun.oss.model.InitiateMultipartUploadResult result = ossClient.initiateMultipartUpload(request);

            log.info("初始化分片上传成功: bucket={}, objectKey={}, uploadId={}", bucket, key, result.getUploadId());
            return result.getUploadId();
        } catch (Exception e) {
            log.error("初始化分片上传失败: bucket={}, objectKey={}", bucket, key, e);
            throw new OssException("oss.multipart.init.failed", new Object[]{e.getMessage()});
        }
    }

    /**
     * 生成分片上传预签名 URL
     * <p>
     * 为指定分片生成预签名上传 URL
     * 前端可直接使用该 URL 上传分片，无需经过应用服务器
     *
     * @param objectKey   对象键，文件标识，不能为空
     * @param uploadId    Upload ID，由 initiateMultipartUpload 返回，不能为空
     * @param partNumber  分片编号，从 1 开始递增
     * @param expiry      URL 有效期，过期后该 URL 不可用
     * @return 分片上传预签名 URL
     * @throws OssException 当生成 URL 失败时抛出
     */
    @Override
    public String generatePresignedUploadUrlForChunk(String objectKey, String uploadId, int partNumber, Duration expiry) {
        String bucket = config.getBucket();
        String key = buildObjectKey(objectKey);

        Duration validDuration = expiry != null ? expiry : Duration.ofMinutes(config.getChunkUploadUrlExpire());
        Date expiration = new Date(System.currentTimeMillis() + validDuration.toMillis());

        GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucket, key);
        request.setExpiration(expiration);
        request.setMethod(HttpMethod.PUT);

        URL url = ossClient.generatePresignedUrl(request);
        String presignedUrl = url.toString();
        
        // 手动添加分片上传参数
        String separator = presignedUrl.contains("?") ? "&" : "?";
        presignedUrl += separator + "partNumber=" + partNumber + "&uploadId=" + uploadId;
        
        return presignedUrl;
    }

    /**
     * 合并分片
     * <p>
     * 所有分片上传完成后，调用此方法合并分片为完整文件
     * 合并成功后，分片会被自动清理
     *
     * @param objectKey 对象键，文件标识，不能为空
     * @param uploadId  Upload ID，由 initiateMultipartUpload 返回，不能为空
     * @param parts     分片列表，包含每个分片的信息（PartETag），不能为空
     * @throws OssException 当合并失败时抛出
     */
    @Override
    public void completeMultipartUpload(String objectKey, String uploadId, List<?> parts) {
        String bucket = config.getBucket();
        String key = buildObjectKey(objectKey);

        try {
            java.util.List<com.aliyun.oss.model.PartETag> partETags = new java.util.ArrayList<>();
            for (Object part : parts) {
                if (part instanceof com.aliyun.oss.model.PartETag) {
                    partETags.add((com.aliyun.oss.model.PartETag) part);
                }
            }

            com.aliyun.oss.model.CompleteMultipartUploadRequest request =
                    new com.aliyun.oss.model.CompleteMultipartUploadRequest(bucket, key, uploadId, partETags);
            ossClient.completeMultipartUpload(request);

            log.info("完成分片上传成功: bucket={}, objectKey={}, uploadId={}", bucket, key, uploadId);
        } catch (Exception e) {
            log.error("完成分片上传失败: bucket={}, objectKey={}, uploadId={}", bucket, key, uploadId, e);
            throw new OssException("oss.multipart.complete.failed", new Object[]{e.getMessage()});
        }
    }

    /**
     * 取消分片上传
     * <p>
     * 取消正在进行的分片上传任务，并删除所有已上传的分片
     * 应在用户上传失败或主动取消时调用，避免产生孤儿分片
     *
     * @param objectKey 对象键，文件标识，不能为空
     * @param uploadId  Upload ID，由 initiateMultipartUpload 返回，不能为空
     * @throws OssException 当取消失败时抛出
     */
    @Override
    public void abortMultipartUpload(String objectKey, String uploadId) {
        String bucket = config.getBucket();
        String key = buildObjectKey(objectKey);

        try {
            com.aliyun.oss.model.AbortMultipartUploadRequest request =
                    new com.aliyun.oss.model.AbortMultipartUploadRequest(bucket, key, uploadId);
            ossClient.abortMultipartUpload(request);

            log.info("取消分片上传成功: bucket={}, objectKey={}, uploadId={}", bucket, key, uploadId);
        } catch (Exception e) {
            log.error("取消分片上传失败: bucket={}, objectKey={}, uploadId={}", bucket, key, uploadId, e);
            throw new OssException("oss.multipart.abort.failed", new Object[]{e.getMessage()});
        }
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

    /**
     * 获取存储类型
     *
     * @return OssTypeEnum.ALIYUN
     */
    @Override
    public OssTypeEnum getType() {
        return OssTypeEnum.ALIYUN;
    }
}
