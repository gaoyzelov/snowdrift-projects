package com.snowdrift.framework.web.xss;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SimpleXssCleanerTest
 *
 * @author gaoyzelov
 * @date 2026/10/10
 * @description 默认 XSS 清洗器黑盒测试：null/空白透传、五个特殊字符 HTML 实体转义、单遍替换（不重复编码已生成实体）、
 * 无特殊字符原样返回，并锁定“对已转义输入会二次编码”的既定行为
 * @since 1.0.0
 */
class SimpleXssCleanerTest {

    private final SimpleXssCleaner cleaner = new SimpleXssCleaner();

    @Test
    @DisplayName("null / 空串 / 纯空白原样返回")
    void blankPassthrough() {
        assertThat(cleaner.clean(null)).isNull();
        assertThat(cleaner.clean("")).isEmpty();
        assertThat(cleaner.clean("   ")).isEqualTo("   ");
    }

    @Test
    @DisplayName("五个特殊字符转义为 HTML 实体")
    void escapesSpecialChars() {
        assertThat(cleaner.clean("&")).isEqualTo("&amp;");
        assertThat(cleaner.clean("<")).isEqualTo("&lt;");
        assertThat(cleaner.clean(">")).isEqualTo("&gt;");
        assertThat(cleaner.clean("\"")).isEqualTo("&quot;");
        assertThat(cleaner.clean("'")).isEqualTo("&#39;");
    }

    @Test
    @DisplayName("脚本片段整体转义")
    void escapesScriptFragment() {
        assertThat(cleaner.clean("<script>alert('x')</script>"))
                .isEqualTo("&lt;script&gt;alert(&#39;x&#39;)&lt;/script&gt;");
    }

    @Test
    @DisplayName("单遍替换：& 只转义一次，不会对本次生成的 &amp; 再次编码")
    void singlePassNoRecursiveEncoding() {
        // 输入 "&" → "&amp;"（而非 "&amp;amp;"），证明 replaceEach 为单遍
        assertThat(cleaner.clean("&")).isEqualTo("&amp;");
        assertThat(cleaner.clean("a & b")).isEqualTo("a &amp; b");
    }

    @Test
    @DisplayName("既定行为：对已转义输入会二次编码（与类文档告诫一致）")
    void doubleEncodesAlreadyEscapedInput() {
        assertThat(cleaner.clean("&amp;")).isEqualTo("&amp;amp;");
    }

    @Test
    @DisplayName("无特殊字符原样返回")
    void noSpecialCharsUnchanged() {
        assertThat(cleaner.clean("hello world 123")).isEqualTo("hello world 123");
    }
}
