package com.snowdrift.framework.oss;

/**
 * OssConst
 *
 * @author gaoyzelov
 * @date 2026/8/7-14:30
 * @description OSS 模块常量
 * @since 1.0.0
 */
public final class OssConst {

    private OssConst() {
    }

    /**
     * 批量操作分区大小（SDK 单次请求上限）
     */
    public static final int BATCH_PARTITION_SIZE = 1000;
}
