package com.snowdrift.framework.schedule.xxljob.enums;

import com.snowdrift.framework.common.enums.IEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * XXL-JOB 执行器路由策略 — 对应 {@code ExecutorRouteStrategyEnum}
 * <p>
 * 控制任务在多执行器节点间的分配方式。
 * </p>
 *
 * @author gaoye
 * @date 2025/05/19
 * @since 1.0
 */
@Getter
@AllArgsConstructor
public enum RouteStrategyEnum implements IEnum<String> {

    FIRST("FIRST", "第一个"),
    LAST("LAST", "最后一个"),
    ROUND("ROUND", "轮巡"),
    RANDOM("RANDOM", "随机"),
    CONSISTENT_HASH("CONSISTENT_HASH", "一致性哈希"),
    LEAST_FREQUENTLY_USED("LEAST_FREQUENTLY_USED", "最不经常使用"),
    LEAST_RECENTLY_USED("LEAST_RECENTLY_USED", "最近最久未使用"),
    FAILOVER("FAILOVER", "故障转移"),
    BUSYOVER("BUSYOVER", "忙碌转移"),
    SHARDING_BROADCAST("SHARDING_BROADCAST", "分片广播");

    private final String code;
    private final String note;
}
