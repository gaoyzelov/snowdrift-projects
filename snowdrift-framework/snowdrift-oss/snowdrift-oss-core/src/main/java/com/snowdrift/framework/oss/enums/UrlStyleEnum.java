package com.snowdrift.framework.oss.enums;

import com.snowdrift.framework.common.enums.IEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Optional;

/**
 * URL 风格枚举
 *
 * @author 83674
 * @date 2026/5/9
 * @description OSS URL 访问风格
 * @since 1.0.0
 */
@Getter
@AllArgsConstructor
public enum UrlStyleEnum implements IEnum<String> {
    
    /**
     * 虚拟主机风格
     * 格式：https://bucket.endpoint/objectKey
     * 示例：https://my-bucket.oss-cn-hangzhou.aliyuncs.com/photo.jpg
     */
    VIRTUAL_HOST("virtual_host", "虚拟主机风格"),
    
    /**
     * 路径风格
     * 格式：https://endpoint/bucket/objectKey
     * 示例：https://oss-cn-hangzhou.aliyuncs.com/my-bucket/photo.jpg
     */
    PATH_STYLE("path_style", "路径风格");
    
    /**
     * 风格标识
     */
    private final String code;
    
    /**
     * 风格名称
     */
    private final String note;
    
    /**
     * 根据 code 获取枚举
     */
    public static Optional<UrlStyleEnum> getByCode(String code) {
        return IEnum.getByCode(UrlStyleEnum.class, code);
    }
    
    /**
     * 根据 code 获取枚举（找不到时返回默认值 PATH_STYLE）
     */
    public static UrlStyleEnum fromCode(String code) {
        return getByCode(code).orElse(PATH_STYLE);
    }
}
