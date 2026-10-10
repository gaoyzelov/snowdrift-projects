package com.snowdrift.framework.oss.core;

import com.snowdrift.framework.oss.dto.OssConfigDTO;
import com.snowdrift.framework.oss.enums.OssTypeEnum;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * OssServiceRegistrationTest
 *
 * @author gaoyzelov
 * @date 2026/10/10
 * @description OSS 服务注册信息黑盒测试：type/creator 载体语义，creator 能据配置产出实例
 * @since 1.0.0
 */
class OssServiceRegistrationTest {

    @Test
    @DisplayName("承载存储类型与创建器；creator.create 产出实例")
    void holdsTypeAndCreator() {
        IOssService service = mock(IOssService.class);
        OssServiceRegistration registration =
                new OssServiceRegistration(OssTypeEnum.MINIO, config -> service);

        assertThat(registration.getType()).isEqualTo(OssTypeEnum.MINIO);
        assertThat(registration.getCreator()).isNotNull();
        assertThat(registration.getCreator().create(new OssConfigDTO())).isSameAs(service);
    }
}
