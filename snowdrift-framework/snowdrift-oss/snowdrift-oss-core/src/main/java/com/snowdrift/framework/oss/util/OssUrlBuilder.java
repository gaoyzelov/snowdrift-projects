package com.snowdrift.framework.oss.util;

import com.snowdrift.framework.oss.exception.OssException;
import org.apache.commons.lang3.StringUtils;

/**
 * OSS URL 构建工具类
 * <p>
 * 统一处理 OSS 访问 URL 的拼接逻辑
 * 自动处理域名/端点中的斜杠，避免路径错误
 *
 * @author 83674
 * @date 2026/5/9
 * @description OSS URL 构建工具
 * @since 1.0.0
 */
public class OssUrlBuilder {

    /**
     * 私有构造函数，防止实例化
     */
    private OssUrlBuilder() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * 构建域名 URL
     * <p>
     * 适用于：已配置 CDN 域名或公网域名的场景
     * 格式：{domain}/{objectKey}
     * <p>
     * 示例：
     * - buildUrl("https://cdn.example.com", "images/photo.jpg")
     *   → "https://cdn.example.com/images/photo.jpg"
     * - buildUrl("http://localhost:8080/files/", "/avatar.png")
     *   → "http://localhost:8080/files/avatar.png"
     *
     * @param domain    域名，不能为空
     * @param objectKey 对象键，不能为空
     * @return 完整的访问 URL
     * @throws OssException 当 domain 或 objectKey 为空时抛出
     */
    public static String buildUrl(String domain, String objectKey) {
        if (StringUtils.isBlank(domain)) {
            throw new OssException("oss.url.domain.empty");
        }
        if (StringUtils.isBlank(objectKey)) {
            throw new OssException("oss.object.key.empty");
        }

        String normalizedDomain = removeTrailingSlash(domain);
        String normalizedKey = removeLeadingSlash(objectKey);

        return normalizedDomain + "/" + normalizedKey;
    }

    /**
     * 构建虚拟主机风格 URL
     * <p>
     * 适用于：阿里云、腾讯云等支持虚拟主机风格的 OSS
     * 格式：https://{bucket}.{endpoint}/{objectKey}
     * <p>
     * 示例：
     * - buildVirtualHostUrl("my-bucket", "oss-cn-hangzhou.aliyuncs.com", "images/photo.jpg")
     *   → "https://my-bucket.oss-cn-hangzhou.aliyuncs.com/images/photo.jpg"
     * - buildVirtualHostUrl("my-bucket", "http://cos.ap-beijing.myqcloud.com", "avatar.png")
     *   → "https://my-bucket.cos.ap-beijing.myqcloud.com/avatar.png"
     *
     * @param bucket    Bucket 名称，不能为空
     * @param endpoint  端点（可包含或不包含协议），不能为空
     * @param objectKey 对象键，不能为空
     * @return 完整的访问 URL（始终使用 https）
     * @throws OssException 当参数为空时抛出
     */
    public static String buildVirtualHostUrl(String bucket, String endpoint, String objectKey) {
        if (StringUtils.isBlank(bucket)) {
            throw new OssException("oss.url.bucket.empty");
        }
        if (StringUtils.isBlank(endpoint)) {
            throw new OssException("oss.url.endpoint.empty");
        }
        if (StringUtils.isBlank(objectKey)) {
            throw new OssException("oss.object.key.empty");
        }

        // 移除协议前缀
        String normalizedEndpoint = removeProtocol(endpoint);
        normalizedEndpoint = removeTrailingSlash(normalizedEndpoint);
        String normalizedKey = removeLeadingSlash(objectKey);

        return "https://" + bucket + "." + normalizedEndpoint + "/" + normalizedKey;
    }

    /**
     * 构建路径风格 URL
     * <p>
     * 适用于：MinIO 等使用路径风格的 OSS
     * 格式：{endpoint}/{bucket}/{objectKey}
     * <p>
     * 示例：
     * - buildPathStyleUrl("http://localhost:9000", "my-bucket", "images/photo.jpg")
     *   → "http://localhost:9000/my-bucket/images/photo.jpg"
     *
     * @param endpoint  端点（必须包含协议），不能为空
     * @param bucket    Bucket 名称，不能为空
     * @param objectKey 对象键，不能为空
     * @return 完整的访问 URL
     * @throws OssException 当参数为空时抛出
     */
    public static String buildPathStyleUrl(String endpoint, String bucket, String objectKey) {
        if (StringUtils.isBlank(endpoint)) {
            throw new OssException("oss.url.endpoint.empty");
        }
        if (StringUtils.isBlank(bucket)) {
            throw new OssException("oss.url.bucket.empty");
        }
        if (StringUtils.isBlank(objectKey)) {
            throw new OssException("oss.object.key.empty");
        }

        String normalizedEndpoint = removeTrailingSlash(endpoint);
        String normalizedKey = removeLeadingSlash(objectKey);

        return normalizedEndpoint + "/" + bucket + "/" + normalizedKey;
    }

    /**
     * 移除字符串末尾的斜杠
     *
     * @param str 原始字符串
     * @return 移除末尾斜杠后的字符串
     */
    private static String removeTrailingSlash(String str) {
        if (str == null) {
            return null;
        }
        return str.endsWith("/") ? str.substring(0, str.length() - 1) : str;
    }

    /**
     * 移除字符串开头的斜杠
     *
     * @param str 原始字符串
     * @return 移除开头斜杠后的字符串
     */
    private static String removeLeadingSlash(String str) {
        if (str == null) {
            return null;
        }
        return str.startsWith("/") ? str.substring(1) : str;
    }

    /**
     * 移除协议前缀（http:// 或 https://）
     *
     * @param str 原始字符串
     * @return 移除协议后的字符串
     */
    private static String removeProtocol(String str) {
        if (str == null) {
            return null;
        }
        if (str.startsWith("http://")) {
            return str.substring(7);
        }
        if (str.startsWith("https://")) {
            return str.substring(8);
        }
        return str;
    }
}
