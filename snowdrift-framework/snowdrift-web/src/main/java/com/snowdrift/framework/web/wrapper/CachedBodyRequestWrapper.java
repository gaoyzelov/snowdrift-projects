package com.snowdrift.framework.web.wrapper;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 请求体缓存包装器 — 支持多次读取 {@link #getInputStream()} 和 {@link #getReader()}。
 * <p>
 * Servlet 标准流只能读一次，本类将 Body 缓存为 {@code byte[]}，
 * 后续 Filter 链、拦截器和 Controller 均可重复读取。
 * </p>
 *
 * @author gaoyzelov
 * @since 1.0.0
 */
public class CachedBodyRequestWrapper extends HttpServletRequestWrapper {

    private static final String FORM_CONTENT_TYPE = "application/x-www-form-urlencoded";

    private final byte[] body;

    /**
     * 基于缓存字节解析出的表单参数（懒加载，仅 x-www-form-urlencoded 请求使用）
     */
    private Map<String, String[]> cachedParameterMap;

    /**
     * 创建包装器并缓存请求体
     *
     * @param request 原始请求
     * @throws IOException 读取请求体失败时抛出
     */
    public CachedBodyRequestWrapper(HttpServletRequest request) throws IOException {
        super(request);
        this.body = request.getInputStream().readAllBytes();
    }

    /**
     * 获取缓存的请求体字节数组，供子类覆盖（如 XSS 过滤后返回处理后的数据）。
     * <p>
     * {@link #getInputStream()} 和 {@link #getReader()} 均通过此方法获取数据，
     * 子类覆盖此方法即可同时影响流和 Reader 的输出。
     * </p>
     */
    public byte[] getCachedBody() {
        return body;
    }

    // ========== 表单参数解析 ==========
    // 构造器已把原始输入流读尽，HttpContextFilter 等下游再调用 getParameter*
    // 会因底层流为 EOF 而拿不到 x-www-form-urlencoded 请求体参数，
    // 因此这里基于缓存字节解析表单参数，与 QueryString 参数合并后对外提供。

    @Override
    public String getParameter(String name) {
        if (!isFormContentType()) {
            return super.getParameter(name);
        }
        String[] values = getCachedParameterMap().get(name);
        return values != null && values.length > 0 ? values[0] : null;
    }

    @Override
    public String[] getParameterValues(String name) {
        if (!isFormContentType()) {
            return super.getParameterValues(name);
        }
        return getCachedParameterMap().get(name);
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        return isFormContentType() ? getCachedParameterMap() : super.getParameterMap();
    }

    @Override
    public Enumeration<String> getParameterNames() {
        if (!isFormContentType()) {
            return super.getParameterNames();
        }
        return Collections.enumeration(getCachedParameterMap().keySet());
    }

    private boolean isFormContentType() {
        String contentType = super.getContentType();
        return contentType != null
                && contentType.toLowerCase(Locale.ENGLISH).startsWith(FORM_CONTENT_TYPE);
    }

    private Map<String, String[]> getCachedParameterMap() {
        Map<String, String[]> cached = this.cachedParameterMap;
        if (cached == null) {
            // 对外保持只读，与 Servlet 规范中 getParameterMap 不可修改的语义一致
            cached = Collections.unmodifiableMap(parseFormParameters());
            this.cachedParameterMap = cached;
        }
        return cached;
    }

    private Map<String, String[]> parseFormParameters() {
        // 先取 QueryString 参数（Body 已被构造器读尽，super 不会重复触发表单解析）
        Map<String, String[]> params = new HashMap<>(super.getParameterMap());
        Charset charset = resolveCharset();
        String bodyStr = new String(getCachedBody(), charset);
        if (bodyStr.isEmpty()) {
            return params;
        }
        for (String pair : bodyStr.split("&")) {
            if (pair.isEmpty()) {
                continue;
            }
            int idx = pair.indexOf('=');
            String key = idx >= 0 ? pair.substring(0, idx) : pair;
            String value = idx >= 0 ? pair.substring(idx + 1) : "";
            key = URLDecoder.decode(key, charset);
            value = URLDecoder.decode(value, charset);
            String[] previous = params.get(key);
            if (previous == null) {
                params.put(key, new String[]{value});
            } else {
                String[] merged = Arrays.copyOf(previous, previous.length + 1);
                merged[previous.length] = value;
                params.put(key, merged);
            }
        }
        return params;
    }

    private Charset resolveCharset() {
        String encoding = super.getCharacterEncoding();
        if (encoding != null) {
            try {
                return Charset.forName(encoding);
            } catch (RuntimeException e) {
                // 非法编码名时回退 UTF-8
            }
        }
        return StandardCharsets.UTF_8;
    }

    @Override
    public ServletInputStream getInputStream() {
        ByteArrayInputStream bis = new ByteArrayInputStream(getCachedBody());
        return new ServletInputStream() {
            @Override
            public int read() {
                return bis.read();
            }

            @Override
            public boolean isFinished() {
                return false;
            }

            @Override
            public boolean isReady() {
                return false;
            }

            @Override
            public void setReadListener(ReadListener listener) {
                // 同步 IO，无需 Listener
            }

            @Override
            public int available() throws IOException {
                return body.length;
            }
        };
    }

    @Override
    public int getContentLength() {
        return body.length;
    }

    @Override
    public long getContentLengthLong() {
        return body.length;
    }

    @Override
    public BufferedReader getReader() {
        return new BufferedReader(new InputStreamReader(this.getInputStream()));
    }
}
