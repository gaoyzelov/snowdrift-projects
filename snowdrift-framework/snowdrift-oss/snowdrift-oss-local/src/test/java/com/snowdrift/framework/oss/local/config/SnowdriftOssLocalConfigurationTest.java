package com.snowdrift.framework.oss.local.config;

import com.snowdrift.framework.oss.core.IOssService;
import com.snowdrift.framework.oss.core.OssServiceRegistration;
import com.snowdrift.framework.oss.dto.OssConfigDTO;
import com.snowdrift.framework.oss.enums.OssTypeEnum;
import com.snowdrift.framework.oss.local.service.LocalOssServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SnowdriftOssLocalConfigurationTest
 *
 * @author gaoyzelov
 * @date 2026/10/10
 * @description 本地存储自动配置黑盒测试：注册信息声明 LOCAL 类型，creator 据配置产出 LocalOssServiceImpl
 * @since 1.0.0
 */
class SnowdriftOssLocalConfigurationTest {

    @Test
    @DisplayName("localRegistration：类型为 LOCAL，creator 产出 LocalOssServiceImpl")
    void registrationProducesLocalService(@TempDir Path tempDir) {
        OssServiceRegistration registration = new SnowdriftOssLocalConfiguration().localRegistration();

        assertThat(registration.getType()).isEqualTo(OssTypeEnum.LOCAL);

        OssConfigDTO config = new OssConfigDTO();
        config.setConfigKey("local");
        config.setOssType(OssTypeEnum.LOCAL);
        config.setEndpoint(tempDir.toString());
        config.setDomain("http://files.local");

        IOssService service = registration.getCreator().create(config);

        assertThat(service).isInstanceOf(LocalOssServiceImpl.class);
        assertThat(service.getType()).isEqualTo(OssTypeEnum.LOCAL);
    }
}
