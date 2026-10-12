package com.snowdrift.framework.oss.dto;

import com.snowdrift.framework.oss.properties.OssInstanceProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SecretRedactionTest
 *
 * @author gaoyzelov
 * @date 2026/10/12
 * @description OSS 凭据字段必须排除出 toString：下游一行 log.info("config={}", dto) 就会把密钥写进日志
 * @since 1.0.0
 */
class SecretRedactionTest {

    private static final String ACCESS_KEY = "AKIA-EXAMPLE-ACCESS";
    private static final String SECRET_KEY = "EXAMPLE-SECRET-VALUE";

    @Test
    @DisplayName("OssConfigDTO.toString 不包含 accessKey / secretKey")
    void configDtoToStringExcludesSecrets() {
        OssConfigDTO dto = new OssConfigDTO();
        dto.setConfigKey("default");
        dto.setAccessKey(ACCESS_KEY);
        dto.setSecretKey(SECRET_KEY);

        assertThat(dto.toString())
                .doesNotContain(ACCESS_KEY)
                .doesNotContain(SECRET_KEY)
                // 非敏感字段仍应保留，便于排障
                .contains("configKey");
    }

    @Test
    @DisplayName("OssInstanceProperties.toString 不包含 accessKey / secretKey")
    void instancePropertiesToStringExcludesSecrets() {
        OssInstanceProperties properties = new OssInstanceProperties();
        properties.setBucket("bkt");
        properties.setAccessKey(ACCESS_KEY);
        properties.setSecretKey(SECRET_KEY);

        assertThat(properties.toString())
                .doesNotContain(ACCESS_KEY)
                .doesNotContain(SECRET_KEY)
                .contains("bucket");
    }
}
