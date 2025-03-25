package com.snowdrift.web.handler;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONWriter;
import com.snowdrift.web.bo.WebLog;
import lombok.extern.slf4j.Slf4j;

/**
 * DefaultAccessLogHandler
 *
 * @author gaoye
 * @date 2025/03/24 14:07:20
 * @description 默认访问日志处理器
 * @since 1.0.0
 */
@Slf4j
public class DefaultAccessLogHandler implements IAccessLogHandler {

    @Override
    public void handle(WebLog webLog) {
        log.info("{}", JSON.toJSONString(webLog, JSONWriter.Feature.PrettyFormat));
    }
}