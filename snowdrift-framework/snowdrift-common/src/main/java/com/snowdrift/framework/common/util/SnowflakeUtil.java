package com.snowdrift.framework.common.util;

import com.snowdrift.framework.common.exception.BizException;

/**
 * SnowflakeUtil
 *
 * @author 83674
 * @date 2026/3/31-15:21
 * @description 雪花ID工具类 参考地址：https://www.cnblogs.com/relucent/p/4955340.html
 * @since 1.0.0
 */
public final class SnowflakeUtil {

    // ==============================Fields===========================================
    /**
     * 开始时间戳 (2015-01-01)
     */
    private final long twepoch = 1420041600000L;

    /**
     * 能容忍的系统时钟回拨时间（2秒）
     */
    private final long timeOffset = 2000L;

    /**
     * 机器id所占的位数
     */
    private final long workerIdBits = 5L;

    /**
     * 数据标识id所占的位数
     */
    private final long datacenterIdBits = 5L;

    /**
     * 支持的最大机器id，结果是31 (这个移位算法可以很快的计算出几位二进制数所能表示的最大十进制数)
     */
    private final long maxWorkerId = -1L ^ (-1L << workerIdBits);

    /**
     * 支持的最大数据标识id，结果是31
     */
    private final long maxDatacenterId = -1L ^ (-1L << datacenterIdBits);

    /**
     * 序列在id中占的位数
     */
    private final long sequenceBits = 12L;

    /**
     * 机器ID向左移12位
     */
    private final long workerIdShift = sequenceBits;

    /**
     * 数据标识id向左移17位(12+5)
     */
    private final long datacenterIdShift = sequenceBits + workerIdBits;

    /**
     * 时间戳向左移22位(5+5+12)
     */
    private final long timestampLeftShift = sequenceBits + workerIdBits + datacenterIdBits;

    /**
     * 生成序列的掩码，这里为4095 (0b111111111111=0xfff=4095)
     */
    private final long sequenceMask = -1L ^ (-1L << sequenceBits);

    /**
     * 工作机器ID(0~31)
     */
    private long workerId;

    /**
     * 数据中心ID(0~31)
     */
    private long datacenterId;

    /**
     * 毫秒内序列(0~4095)
     */
    private long sequence = 0L;

    /**
     * 上次生成ID的时间戳
     */
    private long lastTimestamp = -1L;

    // ==============================Constructors=====================================

    private SnowflakeUtil() {
    }
    /**
     * 构造函数
     *
     * @param workerId     工作ID (0~31)
     * @param datacenterId 数据中心ID (0~31)
     */
    private SnowflakeUtil(long workerId, long datacenterId) {
        if (workerId > maxWorkerId || workerId < 0) {
            throw new BizException(String.format("worker Id can't be greater than %d or less than 0", maxWorkerId));
        }
        if (datacenterId > maxDatacenterId || datacenterId < 0) {
            throw new BizException(String.format("datacenter Id can't be greater than %d or less than 0", maxDatacenterId));
        }
        this.workerId = workerId;
        this.datacenterId = datacenterId;
    }

    // ==============================Methods==========================================

    /**
     * 获得下一个ID (该方法是线程安全的)
     *
     * @return SnowflakeId
     */
    public synchronized long nextId() {
        long timestamp = timeGen();

        // 当前时间戳小于上一次ID生成的时间戳
        if (timestamp < lastTimestamp) {
            long offset = lastTimestamp - timestamp;
            // 系统时钟回退在容忍范围内，例如出现过闰秒
            if (offset < timeOffset) {
                timestamp = lastTimestamp;
            }
            // 系统时钟回退超出容忍范围，抛出异常
            else {
                throw new BizException(String.format("Clock moved backwards. Refusing to generate id for %d milliseconds", offset));
            }
        }

        // 如果是同一时间生成的，则进行毫秒内序列
        if (lastTimestamp == timestamp) {
            sequence = (sequence + 1) & sequenceMask;
            // 毫秒内序列溢出
            if (sequence == 0) {
                // 阻塞到下一个毫秒,获得新的时间戳
                timestamp = tilNextMillis(lastTimestamp);
            }
        }
        // 时间戳改变，毫秒内序列重置
        else {
            sequence = 0L;
        }

        // 上次生成ID的时间戳
        lastTimestamp = timestamp;

        // 移位并通过或运算拼到一起组成64位的ID
        return ((timestamp - twepoch) << timestampLeftShift) //
                | (datacenterId << datacenterIdShift) //
                | (workerId << workerIdShift) //
                | sequence;
    }

    /**
     * 阻塞到下一个毫秒，直到获得新的时间戳
     *
     * @param lastTimestamp 上次生成ID的时间戳
     * @return 当前时间戳
     */
    private long tilNextMillis(long lastTimestamp) {
        long timestamp = timeGen();
        while (timestamp <= lastTimestamp) {
            timestamp = timeGen();
        }
        return timestamp;
    }

    /**
     * 返回以毫秒为单位的当前时间
     *
     * @return 当前时间(毫秒)
     */
    private long timeGen() {
        return System.currentTimeMillis();
    }

    /**
     * 获取实例（使用默认 workerId=0, datacenterId=0）
     * <p>
     * <b>注意：</b>多节点/多JVM部署时，使用此方法创建的实例会产生重复ID。
     * 分布式环境下请使用 {@link #getInstance(long, long)} 为每个节点显式指定
     * 不同的 workerId 和 datacenterId。
     * </p>
     *
     * @return SnowflakeUtil 实例
     */
    public static SnowflakeUtil getInstance() {
        return new SnowflakeUtil();
    }

    /**
     * 获取实例
     *
     * @param workerId     工作ID (0~31)
     * @return SnowflakeUtil
     */
    public static SnowflakeUtil getInstance(long workerId) {
        return new SnowflakeUtil(workerId, 31);
    }

    /**
     * 获取实例
     *
     * @param workerId     工作ID (0~31)
     * @param datacenterId 数据中心ID (0~31)
     * @return SnowflakeUtil
     */
    public static SnowflakeUtil getInstance(long workerId, long datacenterId) {
        return new SnowflakeUtil(workerId, datacenterId);
    }
}
