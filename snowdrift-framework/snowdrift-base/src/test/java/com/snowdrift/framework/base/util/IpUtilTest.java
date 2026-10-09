package com.snowdrift.framework.base.util;

import com.snowdrift.framework.base.constant.StrConst;
import com.snowdrift.framework.base.exception.BizException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * IpUtilTest
 *
 * @author gaoyzelov
 * @date 2026/10/9
 * @description IP 工具类单元测试
 * @since 1.0.0
 */
class IpUtilTest {

    private static final String PROXY = "10.0.0.1";
    private static final Set<String> TRUSTED = Set.of(PROXY, "10.0.0.5");

    private static HttpServletRequest request(String remoteAddr, String forwardedFor, String realIp) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn(remoteAddr);
        when(request.getHeader("X-Forwarded-For")).thenReturn(forwardedFor);
        when(request.getHeader("X-Real-IP")).thenReturn(realIp);
        return request;
    }

    @Nested
    @DisplayName("地址格式判断")
    class Format {

        @ParameterizedTest
        @ValueSource(strings = {"1.2.3.4", "0.0.0.0", "255.255.255.255", "2001:db8::1", "::1", "fe80::1%eth0"})
        void valid(String ip) {
            assertThat(IpUtil.isValidIp(ip)).isTrue();
        }

        @ParameterizedTest
        @ValueSource(strings = {"1.2.3", "1.2.3.4.5", "256.1.1.1", "1.2.3.4:80", "host.internal", "1.2.3.4 "})
        void invalid(String ip) {
            assertThat(IpUtil.isValidIp(ip)).isFalse();
        }

        @Test
        void nullIsInvalid() {
            assertThat(IpUtil.isValidIp(null)).isFalse();
        }

        @Test
        void ipVersion() {
            assertThat(IpUtil.isIpv4("1.2.3.4")).isTrue();
            assertThat(IpUtil.isIpv4("2001:db8::1")).isFalse();
            assertThat(IpUtil.isIpv4(null)).isFalse();
            assertThat(IpUtil.isIpv4("not-an-ip")).isFalse();

            assertThat(IpUtil.isIpv6("2001:db8::1")).isTrue();
            assertThat(IpUtil.isIpv6("1.2.3.4")).isFalse();
            assertThat(IpUtil.isIpv6(null)).isFalse();
        }

        @Test
        @DisplayName("内网判断覆盖回环、站点本地与链路本地")
        void internalIp() {
            assertThat(IpUtil.isInternalIp("127.0.0.1")).isTrue();
            assertThat(IpUtil.isInternalIp("10.1.2.3")).isTrue();
            assertThat(IpUtil.isInternalIp("192.168.1.1")).isTrue();
            assertThat(IpUtil.isInternalIp("172.16.0.1")).isTrue();
            assertThat(IpUtil.isInternalIp("169.254.1.1")).isTrue();
            assertThat(IpUtil.isInternalIp("fe80::1")).isTrue();

            // 注意：不覆盖 CGNAT、0.0.0.0 等其它保留段，也不是「公网可访问」判断
            assertThat(IpUtil.isInternalIp("100.64.0.1")).isFalse();
            assertThat(IpUtil.isInternalIp("8.8.8.8")).isFalse();
            assertThat(IpUtil.isInternalIp("not-an-ip")).isFalse();
            assertThat(IpUtil.isInternalIp(null)).isFalse();
        }
    }

    @Nested
    @DisplayName("getIp")
    class GetIp {

        @Test
        @DisplayName("默认重载无条件信任 X-Forwarded-For（可用于日志，不可用于安全判定）")
        void defaultOverloadTrustsForwardedHeader() {
            assertThat(IpUtil.getIp(request("10.0.0.1", "1.2.3.4", null))).isEqualTo("1.2.3.4");
            // 客户端可伪造：任意来源都会被采纳
            assertThat(IpUtil.getIp(request("8.8.8.8", "9.9.9.9", null))).isEqualTo("9.9.9.9");
        }

        @Test
        @DisplayName("无转发头时使用直连地址")
        void fallsBackToRemoteAddr() {
            assertThat(IpUtil.getIp(request("10.0.0.1", null, null))).isEqualTo("10.0.0.1");
            assertThat(IpUtil.getIp(request("10.0.0.1", "   ", null))).isEqualTo("10.0.0.1");
        }

        @Test
        @DisplayName("直连节点不可信时直接返回直连地址，不读取转发头")
        void untrustedPeerIsNotForwarded() {
            assertThat(IpUtil.getIp(request("8.8.8.8", "1.2.3.4", null), TRUSTED::contains)).isEqualTo("8.8.8.8");
        }

        @Test
        @DisplayName("从右向左剥离可信节点，首个不可信地址即客户端")
        void stripsTrustedHopsFromRight() {
            assertThat(IpUtil.getIp(request(PROXY, "1.2.3.4", null), TRUSTED::contains)).isEqualTo("1.2.3.4");
            assertThat(IpUtil.getIp(request(PROXY, "1.2.3.4, 10.0.0.5", null), TRUSTED::contains))
                    .isEqualTo("1.2.3.4");
            // 全部节点都可信时返回最左侧地址
            assertThat(IpUtil.getIp(request(PROXY, "10.0.0.5", null), TRUSTED::contains)).isEqualTo("10.0.0.5");
        }

        @Test
        @DisplayName("无 XFF 时才考虑 X-Real-IP")
        void usesRealIpOnlyWithoutForwardedFor() {
            assertThat(IpUtil.getIp(request(PROXY, null, "1.2.3.4"), TRUSTED::contains)).isEqualTo("1.2.3.4");
            // 有 XFF 时忽略 X-Real-IP
            assertThat(IpUtil.getIp(request(PROXY, "5.6.7.8", "1.2.3.4"), TRUSTED::contains)).isEqualTo("5.6.7.8");
            // X-Real-IP 非法时回退直连地址
            assertThat(IpUtil.getIp(request(PROXY, null, "junk"), TRUSTED::contains)).isEqualTo(PROXY);
        }

        @Test
        @DisplayName("转发链含非法节点时返回 unknown，不回退为可信代理地址")
        void malformedChainReturnsUnknown() {
            assertThat(IpUtil.getIp(request(PROXY, "garbage", null), TRUSTED::contains)).isEqualTo(StrConst.UNKNOWN);
            assertThat(IpUtil.getIp(request(PROXY, "1.2.3.4, garbage", null), TRUSTED::contains))
                    .isEqualTo(StrConst.UNKNOWN);
            // 只有分隔符的链条没有任何有效节点
            assertThat(IpUtil.getIp(request(PROXY, ",", null), TRUSTED::contains)).isEqualTo(StrConst.UNKNOWN);
            assertThat(IpUtil.getIp(request(PROXY, " , ", null), TRUSTED::contains)).isEqualTo(StrConst.UNKNOWN);
        }

        @Test
        @DisplayName("多余的空段被忽略（代理常追加逗号），不影响正常节点")
        void blankSegmentsAreIgnored() {
            assertThat(IpUtil.getIp(request(PROXY, "1.2.3.4,", null), TRUSTED::contains)).isEqualTo("1.2.3.4");
            assertThat(IpUtil.getIp(request(PROXY, "1.2.3.4, ,10.0.0.5", null), TRUSTED::contains)).isEqualTo("1.2.3.4");
        }

        @Test
        @DisplayName("超长转发头返回 unknown")
        void oversizedHeaderReturnsUnknown() {
            String oversized = "1.2.3.4,".repeat(1_200);
            assertThat(oversized).hasSizeGreaterThan(8192);

            assertThat(IpUtil.getIp(request(PROXY, oversized, null), TRUSTED::contains)).isEqualTo(StrConst.UNKNOWN);
        }

        @Test
        @DisplayName("带不存在网卡作用域的 IPv6 不抛 IllegalArgumentException（回归）")
        void scopedIpv6WithUnknownInterfaceDoesNotThrow() {
            assertThat(IpUtil.getIp(request("fe80::1%zzz", null, null), TRUSTED::contains))
                    .isEqualTo(StrConst.UNKNOWN);
            assertThat(IpUtil.getIp(request(PROXY, "::%zzz", null), TRUSTED::contains)).isEqualTo(StrConst.UNKNOWN);
        }

        @Test
        @DisplayName("request 为 null 返回 unknown，trustedProxy 为 null 抛业务异常")
        void nullArguments() {
            assertThat(IpUtil.getIp(null, TRUSTED::contains)).isEqualTo(StrConst.UNKNOWN);
            assertThat(IpUtil.getIp(null)).isEqualTo(StrConst.UNKNOWN);

            assertThatThrownBy(() -> IpUtil.getIp(request(PROXY, null, null), null))
                    .isInstanceOf(BizException.class).hasMessage("可信代理判断不能为空");
        }
    }

    @Nested
    @DisplayName("归属地解析")
    class Location {

        @Test
        @DisplayName("内网/非法 IP 与数据库缺失时返回 5 个空串，不查库")
        void returnsEmptyArrayForUnqueryableIp() {
            assertThat(IpUtil.parseIp("10.0.0.1")).containsExactly("", "", "", "", "");
            assertThat(IpUtil.parseIp("127.0.0.1")).containsExactly("", "", "", "", "");
            assertThat(IpUtil.parseIp("not-an-ip")).containsExactly("", "", "", "", "");
            assertThat(IpUtil.parseIp(null)).containsExactly("", "", "", "", "");
        }

        @Test
        @DisplayName("公网地址按数据库分段返回（内置库为 4 段）")
        void parsesPublicIp() {
            String[] region = IpUtil.parseIp("8.8.8.8");

            assertThat(region).hasSize(4);
            assertThat(region[0]).isNotBlank();
        }

        @Test
        @DisplayName("getIpLocation 过滤空串与占位段，不再返回只有分隔符的字符串（回归）")
        void getIpLocationFiltersPlaceholders() {
            assertThat(IpUtil.getIpLocation("10.0.0.1", "-")).isEmpty();
            assertThat(IpUtil.getIpLocation("127.0.0.1", "-")).isEmpty();
            assertThat(IpUtil.getIpLocation("not-an-ip", "-")).isEmpty();
            assertThat(IpUtil.getIpLocation(null, "-")).isEmpty();

            assertThat(IpUtil.getIpLocation("8.8.8.8", "-")).isNotBlank().doesNotContain("--");
        }

        @Test
        @DisplayName("分隔符为空时使用空格")
        void defaultDelimiterIsSpace() {
            assertThat(IpUtil.getIpLocation("8.8.8.8", null)).doesNotContain("-");
            assertThat(IpUtil.getIpLocation("8.8.8.8", " ")).isNotBlank();
        }
    }
}
