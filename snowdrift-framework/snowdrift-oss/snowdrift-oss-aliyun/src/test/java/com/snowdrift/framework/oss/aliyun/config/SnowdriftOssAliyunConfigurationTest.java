package com.snowdrift.framework.oss.aliyun.config;

import com.snowdrift.framework.oss.aliyun.service.AliyunOssServiceImpl;
import com.snowdrift.framework.oss.core.IOssService;
import com.snowdrift.framework.oss.core.OssServiceRegistration;
import com.snowdrift.framework.oss.dto.OssConfigDTO;
import com.snowdrift.framework.oss.enums.OssTypeEnum;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SnowdriftOssAliyunConfigurationTest
 *
 * @author gaoyzelov
 * @date 2026/10/10
 * @description 阿里云 OSS 自动配置黑盒测试：注册信息声明 ALIYUN 类型，creator 据配置产出 AliyunOssServiceImpl
 * @since 1.0.0
 */
class SnowdriftOssAliyunConfigurationTest {

    @Test
    @DisplayName("aliyunRegistration：类型为 ALIYUN，creator 产出 AliyunOssServiceImpl")
    void registrationProducesAliyunService() throws Exception {
        OssServiceRegistration registration = new SnowdriftOssAliyunConfiguration().aliyunRegistration();
        assertThat(registration.getType()).isEqualTo(OssTypeEnum.ALIYUN);

        OssConfigDTO config = new OssConfigDTO();
        config.setConfigKey("aliyun");
        config.setOssType(OssTypeEnum.ALIYUN);
        config.setEndpoint("https://oss-cn-hangzhou.aliyuncs.com");
        config.setAccessKey("ak");
        config.setSecretKey("sk");
        config.setBucket("test-bucket");

        IOssService service = registration.getCreator().create(config);
        try {
            assertThat(service).isInstanceOf(AliyunOssServiceImpl.class);
        } finally {
            service.close();
        }
    }
}
