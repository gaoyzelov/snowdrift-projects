package com.snowdrift.framework.common.util;

import com.alibaba.fastjson2.JSON;
import com.snowdrift.framework.common.constant.StrConst;
import com.snowdrift.framework.common.result.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * ServletUtil
 *
 * @author gaoyzelov
 * @date 2026/4/29-16:44
 * @description 获取请求工具类
 * @since 1.0.0
 */
public final class ServletUtil {
    private ServletUtil() {
    }

    /**
     * 获取请求ua
     *
     * @param request 请求
     * @return ua
     */
    public static String getUserAgent(HttpServletRequest request) {
        String ua = request.getHeader("User-Agent");
        return ua != null ? ua : StrConst.EMPTY;
    }

    /**
     * 获取请求头
     *
     * @param request 请求
     * @return header
     */
    public static Map<String, String> getHeaderMap(HttpServletRequest request) {
        Map<String, String> headerMap = new HashMap<>();
        Enumeration<String> names = request.getHeaderNames();
        if (Objects.isNull(names)) return headerMap;
        String name;
        while (names.hasMoreElements()) {
            name = names.nextElement();
            headerMap.put(name, request.getHeader(name));
        }
        return headerMap;
    }

    /**
     * 获取请求参数
     *
     * @param request 请求
     * @return 参数
     */
    public static Map<String, String> getParamMap(HttpServletRequest request) {
        Map<String, String[]> parameterMap = request.getParameterMap();
        Map<String, String> params = new HashMap<>();
        for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
            params.put(entry.getKey(), StringUtils.join(entry.getValue(), StrConst.COMMA));
        }
        return params;
    }

    /**
     * 写入 JSON 响应
     *
     * @param response 响应对象
     * @param status   HTTP 状态码
     * @param result   响应体
     * @throws IOException IO异常
     */
    public static void writeJsonResponse(HttpServletResponse response, int status, Result<?> result) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(JSON.toJSONString(result));
        response.getWriter().flush();
    }
}
