package com.snowdrift.protocol.jt808.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.io.Serializable;

/**
 * JT808Properties
 *
 * @author gaoye
 * @date 2025/06/16 15:58:46
 * @description xxxxxxxx
 * @since 1.0
 */
@Data
@ConfigurationProperties(prefix = "protocol.jt808")
public class JT808Properties implements Serializable {

    private Integer serverPort;

    private Integer bossThreadCount;

    private Integer workThreadCount;

    public static JT808Properties defaultProperties() {
        JT808Properties properties = new JT808Properties();
        properties.setServerPort(3456);
        return properties;
    }
}