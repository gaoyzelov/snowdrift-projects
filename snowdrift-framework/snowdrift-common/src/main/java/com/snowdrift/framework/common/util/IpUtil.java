package com.snowdrift.framework.common.util;

import com.google.common.net.InetAddresses;
import com.snowdrift.framework.common.constant.StrConst;
import com.snowdrift.framework.common.exception.BizException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.lionsoul.ip2region.xdb.Searcher;

import java.io.InputStream;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.List;

/**
 * IpUtil
 *
 * @author gaoyzelov
 * @date 2026/4/29-16:10
 * @description ip工具类
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
    private static volatile boolean searcherInitialized;

    /**
     * 获取 IP 查询器（懒加载，首次调用时初始化）
     */
    private static Searcher getSearcher() {
        if (!searcherInitialized) {
            synchronized (IpUtil.class) {
                if (!searcherInitialized) {
                    long now = System.currentTimeMillis();
                    try (InputStream stream = IpUtil.class.getClassLoader().getResourceAsStream("ip2region.xdb")) {
                        if (stream == null) {
                            log.warn("未找到 IP2Region 数据库文件 (ip2region.xdb)，IP 归属地查询将不可用。"
                                    + "请将文件放置在 classpath 根目录下。");
                        } else {
                            byte[] dbBinStr = stream.readAllBytes();
                            searcher = Searcher.newWithBuffer(dbBinStr);
                            log.info("IP2Region 数据库加载成功，耗时 ({} ms)", System.currentTimeMillis() - now);
                        }
                    } catch (Exception e) {
                        log.error("IP2Region 初始化失败，IP 归属地查询将不可用", e);
                    }
                    searcherInitialized = true;
                }
            }
        }
        return searcher;
    }

    /**
     * 获取请求IP
     *
     * @param request HttpServletRequest
     * @return IP地址
     */
    public static String getIp(HttpServletRequest request) {
        if (request == null) {
            return StrConst.UNKNOWN;
        }
        // 请求头
        String[] headers = {"X-Forwarded-For", "X-Real-IP", "Proxy-Client-IP", "WL-Proxy-Client-IP", "HTTP_CLIENT_IP", "HTTP_X_FORWARDED_FOR"};
        String ip;
        for (String header : headers) {
            ip = request.getHeader(header);
            if (StringUtils.isNotBlank(ip) && !StringUtils.endsWithIgnoreCase(ip, StrConst.UNKNOWN)) {
                return getMultistageReverseProxyIp(ip);
            }
        }
        ip = request.getRemoteAddr();
        return getMultistageReverseProxyIp(ip);
    }

    /**
     * 多级反向代理IP处理
     *
     * @param ip ip地址
     * @return 处理后的IP
     */
    private static String getMultistageReverseProxyIp(String ip) {
        // 多级反向代理检测
        if (StringUtils.isNotBlank(ip) && StringUtils.indexOf(ip, StrConst.COMMA) > 0) {
            String[] ips = StringUtils.split(ip, StrConst.COMMA);
            for (String subIp : ips) {
                if (StringUtils.isNotBlank(subIp) && !StringUtils.endsWithIgnoreCase(subIp, StrConst.UNKNOWN)) {
                    ip = subIp;
                    break;
                }
            }
        }
        return ip;
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
            String region = s.search(ip);
            return region.split("\\|");
        } catch (Exception e) {
            log.error("IP解析失败，ip={}", ip, e);
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
                .filter(s -> !"0".equals(s) && !"内网IP".equals(s))
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
        try {
            basicCheck(ip);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    /**
     * 检测IP地址是否为IPv4
     *
     * @param ip ip地址
     * @return true/false
     */
    public static boolean isIpv4(String ip) {
        try {
            basicCheck(ip);
            InetAddress address = InetAddresses.forString(ip);
            return address instanceof Inet4Address;
        } catch (Exception e) {
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
        try {
            basicCheck(ip);
            InetAddress address = InetAddresses.forString(ip);
            return address instanceof Inet6Address;
        } catch (Exception e) {
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
        try {
            basicCheck(ip);
            InetAddress address = InetAddresses.forString(ip);
            return address.isLoopbackAddress() || address.isSiteLocalAddress() || address.isLinkLocalAddress();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 基本校验
     *
     * @param ip ip地址
     */
    private static void basicCheck(String ip) {
        AssertUtil.notBlank(ip, "IP地址不能为空");
        AssertUtil.isTrue(InetAddresses.isInetAddress(ip), "IP地址格式有误");
    }
}
