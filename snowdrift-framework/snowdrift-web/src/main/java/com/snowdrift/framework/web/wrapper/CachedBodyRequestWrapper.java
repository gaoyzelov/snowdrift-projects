package com.snowdrift.framework.web.wrapper;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

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

    private final byte[] body;

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
