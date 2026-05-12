package com.snowdrift.framework.oss.enums;

import com.snowdrift.framework.common.enums.IEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Optional;

/**
 * OSS 存储类型枚举
 *
 * @author 83674
 * @date 2026/5/9
 * @description 支持的 OSS 存储类型
 * @since 1.0.0
 */
@Getter
@AllArgsConstructor
public enum OssTypeEnum implements IEnum<String> {
    
    /**
     * MinIO（自建对象存储）
     */
    MINIO("minio", "MinIO"),
    
    /**
     * 阿里云 OSS
     */
    ALIYUN("aliyun", "阿里云 OSS"),
    
    /**
     * 七牛云 Kodo
     */
    QINIU("qiniu", "七牛云"),
    
    /**
     * 腾讯云 COS
     */
    TENCENT("tencent", "腾讯云 COS"),
    
    /**
     * 本地存储
     */
    LOCAL("local", "本地存储");
    
    /**
     * 类型标识
     */
    private final String code;
    
    /**
     * 类型名称
     */
    private final String note;
    
    /**
     *
     * @return 类型标识
     */
    public String getCode() {
        return code;
    }
    
    /**
     * 根据 code 获取枚举
     */
    public static Optional<OssTypeEnum> getByCode(String code) {
        return IEnum.getByCode(OssTypeEnum.class, code);
    }
    
    /**
     * 根据 code 获取枚举（找不到时抛出异常）
     */
    public static OssTypeEnum fromCode(String code) {
        return getByCode(code).orElseThrow(() -> 
            new IllegalArgumentException("未知的 OSS 类型: " + code));
    }
}
