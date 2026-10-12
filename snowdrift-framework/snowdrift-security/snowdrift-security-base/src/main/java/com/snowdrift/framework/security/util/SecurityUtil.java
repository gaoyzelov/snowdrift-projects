package com.snowdrift.framework.security.util;

import com.snowdrift.framework.base.constant.StrConst;
import org.apache.commons.lang3.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SecurityUtil
 *
 * @author gaoyzelov
 * @date 2026/10/8
 * @description 安全模块公共工具
 * @since 1.0.0
 */
public final class SecurityUtil {

    /** 令牌中可识别的片段：连续的字母、数字、下划线或连字符 */
    private static final Pattern TOKEN_SEGMENT = Pattern.compile("[\\w-]+");

    /** 片段中保留的前后明文位数 */
    private static final int KEEP_LENGTH = 5;

    /** 可安全脱敏的最小片段长度：需同时保留前后各 {@link #KEEP_LENGTH} 位并至少隐藏 1 位 */
    private static final int MIN_MASKABLE_SEGMENT_LENGTH = KEEP_LENGTH * 2 + 1;

    /** 片段中间被隐藏部分的占位符 */
    private static final String MASK_MIDDLE = "*****";

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
        // 逐「连续 [\w-] 片段」脱敏，而不是整串一次 replaceAll：
        // 整串替换时长度不足 11 位的片段既不匹配、也不触发兜底（因为其它片段命中了），
        // 会把该片段原文打进日志 —— 例如 JWT 形态的 "aaaaaaaaaa.bbbbbbbbbbbb" 会漏出前一段。
        Matcher matcher = TOKEN_SEGMENT.matcher(tokenValue);
        StringBuilder masked = new StringBuilder(tokenValue.length());
        int cursor = 0;
        boolean maskedAny = false;
        while (matcher.find()) {
            String segment = matcher.group();
            if (segment.length() < MIN_MASKABLE_SEGMENT_LENGTH) {
                // 任一片段短到无法安全保留前后各 5 位：整体掩码，绝不把该片段原文输出
                return StrConst.MASK_REPLACEMENT;
            }
            masked.append(tokenValue, cursor, matcher.start())
                    .append(segment, 0, KEEP_LENGTH)
                    .append(MASK_MIDDLE)
                    .append(segment, segment.length() - KEEP_LENGTH, segment.length());
            cursor = matcher.end();
            maskedAny = true;
        }
        if (!maskedAny) {
            // 没有任何可识别片段（如全符号、非 ASCII）：整体掩码
            return StrConst.MASK_REPLACEMENT;
        }
        masked.append(tokenValue, cursor, tokenValue.length());
        return masked.toString();
    }
}
