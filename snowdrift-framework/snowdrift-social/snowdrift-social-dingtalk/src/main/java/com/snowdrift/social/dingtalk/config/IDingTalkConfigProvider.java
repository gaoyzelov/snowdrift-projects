package com.snowdrift.social.dingtalk.config;

import com.snowdrift.framework.base.exception.BizException;

/**
 * 钉钉配置来源扩展点。数据库适配由下游实现，本模块不绑定具体 ORM 或表结构。
 */
public interface IDingTalkConfigProvider {

    /**
     * 加载全部配置及当前激活标识。返回值应代表一个一致的配置快照。
     */
    DingTalkConfigSnapshot load();

    /**
     * 持久化/更新当前激活标识。
     * <p>
     * 配置文件默认实现只在当前进程内切换；数据库实现应更新权威存储。
     * 多实例应用还应通过消息通知或轮询让其他实例刷新。
     */
    void activate(String configKey) throws BizException;
}
