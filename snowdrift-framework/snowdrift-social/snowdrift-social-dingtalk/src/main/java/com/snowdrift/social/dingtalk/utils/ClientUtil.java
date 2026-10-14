package com.snowdrift.social.dingtalk.utils;

import com.aliyun.teaopenapi.Client;
import com.aliyun.teaopenapi.models.Config;
import com.snowdrift.framework.base.exception.BizException;
import com.snowdrift.framework.base.util.AssertUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * ClientUtil
 *
 * @author 83674
 * @date 2026/10/14-13:26
 * @description 钉钉客户端工具类
 * @since 1.0.0
 */
@Slf4j
public class ClientUtil {

    private static final String PROTOCOL = "https";

    private static final String REGION_ID = "central";

    private static final Config config;

    static {
        config = new Config();
        config.protocol = PROTOCOL;
        config.regionId = REGION_ID;
    }

    private ClientUtil() {
    }

    public static <T extends Client> T getInstance(Class<T> clazz) {
        AssertUtil.notNull(clazz, "钉钉客户端类型不能为空");
        try {
            return clazz.getDeclaredConstructor(Config.class).newInstance(config);
        } catch (ReflectiveOperationException | SecurityException e) {
            log.error("获取钉钉客户端实例失败, class={}, exceptionType={}", clazz.getName(), e.getClass().getName());
            throw new BizException("获取钉钉客户端实例失败: " + clazz.getName(), e);
        }
    }
}
