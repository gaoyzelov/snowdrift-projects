package com.snowdrift.framework.oss.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * UrlStyleEnumTest
 *
 * @author gaoyzelov
 * @date 2026/10/10
 * @description URL 风格枚举黑盒测试：code 契约、of(code) 命中与未命中回退 PATH_STYLE
 * @since 1.0.0
 */
class UrlStyleEnumTest {

    @Test
    @DisplayName("code/note 契约稳定，共两种风格")
    void codeContract() {
        assertThat(UrlStyleEnum.VIRTUAL_HOST.getCode()).isEqualTo("virtual_host");
        assertThat(UrlStyleEnum.PATH_STYLE.getCode()).isEqualTo("path_style");
        assertThat(UrlStyleEnum.values()).hasSize(2);
    }

    @Test
    @DisplayName("of：命中返回对应枚举")
    void ofMatches() {
        assertThat(UrlStyleEnum.of("virtual_host")).isEqualTo(UrlStyleEnum.VIRTUAL_HOST);
        assertThat(UrlStyleEnum.of("path_style")).isEqualTo(UrlStyleEnum.PATH_STYLE);
    }

    @Test
    @DisplayName("of：未命中或 null 回退默认 PATH_STYLE（fail-safe）")
    void ofFallsBackToPathStyle() {
        assertThat(UrlStyleEnum.of("unknown")).isEqualTo(UrlStyleEnum.PATH_STYLE);
        assertThat(UrlStyleEnum.of(null)).isEqualTo(UrlStyleEnum.PATH_STYLE);
    }
}
