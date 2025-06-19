package com.snowdrift.protocol.jt808.config;

import com.snowdrift.protocol.jt808.properties.JT808Properties;
import com.snowdrift.protocol.jt808.server.JT808TcpServer;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * JT808ServerAutoConfiguration
 *
 * @author gaoye
 * @date 2025/06/16 15:58:27
 * @description xxxxxxxx
 * @since 1.0
 */
@EnableConfigurationProperties(JT808Properties.class)
public class JT808ServerAutoConfiguration implements InitializingBean {

    @Resource
    private JT808Properties prop;

    private final ExecutorService singleThreadExecutor = Executors.newFixedThreadPool(1);

    @Override
    public void afterPropertiesSet() throws Exception {
        if (Objects.isNull(prop)){
            prop = JT808Properties.defaultProperties();
        }
        singleThreadExecutor.execute(new JT808TcpServer(prop));
    }
}