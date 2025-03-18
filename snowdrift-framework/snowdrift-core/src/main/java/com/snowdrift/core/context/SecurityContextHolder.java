package com.snowdrift.core.context;

import com.alibaba.ttl.TransmittableThreadLocal;
import lombok.NonNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * SecurityContextHolder
 *
 * @author gaoye
 * @date 2025/03/18 16:53:12
 * @description xxxxxxxx
 * @since 1.0
 */
public final class SecurityContextHolder {

    private static final ThreadLocal<SecurityContext> contextHolder = new TransmittableThreadLocal<>();

    private SecurityContextHolder() {
    }

    /**
     * 获取上下文
     *
     * @return SecurityContext
     */
    public static SecurityContext getContext() {
        SecurityContext ctx = contextHolder.get();
        if (ctx == null) {
            ctx = emptyContext();
            contextHolder.set(ctx);
        }
        return ctx;
    }

    /**
     * 设置上下文
     *
     * @param ctx 上下文
     */
    public static void setContext(@NonNull SecurityContext ctx) {
        contextHolder.set(ctx);
    }

    /**
     * 获取ID
     *
     * @return ID
     */
    public static Optional<Object> getId() {
        return Optional.ofNullable(getContext().getId());
    }

    /**
     * 获取ID
     *
     * @param defVal 默认值
     * @return ID
     */
    public static Object getId(@NonNull Object defVal) {
        return getId().orElse(defVal);
    }

    /**
     * 获取ID
     *
     * @param clazz ID类型
     * @return ID
     * @throws ClassCastException   类型转换异常
     * @throws NullPointerException ID为空异常
     */
    public static <T> T getId(@NonNull Class<T> clazz) throws ClassCastException, NullPointerException {
        Optional<Object> id = getId();
        if (id.isPresent()) {
            return clazz.cast(id.get());
        }
        throw new NullPointerException("Can not found id from context.");
    }

    /**
     * 获取名称
     *
     * @return 名称
     */
    public static Optional<String> getName() {
        return Optional.ofNullable(getContext().getName());
    }

    /**
     * 获取名称
     *
     * @param defVal 默认值
     * @return 名称
     */
    public static String getName(@NonNull String defVal) {
        return getName().orElse(defVal);
    }

    /**
     * 获取租户ID
     *
     * @return 租户ID
     */
    public static Optional<Object> getTenantId() {
        return Optional.ofNullable(getContext().getTenantId());
    }

    /**
     * 获取租户ID
     *
     * @param defVal 默认租户ID
     * @return 租户ID
     */
    public static Object getTenantId(@NonNull Object defVal) {
        return getTenantId().orElse(defVal);
    }

    /**
     * 获取租户ID
     *
     * @param clazz 租户ID类型
     * @return 租户ID
     * @throws ClassCastException   类型转换异常
     * @throws NullPointerException ID为空异常
     */
    public static <T> T getTenantId(@NonNull Class<T> clazz) throws ClassCastException, NullPointerException {
        Optional<Object> tenantId = getTenantId();
        if (tenantId.isPresent()) {
            return clazz.cast(tenantId.get());
        }
        throw new NullPointerException("Can not found tenantId from context.");
    }

    /**
     * 获取扩展信息
     *
     * @return 扩展信息
     */
    public static Map<String, Object> getExtra() {
        return Optional.ofNullable(getContext().getExtra()).orElse(new HashMap<>());
    }

    /**
     * 获取扩展信息
     *
     * @param key   扩展信息键
     * @param clazz 扩展信息类型
     * @return 扩展信息
     * @throws ClassCastException   类型转换异常
     * @throws NullPointerException 扩展信息为空异常
     */
    public static <T> T getExtra(@NonNull String key, @NonNull Class<T> clazz) throws ClassCastException, NullPointerException {
        Map<String, Object> extra = getExtra();
        return Optional.ofNullable(extra.get(key))
                .map(clazz::cast)
                .orElseThrow(NullPointerException::new);
    }

    /**
     * 获取扩展信息
     *
     * @param key    扩展信息键
     * @param clazz  扩展信息类型
     * @param defVal 默认值
     * @return 扩展信息
     */
    public static <T> T getExtra(@NonNull String key, @NonNull Class<T> clazz, @NonNull T defVal) {
        Map<String, Object> extra = getExtra();
        return Optional.ofNullable(extra.get(key))
                .map(clazz::cast)
                .orElse(defVal);
    }

    /**
     * 创建空的上下文
     *
     * @return SecurityContext
     */
    public static SecurityContext emptyContext() {
        return SecurityContext.builder().build();
    }

    /**
     * 清除上下文
     */
    public static void clearContext() {
        contextHolder.remove();
    }
}