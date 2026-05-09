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
     *
     * @param request 上传请求
     * @return 上传结果
     */
    OssResult upload(OssUploadRequest request);
    
    /**
     * 下载文件
     *
     * @param objectKey 对象键
     * @return 文件输入流
     */
    InputStream download(String objectKey);
    
    /**
     * 删除文件
     *
     * @param objectKey 对象键
     */
    void delete(String objectKey);
    
    /**
     * 批量删除文件
     *
     * @param objectKeys 对象键列表
     */
    void deleteBatch(List<String> objectKeys);
    
    /**
     * 判断文件是否存在
     *
     * @param objectKey 对象键
     * @return 是否存在
     */
    boolean exists(String objectKey);
    
    /**
     * 获取文件访问 URL
     *
     * @param objectKey 对象键
     * @param expiry    有效期（私有 Bucket 需要签名，公开 Bucket 可为 null）
     * @return 文件访问 URL
     */
    String getUrl(String objectKey, Duration expiry);
    
    // ========== 分片上传（可选实现） ==========
    
    /**
     * 初始化分片上传
     *
     * @param objectKey   对象键
     * @param contentType 文件类型
     * @return Upload ID
     */
    default String initiateMultipartUpload(String objectKey, String contentType) {
        throw new UnsupportedOperationException("当前存储类型不支持分片上传");
    }
    
    /**
     * 生成分片上传预签名 URL
     *
     * @param objectKey   对象键
     * @param uploadId    Upload ID
     * @param partNumber  分片编号（从 1 开始）
     * @param expiry      有效期
     * @return 分片上传 URL
     */
    default String generatePresignedUploadUrlForChunk(String objectKey, String uploadId, 
                                                      int partNumber, Duration expiry) {
        throw new UnsupportedOperationException("当前存储类型不支持分片上传");
    }
    
    /**
     * 合并分片
     *
     * @param objectKey 对象键
     * @param uploadId  Upload ID
     * @param parts     分片列表
     */
    default void completeMultipartUpload(String objectKey, String uploadId, List<?> parts) {
        throw new UnsupportedOperationException("当前存储类型不支持分片上传");
    }
    
    /**
     * 取消分片上传（会删除所有已上传的分片）
     *
     * @param objectKey 对象键
     * @param uploadId  Upload ID
     */
    default void abortMultipartUpload(String objectKey, String uploadId) {
        throw new UnsupportedOperationException("当前存储类型不支持分片上传");
    }
    
    // ========== 配置信息 ==========
    
    /**
     * 获取存储类型
     *
     * @return OSS 类型
     */
    OssTypeEnum getType();
    
    /**
     * 获取 Bucket 名称
     *
     * @return Bucket 名称
     */
    String getBucket();
    
    /**
     * 获取配置标识
     *
     * @return 配置标识
     */
    String getConfigKey();
}
