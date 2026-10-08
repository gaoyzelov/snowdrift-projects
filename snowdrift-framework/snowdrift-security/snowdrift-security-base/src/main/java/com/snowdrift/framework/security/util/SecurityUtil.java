package com.snowdrift.framework.security.util;

import com.snowdrift.framework.base.constant.StrConst;
import com.snowdrift.framework.base.util.DesensitizeUtil;
import org.apache.commons.lang3.StringUtils;

/**
 * SecurityUtil
 *
 * @author gaoyzelov
 * @date 2026/10/8
 * @description 安全模块公共工具
 * @since 1.0.0
 */
public final class SecurityUtil {

    private SecurityUtil() {
    }

    /**
     * 令牌脱敏，用于令牌进入日志的场景（登录、登出、被踢下线、TokenStore 读写等）。
     * <p>
     * 保留前 5 位与后 5 位，中间以 {@code *****} 覆盖，兼顾"同一次会话的日志可关联"与
     * "不暴露可用于认证的完整令牌"。长度不足以同时保留前后各 5 位时整体掩码为
     * {@link StrConst#MASK_REPLACEMENT}，避免短令牌被原样打印。
     * </p>
     *
     * @param tokenValue 原始令牌，可为 null 或空
     * @return 脱敏后的令牌
     */
    public static String desensitizeToken(String tokenValue) {
        if (StringUtils.isBlank(tokenValue)) {
            return StrConst.MASK_REPLACEMENT;
        }
        String masked = DesensitizeUtil.process(tokenValue, "([\\w-]{5})[\\w-]+([\\w-]{5})", "$1*****$2");
        // DesensitizeUtil.process 在正则未命中时原样返回，此处兜底为整体掩码
        return StringUtils.equals(masked, tokenValue) ? StrConst.MASK_REPLACEMENT : masked;
    }
}
