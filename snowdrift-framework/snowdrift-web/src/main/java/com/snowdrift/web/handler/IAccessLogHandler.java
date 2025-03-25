package com.snowdrift.web.handler;

import com.snowdrift.web.bo.WebLog;

/**
 * AccessLogHandler
 *
 * @author gaoye
 * @date 2025/03/24 14:04:42
 * @description 访问日志处理器
 * @since 1.0.0
 */
public interface IAccessLogHandler {

    void handle(WebLog webLog);
}