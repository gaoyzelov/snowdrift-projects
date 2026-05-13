package com.snowdrift.framework.oss.core;

import com.snowdrift.framework.common.constant.StrConst;
import com.snowdrift.framework.oss.dto.OssConfigDTO;
import com.snowdrift.framework.oss.enums.OssTypeEnum;
import com.snowdrift.framework.oss.exception.OssException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

/**
 * OSS Service 抽象基类
 * <p>
 * 提供所有 OSS 实现的公共方法，避免代码重复
 * 包含 objectKey 规范化、批量删除、配置信息获取等通用逻辑
 *
 * @author 83674
 * @date 2026/5/10
 * @description OSS 服务抽象基类，封装公共逻辑
 * @since 1.0.0
 */
@Slf4j
public abstract class AbstractOssService implements IOssService {

    /**
     * OSS 配置信息
     */
    protected final OssConfigDTO config;

    /**
     * 构造函数
     * <p>
     * 初始化公共配置字段
     *
     * @param config OSS 配置信息
     */
    protected AbstractOssService(OssConfigDTO config) {
        this.config = config;
    }

    /**
     * 批量删除文件
     * <p>
     * 批量删除多个文件，内部会逐个删除
     * 如果某个文件删除失败，会记录警告日志但继续删除其他文件
     *
     * @param objectKeys 对象键列表，要删除的文件标识集合
     */
    @Override
    public void deleteBatch(List<String> objectKeys) {
        if (CollectionUtils.isEmpty(objectKeys)) {
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
     * 获取 Bucket 名称
     * <p>
     * 返回当前 OSS Service 配置的 Bucket 名称
     *
     * @return Bucket 名称
     */
    @Override
    public String getBucket() {
        return config.getBucket();
    }

    /**
     * 获取配置标识
     * <p>
     * 返回当前 OSS Service 的配置标识（如 default、backup 等）
     * 用于在 OssStrategyFactory 中区分不同的 OSS 实例
     *
     * @return 配置标识
     */
    @Override
    public String getConfigKey() {
        return config.getConfigKey();
    }

    /**
     * 获取 OSS 类型
     * <p>
     * 返回当前 OSS Service 的类型（如 Aliyun、Qiniu 等）
     *
     * @return OSS 类型
     */
    @Override
    public OssTypeEnum getType() {
        return config.getOssType();
    }

    /**
     * 构建完整的 objectKey
     * <p>
     * 将原始 objectKey 规范化并添加配置的路径前缀
     *
     * @param objectKey 原始对象 Key，不能为空
     * @return 完整的对象 Key（包含路径前缀）
     * @throws OssException 当 objectKey 为空时抛出
     */
    protected String buildObjectKey(String objectKey) {
        String key = normalizeObjectKey(objectKey);
        String prefix = config.getPathPrefix();

        if (StringUtils.isNotBlank(prefix)) {
            String prefixPath = prefix.endsWith(StrConst.SLASH) ? prefix : prefix + StrConst.SLASH;
            return prefixPath + key;
        }
        return key;
    }

    /**
     * 规范化对象 Key
     * <p>
     * 移除开头的斜杠，将反斜杠替换为正斜杠
     * 保证所有平台的路径一致性
     *
     * @param objectKey 对象 Key，不能为空
     * @return 规范化后的对象 Key
     * @throws OssException 当 objectKey 为空时抛出
     */
    protected String normalizeObjectKey(String objectKey) {
        if (StringUtils.isBlank(objectKey)) {
            throw new OssException("oss.object.key.empty");
        }

        // 移除开头的斜杠
        String key = objectKey.startsWith(StrConst.SLASH) ? objectKey.substring(1) : objectKey;
        // 将 Windows 反斜杠替换为正斜杠
        key = key.replace("\\", StrConst.SLASH);

        return key;
    }
}
