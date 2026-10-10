package com.snowdrift.framework.oss.qiniu.config;

import com.snowdrift.framework.oss.core.IOssService;
import com.snowdrift.framework.oss.core.OssServiceRegistration;
import com.snowdrift.framework.oss.dto.OssConfigDTO;
import com.snowdrift.framework.oss.enums.OssTypeEnum;
import com.snowdrift.framework.oss.qiniu.service.QiniuOssServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SnowdriftOssQiniuConfigurationTest
 *
 * @author gaoyzelov
 * @date 2026/10/10
 * @description 七牛云自动配置黑盒测试：注册信息声明 QINIU 类型，creator 据配置产出 QiniuOssServiceImpl
 * @since 1.0.0
 */
class SnowdriftOssQiniuConfigurationTest {

    @Test
    @DisplayName("qiniuRegistration：类型为 QINIU，creator 产出 QiniuOssServiceImpl")
    void registrationProducesQiniuService() throws Exception {
        OssServiceRegistration registration = new SnowdriftOssQiniuConfiguration().qiniuRegistration();
        assertThat(registration.getType()).isEqualTo(OssTypeEnum.QINIU);

        OssConfigDTO config = new OssConfigDTO();
        config.setConfigKey("qiniu");
        config.setOssType(OssTypeEnum.QINIU);
        config.setAccessKey("ak");
        config.setSecretKey("sk");
        config.setBucket("test-bucket");
        config.setDomain("https://cdn.example.com");

        IOssService service = registration.getCreator().create(config);
        try {
            assertThat(service).isInstanceOf(QiniuOssServiceImpl.class);
        } finally {
            service.close();
        }
    }
}
