package com.snowdrift.framework.orm.mp.config;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;

/**
 * MyBatis-Plus 拦截器扩展点。
 * <p>
 * 业务应用可通过此接口向框架创建的 {@link MybatisPlusInterceptor} 追加自定义拦截器，
 * 保留框架默认的数据权限、全表操作保护、分页和其它已启用的安全插件。
 * </p>
 */
@FunctionalInterface
public interface MybatisPlusInterceptorCustomizer {

    /**
     * 定制框架默认拦截器。多个定制器按 Spring 的 {@code @Order} 顺序依次执行，
     * 并在框架内置拦截器之后追加自定义拦截器。
     *
     * @param interceptor 框架创建的 MyBatis-Plus 拦截器
     */
    void customize(MybatisPlusInterceptor interceptor);
}
