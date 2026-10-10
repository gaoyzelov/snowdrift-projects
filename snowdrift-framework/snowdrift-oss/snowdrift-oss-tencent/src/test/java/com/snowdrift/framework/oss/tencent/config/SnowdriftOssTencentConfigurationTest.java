package com.snowdrift.framework.oss.tencent.config;

import com.snowdrift.framework.oss.core.IOssService;
import com.snowdrift.framework.oss.core.OssServiceRegistration;
import com.snowdrift.framework.oss.dto.OssConfigDTO;
import com.snowdrift.framework.oss.enums.OssTypeEnum;
import com.snowdrift.framework.oss.tencent.service.TencentOssServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SnowdriftOssTencentConfigurationTest
 *
 * @author gaoyzelov
 * @date 2026/10/10
 * @description 腾讯云 COS 自动配置黑盒测试：注册信息声明 TENCENT 类型，creator 据配置产出 TencentOssServiceImpl
 * @since 1.0.0
 */
class SnowdriftOssTencentConfigurationTest {

    @Test
    @DisplayName("tencentRegistration：类型为 TENCENT，creator 产出 TencentOssServiceImpl")
    void registrationProducesTencentService() throws Exception {
        OssServiceRegistration registration = new SnowdriftOssTencentConfiguration().tencentRegistration();
        assertThat(registration.getType()).isEqualTo(OssTypeEnum.TENCENT);

        OssConfigDTO config = new OssConfigDTO();
        config.setConfigKey("tencent");
        config.setOssType(OssTypeEnum.TENCENT);
        config.setAccessKey("ak");
        config.setSecretKey("sk");
        config.setBucket("test-bucket");
        config.setDomain("https://cdn.example.com");
        config.setRegion("ap-beijing");

        IOssService service = registration.getCreator().create(config);
        try {
            assertThat(service).isInstanceOf(TencentOssServiceImpl.class);
        } finally {
            service.close();
        }
    }
}
