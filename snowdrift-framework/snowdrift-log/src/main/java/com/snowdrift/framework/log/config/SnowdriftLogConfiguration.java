package com.snowdrift.framework.log.config;

import com.mzt.logapi.service.ILogRecordService;
import com.snowdrift.framework.log.ILogService;
import com.snowdrift.framework.log.SnowdriftDefaultLogServiceImpl;
import com.snowdrift.framework.log.SnowdriftLogRecordServiceImpl;
import com.snowdrift.framework.log.aspect.ApiLogAspect;
import com.snowdrift.framework.log.aspect.LoginLogAspect;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * SnowdriftLogConfiguration
 *
 * @author gaoyzelov
 * @date 2026/4/30-14:46
 * @description 日志配置
 * @since 1.0.0
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class SnowdriftLogConfiguration {

    /**
     * 默认日志服务
     * @return 日志服务
     */
    @Bean
    @ConditionalOnMissingBean(ILogService.class)
    public ILogService defaultLogService(){
        return new SnowdriftDefaultLogServiceImpl();
    }

    /**
     * 操作日志记录服务（bizlog 事件 → {@link ILogService} 的桥接）
     * <p>
     * 这里**不能**只用 {@code @ConditionalOnMissingBean}：{@code @EnableLogRecord} 通过 {@code @Import}
     * 引入的配置属于用户级配置，会先于自动配置解析并注册它自己的
     * {@code com.mzt.logapi.service.impl.DefaultLogRecordServiceImpl}，条件判断会让本桥接直接退让——结果是 bizlog 事件只打一行 info，
     * {@code ILogService.saveOperateLog} 永远不被调用。
     * </p>
     * <p>
     * 默认场景下本桥接生效（bizlog 按类型注入时拿到本实现）。业务若要自定义操作日志服务：
     * 设置 {@code snowdrift.log.record-service=false} 关掉本 Bean，再注册自己的
     * {@code ILogRecordService}（建议标注 {@code @Primary}）——否则会有两个 {@code @Primary} 导致注入歧义。
     * </p>
     *
     * @param logService 日志服务
     * @return 日志记录服务
     */
    @Bean
    @Primary
    @ConditionalOnProperty(prefix = "snowdrift.log", name = "record-service", havingValue = "true",
            matchIfMissing = true)
    public ILogRecordService logRecordService(ILogService logService) {
        return new SnowdriftLogRecordServiceImpl(logService);
    }

    /**
     * api日志切面
     * <p>业务自定义切面时本 Bean 退让，避免两个切面重复记录。</p>
     *
     * @param logService 日志服务
     * @return api日志切面
     */
    @Bean
    @ConditionalOnMissingBean(ApiLogAspect.class)
    public ApiLogAspect apiLogAspect(ILogService logService) {
        return new ApiLogAspect(logService);
    }

    /**
     * 登录日志切面
     * <p>业务自定义切面时本 Bean 退让，避免两个切面重复记录。</p>
     *
     * @param logService 日志服务
     * @return 登录日志切面
     */
    @Bean
    @ConditionalOnMissingBean(LoginLogAspect.class)
    public LoginLogAspect loginLogAspect(ILogService logService) {
        return new LoginLogAspect(logService);
    }



}
