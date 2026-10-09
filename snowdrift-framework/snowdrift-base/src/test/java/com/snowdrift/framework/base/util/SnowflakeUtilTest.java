package com.snowdrift.framework.base.util;

import com.snowdrift.framework.base.exception.BizException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SnowflakeUtilTest
 *
 * @author gaoyzelov
 * @date 2026/10/9
 * @description 雪花 ID 工具类单元测试
 * @since 1.0.0
 */
class SnowflakeUtilTest {

    /** 位布局：1 位符号 + 41 位时间戳 + 5 位 datacenterId + 5 位 workerId + 12 位序列 */
    private static final int WORKER_ID_SHIFT = 12;
    private static final int DATACENTER_ID_SHIFT = 17;
    private static final long ID_MASK = 0x1F;

    @Test
    @DisplayName("相同编号复用同一实例，不同编号相互独立")
    void instancesAreCached() {
        assertThat(SnowflakeUtil.getInstance(1, 2)).isSameAs(SnowflakeUtil.getInstance(1, 2));
        assertThat(SnowflakeUtil.getInstance(1, 2)).isNotSameAs(SnowflakeUtil.getInstance(2, 1));
        assertThat(SnowflakeUtil.getInstance()).isSameAs(SnowflakeUtil.getInstance(0, 0));
        assertThat(SnowflakeUtil.getInstance(3)).isSameAs(SnowflakeUtil.getInstance(3, 0));
    }

    @Test
    @DisplayName("ID 中可解析出指定的 workerId 与 datacenterId")
    void bitLayout() {
        SnowflakeUtil snowflake = SnowflakeUtil.getInstance(5, 7);
        long id = snowflake.nextId();

        assertThat((id >> WORKER_ID_SHIFT) & ID_MASK).isEqualTo(5L);
        assertThat((id >> DATACENTER_ID_SHIFT) & ID_MASK).isEqualTo(7L);
        assertThat(id).isPositive();
    }

    @Test
    @DisplayName("单实例连续生成的 ID 严格递增且唯一")
    void monotonicallyIncreasing() {
        SnowflakeUtil snowflake = SnowflakeUtil.getInstance(9, 9);

        long previous = snowflake.nextId();
        Set<Long> ids = new HashSet<>();
        ids.add(previous);
        for (int i = 0; i < 5_000; i++) {
            long current = snowflake.nextId();
            assertThat(current).isGreaterThan(previous);
            previous = current;
            ids.add(current);
        }
        assertThat(ids).hasSize(5_001);
    }

    @Test
    @DisplayName("并发调用不产生重复 ID")
    void uniqueUnderConcurrency() throws Exception {
        int threads = 4;
        int perThread = 2_000;
        SnowflakeUtil snowflake = SnowflakeUtil.getInstance(11, 13);

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Callable<List<Long>>> tasks = new ArrayList<>();
            for (int t = 0; t < threads; t++) {
                tasks.add(() -> {
                    start.await(5, TimeUnit.SECONDS);
                    List<Long> local = new ArrayList<>(perThread);
                    for (int i = 0; i < perThread; i++) {
                        local.add(snowflake.nextId());
                    }
                    return local;
                });
            }

            List<Future<List<Long>>> futures = new ArrayList<>();
            for (Callable<List<Long>> task : tasks) {
                futures.add(pool.submit(task));
            }
            start.countDown();

            Set<Long> all = Collections.synchronizedSet(new HashSet<>());
            for (Future<List<Long>> future : futures) {
                all.addAll(future.get(30, TimeUnit.SECONDS));
            }
            assertThat(all).hasSize(threads * perThread);
        } finally {
            pool.shutdownNow();
            assertThat(pool.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        }
    }

    @Test
    @DisplayName("编号越界抛 BizException，且不会污染实例缓存")
    void invalidIds() {
        assertThatThrownBy(() -> SnowflakeUtil.getInstance(32, 0))
                .isInstanceOf(BizException.class)
                .hasMessage("workerId 不能大于 31 或小于 0");
        assertThatThrownBy(() -> SnowflakeUtil.getInstance(-1, 0))
                .isInstanceOf(BizException.class)
                .hasMessage("workerId 不能大于 31 或小于 0");
        assertThatThrownBy(() -> SnowflakeUtil.getInstance(0, 32))
                .isInstanceOf(BizException.class)
                .hasMessage("datacenterId 不能大于 31 或小于 0");
        assertThatThrownBy(() -> SnowflakeUtil.getInstance(0, -1))
                .isInstanceOf(BizException.class)
                .hasMessage("datacenterId 不能大于 31 或小于 0");

        // 越界组合未进入缓存，后续合法调用仍可正常工作
        Set<Long> ids = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            ids.add(SnowflakeUtil.getInstance(31, 31).nextId());
        }
        assertThat(ids).hasSize(100);
    }
}
