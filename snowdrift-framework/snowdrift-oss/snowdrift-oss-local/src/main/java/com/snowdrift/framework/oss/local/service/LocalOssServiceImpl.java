package com.snowdrift.framework.oss.local.service;

import com.snowdrift.framework.oss.core.IOssService;
import com.snowdrift.framework.oss.dto.OssConfigDTO;
import com.snowdrift.framework.oss.dto.OssResult;
import com.snowdrift.framework.oss.dto.OssUploadRequest;
import com.snowdrift.framework.oss.enums.OssTypeEnum;
import com.snowdrift.framework.oss.exception.OssException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;


import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.List;

/**
 * 本地存储 OSS Service 实现
 *
 * @author 83674
 * @date 2026/5/9
 * @description 基于本地文件系统的 OSS 实现，适用于开发环境和小型项目
 * @since 1.0.0
 */
@Slf4j
public class LocalOssServiceImpl implements IOssService {

    /**
     * OSS 配置
     */
    private final OssConfigDTO config;

    /**
     * 存储根目录
     */
    private final Path storageRoot;

    /**
     * 访问域名
     */
    private final String domain;

    /**
     * 构造函数
     *
     * @param config OSS 配置
     */
    public LocalOssServiceImpl(OssConfigDTO config) {
        this.config = config;
        this.domain = config.getDomain();

        // 初始化存储根目录
        String endpoint = config.getEndpoint();
        if (StringUtils.isBlank(endpoint)) {
            throw new OssException("oss.local.endpoint.empty");
        }

        this.storageRoot = Paths.get(endpoint);
        initializeStorageDirectory();

        log.info("本地存储初始化完成: root={}, domain={}", storageRoot, domain);
    }

    /**
     * 初始化存储目录
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

    @Override
    public OssResult upload(OssUploadRequest request) {
        if (request == null) {
            throw new OssException("oss.upload.request.null");
        }
        if (request.getInputStream() == null) {
            throw new OssException("oss.upload.inputstream.null");
        }

        String objectKey = buildObjectKey(request.getObjectKey());
        Path targetPath = storageRoot.resolve(objectKey);

        try {
            // 确保父目录存在
            Path parentDir = targetPath.getParent();
            if (parentDir != null && !Files.exists(parentDir)) {
                Files.createDirectories(parentDir);
            }

            // 复制文件
            long fileSize = Files.copy(request.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            // 构建返回结果
            OssResult result = OssResult.builder()
                    .objectKey(objectKey)
                    .url(getUrl(objectKey, null))
                    .bucket(config.getBucket())
                    .size(fileSize)
                    .build();

            log.info("文件上传成功: objectKey={}, size={}", objectKey, fileSize);
            return result;

        } catch (IOException e) {
            log.error("文件上传失败: objectKey={}", objectKey, e);
            throw new OssException("oss.upload.failed", new Object[]{e.getMessage()});
        }
    }

    @Override
    public InputStream download(String objectKey) {
        Path filePath = storageRoot.resolve(buildObjectKey(objectKey));

        if (!Files.exists(filePath)) {
            throw new OssException("oss.file.not.exists", new Object[]{objectKey});
        }

        try {
            return Files.newInputStream(filePath);
        } catch (IOException e) {
            log.error("文件下载失败: objectKey={}", objectKey, e);
            throw new OssException("oss.download.failed", new Object[]{e.getMessage()});
        }
    }

    @Override
    public void delete(String objectKey) {
        Path filePath = storageRoot.resolve(buildObjectKey(objectKey));

        try {
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                log.info("文件删除成功: objectKey={}", objectKey);
            }
        } catch (IOException e) {
            log.error("文件删除失败: objectKey={}", objectKey, e);
            throw new OssException("oss.delete.failed", new Object[]{e.getMessage()});
        }
    }

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

    @Override
    public boolean exists(String objectKey) {
        Path filePath = storageRoot.resolve(buildObjectKey(objectKey));
        return Files.exists(filePath);
    }

    @Override
    public String getUrl(String objectKey, Duration expiry) {
        String key = buildObjectKey(objectKey);

        // 如果配置了域名，使用域名
        if (StringUtils.isNotBlank(domain)) {
            String url = domain.endsWith("/") ? domain : domain + "/";
            return url + key;
        }

        // 否则返回本地文件路径
        return storageRoot.resolve(key).toUri().toString();
    }

    @Override
    public OssTypeEnum getType() {
        return OssTypeEnum.LOCAL;
    }

    @Override
    public String getBucket() {
        return config.getBucket();
    }

    @Override
    public String getConfigKey() {
        return config.getConfigKey();
    }

    /**
     * 构建完整的 objectKey（包含路径前缀）
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
