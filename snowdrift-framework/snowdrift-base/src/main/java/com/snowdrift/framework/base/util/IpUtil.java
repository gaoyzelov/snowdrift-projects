package com.snowdrift.framework.base.util;

import com.google.common.net.InetAddresses;
import com.snowdrift.framework.base.constant.StrConst;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.lionsoul.ip2region.xdb.Searcher;

import java.io.InputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

/**
 * IpUtil
 *
 * @author gaoyzelov
 * @date 2026/4/29-16:10
 * @description IP工具类
 * @since 1.0.0
 */
@Slf4j
public final class IpUtil {

    private IpUtil() {
    }

    /**
     * IP 查询器，懒加载避免 ip2region.xdb 缺失时阻断应用启动
     */
    private static volatile Searcher searcher;

    private static InputStream openDatabase() throws IOException {
        String path = System.getProperty("snowdrift.ip2region.path");
        return StringUtils.isBlank(path)
                ? IpUtil.class.getClassLoader().getResourceAsStream("ip2region.xdb")
                : Files.newInputStream(Path.of(path));
    }

    /**
     * 获取 IP 查询器（懒加载，首次调用时初始化，失败后下次调用自动重试）
     */
    private static Searcher getSearcher() {
        if (searcher == null) {
            synchronized (IpUtil.class) {
                if (searcher == null) {
                    long now = System.currentTimeMillis();
                    try (InputStream stream = openDatabase()) {
                        if (stream == null) {
                            log.warn("未找到 IP2Region 数据库文件 (ip2region.xdb)，IP 归属地查询将不可用。"
                                    + "请将文件放置在 classpath 根目录下。");
                        } else {
                            byte[] dbBinStr = stream.readAllBytes();
                            searcher = Searcher.newWithBuffer(dbBinStr);
                            log.info("IP2Region 数据库加载成功，耗时 ({} ms)", System.currentTimeMillis() - now);
                        }
                    } catch (Exception e) {
                        log.error("IP2Region 初始化失败，IP 归属地查询将不可用, err={}", e.getMessage());
                    }
                }
            }
        }
        return searcher;
    }

    /**
     * 获取请求IP
     * <p>
     * <b>本方法无条件信任 {@code X-Forwarded-For}</b>（等价于 {@code trustedProxy = ip -> true}），
     * 因此返回值可被客户端伪造，<b>不得用于安全判定</b>（限流、IP 白名单、审计归属等）。
     * </p>
     * <p>
     * 安全场景请改用 {@link #getIp(HttpServletRequest, java.util.function.Predicate)} 并传入真实的
     * 可信代理判定；前面若没有可信代理，直接使用 {@code request.getRemoteAddr()}。
     * </p>
     *
     * @param request HttpServletRequest
     * @return IP地址（可能取自客户端提供的转发头）
     */
    public static String getIp(HttpServletRequest request) {
        return getIp(request, ip -> true);
    }

    /**
     * 仅信任显式指定的代理，从 X-Forwarded-For 右侧向左剥离可信节点。
     * <p>
     * {@code trustedProxy} 不应无条件返回 {@code true}——那等同于信任任何客户端自带的转发头；
     * 代理必须正确覆写或追加转发头。
     * </p>
     * <p>
     * 注意与另一重载的区别：{@link #getIp(HttpServletRequest)} 正是无条件信任转发头，两者行为不同。
     * </p>
     */
    public static String getIp(HttpServletRequest request, Predicate<String> trustedProxy) {
        if (request == null) return StrConst.UNKNOWN;
        AssertUtil.notNull(trustedProxy, "可信代理判断不能为空");
        String remote = normalizedIp(request.getRemoteAddr());
        if (StrConst.UNKNOWN.equals(remote) || !trustedProxy.test(remote)) return remote;
        String forwarded = request.getHeader("X-Forwarded-For");
        if (StringUtils.isBlank(forwarded)) {
            String real = normalizedIp(request.getHeader("X-Real-IP"));
            return StrConst.UNKNOWN.equals(real) ? remote : real;
        }
        if (forwarded.length() > 8192) return remote;
        String current = remote;
        String[] hops = forwarded.split(",");
        for (int i = hops.length - 1; i >= 0; i--) {
            if (!trustedProxy.test(current)) return current;
            String candidate = normalizedIp(hops[i]);
            if (StrConst.UNKNOWN.equals(candidate)) return remote;
            current = candidate;
        }
        return current;
    }

    /**
     * 规范化IP地址
     *
     * @param ip ip地址
     * @return 规范化IP地址
     */
    private static String normalizedIp(String ip) {
        String trimmed = StringUtils.trim(ip);
        return isValidIp(trimmed) ? InetAddresses.forString(trimmed).getHostAddress() : StrConst.UNKNOWN;
    }


    /**
     * 解析IP地址，返回完整地域信息数组
     *
     * @param ip ip地址
     * @return 解析IP地址，返回完整地域信息数组
     */
    public static String[] parseIp(String ip) {
        if (!isValidIp(ip) || isInternalIp(ip)) {
            return new String[]{"", "", "", "", ""};
        }
        Searcher s = getSearcher();
        if (s == null) {
            return new String[]{"", "", "", "", ""};
        }
        try {
            String region = s.search(InetAddresses.forString(ip).getHostAddress());
            return StringUtils.isBlank(region) ? new String[]{"", "", "", "", ""} : region.split("\\|");
        } catch (Exception e) {
            log.error("IP解析失败，ip={}, err={}", ip, e.getMessage());
            return new String[]{"", "", "", "", ""};
        }
    }

    /**
     * 获取IP地址
     *
     * @param ip        ip地址
     * @param delimiter 分隔符
     * @return IP地址
     */
    public static String getIpLocation(String ip, String delimiter) {
        String[] info = parseIp(ip);
        delimiter = StringUtils.isBlank(delimiter) ? StrConst.SPACE : delimiter;
        // 过滤无意义的"0"和内网IP标识
        List<String> list = Arrays.stream(info)
                .filter(s -> !StringUtils.equals(s, "0") && !StringUtils.equals(s, "内网IP"))
                .toList();
        return String.join(delimiter, list);
    }

    /**
     * 检测IP地址是否有效
     *
     * @param ip ip地址
     * @return true/false
     */
    public static boolean isValidIp(String ip) {
        return ip != null && InetAddresses.isInetAddress(ip);
    }

    /**
     * 检测IP地址是否为IPv4
     *
     * @param ip ip地址
     * @return true/false
     */
    public static boolean isIpv4(String ip) {
        if (ip == null) {
            return false;
        }
        try {
            InetAddress address = InetAddresses.forString(ip);
            return address instanceof Inet4Address;
        } catch (RuntimeException e) {
            return false;
        }
    }

    /**
     * 检测IP地址是否为IPv6
     *
     * @param ip ip地址
     * @return true/false
     */
    public static boolean isIpv6(String ip) {
        if (ip == null) {
            return false;
        }
        try {
            InetAddress address = InetAddresses.forString(ip);
            return address instanceof Inet6Address;
        } catch (RuntimeException e) {
            return false;
        }
    }

    /**
     * 检测IP是否为内部IP
     *
     * @param ip ip地址
     * @return true/false
     */
    public static boolean isInternalIp(String ip) {
        if (ip == null) {
            return false;
        }
        try {
            InetAddress address = InetAddresses.forString(ip);
            return address.isLoopbackAddress() || address.isSiteLocalAddress() || address.isLinkLocalAddress();
        } catch (RuntimeException e) {
            return false;
        }
    }

}
