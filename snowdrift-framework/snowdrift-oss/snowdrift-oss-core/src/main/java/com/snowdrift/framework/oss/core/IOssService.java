package com.snowdrift.framework.oss.core;

import com.snowdrift.framework.oss.dto.OssResult;
import com.snowdrift.framework.oss.dto.OssUploadRequest;
import com.snowdrift.framework.oss.enums.OssTypeEnum;

import java.io.InputStream;
import java.time.Duration;
import java.util.List;

/**
 * OSS 统一操作接口
 *
 * @author 83674
 * @date 2026/5/9
 * @description 定义所有 OSS 存储的统一操作接口，屏蔽底层差异
 * @since 1.0.0
 */
public interface IOssService {
    
    // ========== 基础操作 ==========
    
    /**
     * 上传文件
     * <p>
     * 将文件上传到 OSS 存储，支持各种存储类型（本地、MinIO、阿里云等）
     * 上传成功后返回文件信息，包括 objectKey、访问 URL、文件大小等
     *
     * @param request 上传请求，包含文件流、objectKey、contentType 等信息
     * @return 上传结果，包含 objectKey、URL、文件大小等信息
     * @throws com.snowdrift.framework.oss.exception.OssException 上传失败时抛出
     */
    OssResult upload(OssUploadRequest request);
    
    /**
     * 下载文件
     * <p>
     * 根据 objectKey 下载文件，返回文件输入流
     * 调用方需要负责关闭输入流
     *
     * @param objectKey 对象键，文件的唯一标识
     * @return 文件输入流，调用方需要负责关闭
     * @throws com.snowdrift.framework.oss.exception.OssException 文件不存在或下载失败时抛出
     */
    InputStream download(String objectKey);
    
    /**
     * 删除文件
     * <p>
     * 根据 objectKey 删除 OSS 中的文件
     * 如果文件不存在，不会抛出异常
     *
     * @param objectKey 对象键，要删除的文件标识
     * @throws com.snowdrift.framework.oss.exception.OssException 删除失败时抛出
     */
    void delete(String objectKey);
    
    /**
     * 批量删除文件
     * <p>
     * 批量删除多个文件，内部会逐个删除
     * 如果某个文件删除失败，会记录日志但继续删除其他文件
     *
     * @param objectKeys 对象键列表，要删除的文件标识集合
     */
    void deleteBatch(List<String> objectKeys);
    
    /**
     * 判断文件是否存在
     * <p>
     * 检查指定 objectKey 的文件是否存在于 OSS 中
     *
     * @param objectKey 对象键，要检查的文件标识
     * @return true 如果文件存在，false 如果文件不存在
     */
    boolean exists(String objectKey);
    
    /**
     * 获取文件访问 URL
     * <p>
     * 根据 objectKey 生成文件的访问 URL
     * 对于私有 Bucket，会生成带签名的临时访问 URL
     * 对于公开 Bucket，直接返回公开访问 URL
     *
     * @param objectKey 对象键，文件标识
     * @param expiry    URL 有效期，仅对私有 Bucket 生效；公开 Bucket 可传 null
     * @return 文件访问 URL
     */
    String getUrl(String objectKey, Duration expiry);
    
    // ========== 分片上传（可选实现） ==========
    
    /**
     * 初始化分片上传
     * <p>
     * 开始一个分片上传任务，返回 Upload ID
     * 后续使用该 Upload ID 上传各个分片
     *
     * @param objectKey   对象键，文件标识
     * @param contentType 文件 MIME 类型，如 image/jpeg、video/mp4 等
     * @return Upload ID，用于后续分片上传和合并
     * @throws UnsupportedOperationException 当前存储类型不支持分片上传时抛出
     */
    default String initiateMultipartUpload(String objectKey, String contentType) {
        throw new UnsupportedOperationException("当前存储类型不支持分片上传");
    }
    
    /**
     * 生成分片上传预签名 URL
     * <p>
     * 为指定分片生成预签名上传 URL
     * 前端可直接使用该 URL 上传分片，无需经过应用服务器
     *
     * @param objectKey   对象键，文件标识
     * @param uploadId    Upload ID，由 initiateMultipartUpload 返回
     * @param partNumber  分片编号，从 1 开始递增
     * @param expiry      URL 有效期，过期后该 URL 不可用
     * @return 分片上传预签名 URL
     * @throws UnsupportedOperationException 当前存储类型不支持分片上传时抛出
     */
    default String generatePresignedUploadUrlForChunk(String objectKey, String uploadId, 
                                                      int partNumber, Duration expiry) {
        throw new UnsupportedOperationException("当前存储类型不支持分片上传");
    }
    
    /**
     * 合并分片
     * <p>
     * 所有分片上传完成后，调用此方法合并分片为完整文件
     * 合并成功后，分片会被自动清理
     *
     * @param objectKey 对象键，文件标识
     * @param uploadId  Upload ID，由 initiateMultipartUpload 返回
     * @param parts     分片列表，包含每个分片的信息（ETag、PartNumber 等）
     * @throws UnsupportedOperationException 当前存储类型不支持分片上传时抛出
     */
    default void completeMultipartUpload(String objectKey, String uploadId, List<?> parts) {
        throw new UnsupportedOperationException("当前存储类型不支持分片上传");
    }
    
    /**
     * 取消分片上传
     * <p>
     * 取消正在进行的分片上传任务，并删除所有已上传的分片
     * 应在用户上传失败或主动取消时调用，避免产生孤儿分片
     *
     * @param objectKey 对象键，文件标识
     * @param uploadId  Upload ID，由 initiateMultipartUpload 返回
     * @throws UnsupportedOperationException 当前存储类型不支持分片上传时抛出
     */
    default void abortMultipartUpload(String objectKey, String uploadId) {
        throw new UnsupportedOperationException("当前存储类型不支持分片上传");
    }
    
    // ========== 配置信息 ==========
    
    /**
     * 获取存储类型
     * <p>
     * 返回当前 OSS Service 的存储类型（本地、MinIO、阿里云等）
     *
     * @return OSS 存储类型枚举
     */
    OssTypeEnum getType();
    
    /**
     * 获取 Bucket 名称
     * <p>
     * 返回当前 OSS Service 配置的 Bucket 名称
     *
     * @return Bucket 名称
     */
    String getBucket();
    
    /**
     * 获取配置标识
     * <p>
     * 返回当前 OSS Service 的配置标识（如 default、backup 等）
     * 用于在 OssStrategyFactory 中区分不同的 OSS 实例
     *
     * @return 配置标识
     */
    String getConfigKey();
}
