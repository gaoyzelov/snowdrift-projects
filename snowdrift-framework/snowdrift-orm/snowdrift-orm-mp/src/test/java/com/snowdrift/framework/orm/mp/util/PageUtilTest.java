package com.snowdrift.framework.orm.mp.util;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.snowdrift.framework.orm.core.PageDTO;
import com.snowdrift.framework.orm.core.PageData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PageUtilTest
 *
 * @author gaoyzelov
 * @date 2026/10/10
 * @description 分页工具黑盒测试：PageDTO→Page 转换与 Page→PageData 映射
 * @since 1.0.0
 */
class PageUtilTest {

    @Test
    @DisplayName("of：把 PageDTO 的页码/页大小转换为 MyBatis-Plus Page")
    void convertsDtoToPage() {
        PageDTO dto = new PageDTO();
        dto.setPageNum(2L);
        dto.setPageSize(20L);

        Page<Object> page = PageUtil.of(dto);

        assertThat(page.getCurrent()).isEqualTo(2L);
        assertThat(page.getSize()).isEqualTo(20L);
    }

    @Test
    @DisplayName("of：使用 PageDTO 默认值（1/10）")
    void convertsDefaultDto() {
        Page<Object> page = PageUtil.of(new PageDTO());

        assertThat(page.getCurrent()).isEqualTo(1L);
        assertThat(page.getSize()).isEqualTo(10L);
    }

    @Test
    @DisplayName("null 兜底：pageDTO 为 null 时兜底为 1/10，不抛 NPE")
    void nullDtoFallsBackToDefaults() {
        Page<Object> page = PageUtil.of(null);

        assertThat(page.getCurrent()).isEqualTo(1L);
        assertThat(page.getSize()).isEqualTo(10L);
    }

    @Test
    @DisplayName("null 兜底：pageNum/pageSize 为 null 时分别兜底，非 null 值保留")
    void nullFieldsFallBackIndividually() {
        PageDTO nullNum = new PageDTO();
        nullNum.setPageNum(null);
        nullNum.setPageSize(20L);
        Page<Object> p1 = PageUtil.of(nullNum);
        assertThat(p1.getCurrent()).isEqualTo(1L);
        assertThat(p1.getSize()).isEqualTo(20L);

        PageDTO nullSize = new PageDTO();
        nullSize.setPageNum(3L);
        nullSize.setPageSize(null);
        Page<Object> p2 = PageUtil.of(nullSize);
        assertThat(p2.getCurrent()).isEqualTo(3L);
        assertThat(p2.getSize()).isEqualTo(10L);

        PageDTO bothNull = new PageDTO();
        bothNull.setPageNum(null);
        bothNull.setPageSize(null);
        Page<Object> p3 = PageUtil.of(bothNull);
        assertThat(p3.getCurrent()).isEqualTo(1L);
        assertThat(p3.getSize()).isEqualTo(10L);
    }

    @Test
    @DisplayName("toPageData：映射总数与记录列表")
    void mapsPageToPageData() {
        Page<String> page = new Page<>(1, 10);
        page.setTotal(42L);
        page.setRecords(List.of("a", "b"));

        PageData<String> data = PageUtil.toPageData(page);

        assertThat(data.getTotal()).isEqualTo(42L);
        assertThat(data.getRecords()).containsExactly("a", "b");
    }

    @Test
    @DisplayName("toPageData：空记录时映射为空列表而非 null")
    void mapsEmptyRecords() {
        Page<String> page = new Page<>(1, 10);
        page.setTotal(0L);

        PageData<String> data = PageUtil.toPageData(page);

        assertThat(data.getTotal()).isZero();
        assertThat(data.getRecords()).isEmpty();
    }
}
