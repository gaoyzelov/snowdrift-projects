package com.snowdrift.framework.oss.minio.config;

import com.snowdrift.framework.oss.core.OssServiceRegistration;
import com.snowdrift.framework.oss.enums.OssTypeEnum;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SnowdriftOssMinioConfigurationTest
 *
 * @author gaoyzelov
 * @date 2026/10/10
 * @description MinIO 自动配置黑盒测试：注册信息声明 MINIO 类型且携带 creator。
 * 注：不调用 creator.create —— MinIO 构造会做真实联网桶检查，需真实服务（见 MinioOssServiceImplTest 说明）。
 * @since 1.0.0
 */
class SnowdriftOssMinioConfigurationTest {

    @Test
    @DisplayName("minioRegistration：类型为 MINIO 且 creator 非空")
    void registrationDeclaresMinioType() {
        OssServiceRegistration registration = new SnowdriftOssMinioConfiguration().minioRegistration();

        assertThat(registration.getType()).isEqualTo(OssTypeEnum.MINIO);
        assertThat(registration.getCreator()).isNotNull();
    }
}
