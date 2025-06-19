package com.snowdrift.protocol.jt808.util;

import io.netty.buffer.ByteBufUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;

/**
 * ByteUtil
 *
 * @author gaoye
 * @date 2025/06/19 13:16:46
 * @description xxxxxxxx
 * @since 1.0
 */
@Slf4j
public final class ByteUtil {

    /**
     * 字节数组转short
     *
     * @param bytes 字节数组
     * @return short
     */
    public static short bytesToShort(byte[] bytes) {
        if (bytes == null || bytes.length != 2) {
            return -1;
        }
        return (short) ((bytes[0] << 8) | (bytes[1] & 0xff));
    }

    /**
     * short转字节数组
     *
     * @param value short
     * @return 字节数组
     */
    public static byte[] shortToBytes(short value) {
        return new byte[]{(byte) (value >> 8), (byte) value};
    }

    /**
     * 字节数组转BCD码
     * BCD-8421码
     *
     * @param bytes 字节数组
     * @return BCD码
     */
    public static String bytesToBCD(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            // 高4位
            int high = (b >> 4) & 0x0F;
            // 低4位
            int low = b & 0x0F;
            sb.append(high).append(low);
        }
        return sb.toString();
    }

    /**
     * 转换字节数组
     * 0x7e -> 0x7d 0x02
     * 0x7d -> 0x7d 0x01
     *
     * @param bytes 字节数组
     * @return 转换后的字节数组
     */
    public static byte[] transform(byte[] bytes) {
        byte[] transBytes = new byte[bytes.length * 2]; // 最大可能扩容两倍
        int index = 0;
        for (byte b : bytes) {
            if (b == 0x7d) {
                transBytes[index++] = (byte) 0x7d;
                transBytes[index++] = (byte) 0x02;
            } else if (b == 0x7e) {
                transBytes[index++] = (byte) 0x7d;
                transBytes[index++] = (byte) 0x01;
            } else {
                transBytes[index++] = b;
            }
        }
        return ArrayUtils.subarray(transBytes, 0, index);
    }

    /**
     * 还原字节数组
     * 0x7d 0x02 -> 0x7e
     * 0x7d 0x01 -> 0x7d
     *
     * @param bytes 字节数组
     * @return 恢复后的字节数组
     */
    public static byte[] restore(byte[] bytes) {
        boolean valid = crcCheck(bytes);
        if (!valid){
            return null;
        }
        byte[] restoreBytes = new byte[bytes.length]; // 最大可能长度
        int index = 0;
        for (int i = 0; i < bytes.length - 1; ) {
            if (bytes[i] == 0x7d) {
                if (bytes[i + 1] == 0x02) {
                    restoreBytes[index++] = 0x7e;
                    i += 2;
                } else if (bytes[i + 1] == 0x01) {
                    restoreBytes[index++] = 0x7d;
                    i += 2;
                } else {
                    restoreBytes[index++] = bytes[i];
                    i++;
                }
            } else {
                restoreBytes[index++] = bytes[i];
                i++;
            }
        }
        restoreBytes[index++] = bytes[bytes.length - 1];
        return ArrayUtils.subarray(restoreBytes, 0, index);
    }

    /**
     * 校验CRC
     *
     * @param bytes 字节数组
     * @return 是否通过校验
     */
    private static boolean crcCheck(byte[] bytes) {
        byte crcByte = bytes[bytes.length - 1];
        byte calcByte = bytes[0];
        for (int i = 1; i < bytes.length - 1; i++) {
            calcByte ^= bytes[i];
        }
        log.info("接受的校验码:{},计算的校验码:{}", crcByte, calcByte);
        return crcByte == calcByte;
    }

    /**
     * 计算CRC
     *
     * @param bytes 字节数组
     * @return 校验码
     */
    public static byte calcCrc(byte[] bytes) {
        byte crcByte = bytes[0];
        for (int i = 1; i < bytes.length; i++) {
            crcByte ^= bytes[i];
        }
        return crcByte;
    }

    public static void main(String[] args) {
        System.out.println(ByteBufUtil.hexDump(shortToBytes((short) 33024)));
    }
}