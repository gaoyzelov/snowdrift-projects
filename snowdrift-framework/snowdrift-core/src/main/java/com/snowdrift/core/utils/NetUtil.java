package com.snowdrift.core.utils;

import com.snowdrift.core.constant.StrConst;
import com.snowdrift.core.exception.BaseException;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.ServerSocket;

/**
 * NetUtil
 *
 * @author gaoye
 * @date 2025/03/24 19:52:33
 * @description 网络工具类
 * @since 1.0.0
 */
public class NetUtil {

    /**
     * 默认最小端口，1024
     */
    private static final int MIN_PORT = 1024;

    /**
     * 默认最大端口，65535
     */
    public static final int MAX_PORT = 65535;

    /**
     * 本地IP地址
     */
    public static final String LOCALHOST = "127.0.0.1";

    /**
     * 未知IP地址
     */
    private static final String UNKNOWN = "unknown";


    /**
     * 是否为有效的端口
     *
     * @param port 端口号
     * @return 是否有效
     */
    public static boolean isValidPort(int port) {
        // 有效端口是0～65535
        return port >= 0 && port <= MAX_PORT;
    }

    /**
     * 检测本地端口可用性
     *
     * @param port 被检测的端口
     * @return 是否可用
     */
    public static boolean isAvailablePort(int port) {
        // 判断端口是否满足规定范围
        if (!isValidPort(port)) {
            return false;
        }
        // 尝试使用指定端口创建ServerSocket
        try (ServerSocket ss = new ServerSocket(port)) {
            ss.setReuseAddress(true);
        } catch (IOException ignored) {
            return false;
        }
        // 尝试使用指定端口创建DatagramSocket
        try (DatagramSocket ds = new DatagramSocket(port)) {
            ds.setReuseAddress(true);
        } catch (IOException ignored) {
            return false;
        }
        return true;
    }

    /**
     * 获取可用端口
     *
     * @return 可用端口
     */
    public static int getAvailablePort() {
        for (int i = MIN_PORT; i <= MAX_PORT; i++) {
            int port = RandomUtils.nextInt(MIN_PORT, MAX_PORT);
            if (isAvailablePort(port)) {
                return port;
            }
        }
        throw new BaseException("获取可用端口失败");
    }

    /**
     * 判断是否为局域网/内网IP
     * <p>
     * 10.0.0.0 到 10.255.255.255
     * 172.16.0.0 到 172.31.255.255
     * 192.168.0.0 到 192.168.255.255
     *
     * @param ip ip地址
     * @return true/false
     */
    public static boolean isInternalIp(String ip) {
        if (StringUtils.equals(ip, LOCALHOST)) {
            return true;
        }
        // ipv4地址转字节
        byte[] bytes = new byte[]{};
        if (bytes == null || bytes.length < 2) {
            return true;
        }
        // 首位
        byte first = bytes[0];
        // 第二位
        byte second = bytes[1];
        // 10.x.x.x
        final byte byte_10 = (byte) 0x0A; // 10
        // 172.16.x.x
        final byte byte_172 = (byte) 0xAC; // 172
        final byte byte_16 = (byte) 0x10; // 16
        final byte byte_31 = (byte) 0x1F; // 31
        // 192.168.x.x
        final byte byte_192 = (byte) 0xC0; // 192
        final byte byte_168 = (byte) 0xA8; // 168

        // 按首位进行判断
        switch (first) {
            case byte_10:
                return true;
            case byte_172:
                if (second >= byte_16 && second <= byte_31) {
                    return true;
                }
            case byte_192:
                if (second == byte_168) {
                    return true;
                }
            default:
                return false;
        }
    }

    /**
     * 获取请求IP
     *
     * @param request HttpServletRequest
     * @return IP地址
     */
    public static String getRequestIp(HttpServletRequest request) {
        // 请求头
        String[] headers = {"X-Forwarded-For", "X-Real-IP", "Proxy-Client-IP", "WL-Proxy-Client-IP", "HTTP_CLIENT_IP", "HTTP_X_FORWARDED_FOR"};
        String ip;
        for (String header : headers) {
            ip = request.getHeader(header);
            if (StringUtils.isNotBlank(ip) && !StringUtils.endsWithIgnoreCase(ip, UNKNOWN)) {
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
                if (StringUtils.isNotBlank(subIp) && !StringUtils.endsWithIgnoreCase(subIp, UNKNOWN)) {
                    ip = subIp;
                    break;
                }
            }
        }
        return ip;
    }
}