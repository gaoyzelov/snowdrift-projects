package com.snowdrift.framework.oss.local.service;

import com.snowdrift.framework.oss.core.AbstractOssService;
import com.snowdrift.framework.oss.dto.OssConfigDTO;
import com.snowdrift.framework.oss.dto.OssResult;
import com.snowdrift.framework.oss.dto.OssUploadRequest;
import com.snowdrift.framework.oss.exception.OssException;
import com.snowdrift.framework.oss.util.OssUrlBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.lang.NonNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;

/**
 * 本地存储 OSS Service 实现
 *
 * @author 83674
 * @date 2026/5/9
 * @description 基于本地文件系统的 OSS 实现，适用于开发环境和小型项目
 * @since 1.0.0
 */
@Slf4j
public class LocalOssServiceImpl extends AbstractOssService {

    /**
     * 存储根目录
     */
    private final Path storageRoot;

    /**
     * 构造函数
     * <p>
     * 初始化本地存储 OSS Service
     * 会验证 endpoint 配置并创建存储根目录（如果不存在）
     *
     * @param config OSS 配置信息，包含 endpoint、domain、pathPrefix 等
     * @throws OssException 当 endpoint 为空或目录创建失败时抛出
     */
    public LocalOssServiceImpl(@NonNull OssConfigDTO config) {
        super(config);
        // 验证必要配置
        String endpoint = config.getEndpoint();

        if (StringUtils.isBlank(endpoint)) {
            throw new OssException("oss.local.endpoint.empty");
        }

        this.storageRoot = Paths.get(endpoint);
        initializeStorageDirectory();
    }

    /**
     * 初始化存储目录
     * <p>
     * 检查存储根目录是否存在，如果不存在则创建
     * 创建失败会抛出异常
     *
     * @throws OssException 当目录创建失败时抛出
     */
    private void initializeStorageDirectory() {
        try {
            if (!Files.exists(storageRoot)) {
                Files.createDirectories(storageRoot);
                log.info("创建本地存储目录: {}", storageRoot);
            }
        } catch (IOException e) {
            throw new OssException("oss.local.dir.create.failed", new Object[]{storageRoot});
        }
    }

    /**
     * 上传文件到本地存储
     * <p>
     * 将文件流保存到本地文件系统，支持自动创建父目录
     * 上传成功后返回文件信息，包括 objectKey、访问 URL、文件大小等
     *
     * @param request 上传请求，包含文件流、objectKey、contentType 等信息
     * @return 上传结果，包含 objectKey、URL、文件大小等信息
     * @throws OssException 当请求为空、文件流为空或保存失败时抛出
     */
    @Override
    public OssResult upload(@NonNull OssUploadRequest request) {
        // 校验请求参数
        request.validate();

        String objectKey = buildObjectKey(request.getObjectKey());
        Path targetPath = storageRoot.resolve(objectKey);

        try (InputStream inputStream = request.getInputStream()) {
            // 确保父目录存在
            Path parentDir = targetPath.getParent();
            if (parentDir != null && !Files.exists(parentDir)) {
                Files.createDirectories(parentDir);
            }
            long fileSize = Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);

            // 构建返回结果
            OssResult result = OssResult.builder()
                    .objectKey(objectKey)
                    .url(getUrl(objectKey, null))
                    .size(fileSize)
                    .build();

            log.debug("文件上传成功: objectKey={}, size={}", objectKey, fileSize);
            return result;
        } catch (IOException e) {
            log.error("文件上传失败: objectKey={}", objectKey, e);
            throw new OssException("oss.upload.failed", new Object[]{e.getMessage()});
        }
    }

    /**
     * 从本地存储下载文件
     * <p>
     * 根据 objectKey 从本地文件系统读取文件，返回文件输入流
     * 调用方需要负责关闭输入流
     *
     * @param objectKey 对象键，文件标识
     * @return 文件输入流，调用方需要负责关闭
     * @throws OssException 当文件不存在或读取失败时抛出
     */
    @Override
    public InputStream download(@NonNull String objectKey) {
        Path filePath = storageRoot.resolve(objectKey);

        if (!Files.exists(filePath)) {
            throw new OssException("oss.local.file.not.found", new Object[]{objectKey});
        }

        try {
            return Files.newInputStream(filePath);
        } catch (IOException e) {
            log.error("文件下载失败: objectKey={}", objectKey, e);
            throw new OssException("oss.download.failed", new Object[]{e.getMessage()});
        }
    }

    /**
     * 从本地存储删除文件
     * <p>
     * 根据 objectKey 从本地文件系统删除文件
     * 如果文件不存在，不会抛出异常
     *
     * @param objectKey 对象键，要删除的文件标识
     * @throws OssException 当删除失败时抛出
     */
    @Override
    public void delete(@NonNull String objectKey) {
        Path filePath = storageRoot.resolve(objectKey);

        try {
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                log.debug("文件删除成功: objectKey={}", objectKey);
            }
        } catch (IOException e) {
            log.error("文件删除失败: objectKey={}", objectKey, e);
            throw new OssException("oss.delete.failed", new Object[]{e.getMessage()});
        }
    }

    /**
     * 判断本地文件是否存在
     * <p>
     * 检查指定 objectKey 的文件是否存在于本地文件系统中
     *
     * @param objectKey 对象键，要检查的文件标识
     * @return true 如果文件存在，false 如果文件不存在
     */
    @Override
    public boolean exists(@NonNull String objectKey) {
        try {
            Path filePath = storageRoot.resolve(objectKey);
            return Files.exists(filePath);
        }catch (Exception e){
            log.error("检查文件存在性失败: objectKey={}", objectKey, e);
            throw new OssException("oss.exists.check.failed", new Object[]{e.getMessage()});
        }
    }

    /**
     * 获取文件访问 URL
     * <p>
     * 根据 objectKey 生成文件的访问 URL
     * 如果配置了 domain，返回 HTTP 访问 URL（需要通过静态资源映射访问）
     * 如果未配置 domain，返回本地文件路径的 URI
     *
     * @param objectKey 对象键，已经是完整路径（包含 path-prefix）
     * @param expiry    URL 有效期，本地存储不支持签名 URL，此参数忽略
     * @return 文件访问 URL
     */
    @Override
    public String getUrl(String objectKey, Duration expiry) {
        // 如果配置了域名，使用域名
        if (StringUtils.isNotBlank(config.getDomain())) {
            return OssUrlBuilder.buildUrl(config.getDomain(), objectKey);
        }

        // 否则返回本地文件路径
        return storageRoot.resolve(objectKey).toUri().toString();
    }

    /**
     * 关闭本地存储 OSS 客户端
     * <p>
     * 本地存储基于文件系统，无需释放连接池等资源
     * 该方法在应用关闭时由 OssStrategyFactory 统一调用
     * 此处仅做日志记录，便于追踪资源生命周期
     */
    @Override
    public void close() {
        log.info("本地存储无需关闭: configKey={}, endpoint={}", config.getConfigKey(), storageRoot);
    }
}
