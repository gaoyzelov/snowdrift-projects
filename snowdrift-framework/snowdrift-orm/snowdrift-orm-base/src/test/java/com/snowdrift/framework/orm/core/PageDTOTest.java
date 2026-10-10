package com.snowdrift.framework.orm.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PageDTOTest
 *
 * @author gaoyzelov
 * @date 2026/10/10
 * @description 分页参数默认值黑盒测试：pageNum 默认 1、pageSize 默认 10，可被覆盖
 * @since 1.0.0
 */
class PageDTOTest {

    @Test
    @DisplayName("默认页码为 1、默认页大小为 10")
    void defaults() {
        PageDTO dto = new PageDTO();

        assertThat(dto.getPageNum()).isEqualTo(1L);
        assertThat(dto.getPageSize()).isEqualTo(10L);
    }

    @Test
    @DisplayName("setter 覆盖默认值")
    void settersOverrideDefaults() {
        PageDTO dto = new PageDTO();
        dto.setPageNum(3L);
        dto.setPageSize(50L);

        assertThat(dto.getPageNum()).isEqualTo(3L);
        assertThat(dto.getPageSize()).isEqualTo(50L);
    }
}
