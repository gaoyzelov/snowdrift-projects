package com.snowdrift.framework.oss.core;

import com.snowdrift.framework.oss.enums.OssTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * OSS 服务注册信息
 * <p>
 * 各存储提供者模块通过注册此 Bean 声明自己支持的存储类型和创建器，
 * 由 {@link com.snowdrift.framework.oss.config.SnowdriftOssConfiguration} 统一收集后批量注册到策略工厂。
 * </p>
 *
 * @author gaoyzelov
 * @date 2026/7/19
 * @since 1.0.0
 */
@Data
@AllArgsConstructor
public class OssServiceRegistration {

    /** 存储类型 */
    private OssTypeEnum type;

    /** 服务创建器 */
    private OssStrategyFactory.ServiceCreator creator;
}
