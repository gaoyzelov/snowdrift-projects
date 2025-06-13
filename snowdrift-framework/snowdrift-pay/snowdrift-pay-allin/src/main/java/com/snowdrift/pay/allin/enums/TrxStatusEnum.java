package com.snowdrift.pay.allin.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/**
 * TrxStatusEnum
 * 仅限收银台API接口使用
 * @author gaoye
 * @date 2025/05/22 13:25:51
 * @description xxxxxxxx
 * @since 1.0
 */
@Getter
@AllArgsConstructor
public enum TrxStatusEnum {

    OK("0000", "交易成功"),
    NOT_EXIST("1001", "交易不存在"),
    PROCESSING("2000", "交易处理中"),
    PROCESSING_1("2008", "交易处理中"),
    LESS_FEE("3014", "交易金额小于应收手续费"),
    REAL_NAME_FAIL("3031", "校验实名信息失败"),
    OTHER("3045", "其他错误，具体看errmsg"),
    CANCEL("3050", "交易已被撤销"),
    UNPAID("3088", "交易未支付"),
    CANCEL_ERR("3089", "撤销异常"),
    CHL_ERR("3099", "渠道商户错误"),
    DUPLICATE_TRX_NO("3888", "流水号重复"),
    CTRL_FAIL("3889", "交易控制失败，具体原因看errmsg"),
    OTHER_1("3999", "其他错误，具体看errmsg");

    private static final Map<String, TrxStatusEnum> CODE_MAP;

    static {
        CODE_MAP = new HashMap<>(TrxStatusEnum.values().length);
        for (TrxStatusEnum value : TrxStatusEnum.values()) {
            CODE_MAP.put(value.getCode(), value);
        }
    }

    private final String code;

    private final String note;

    public static TrxStatusEnum getByCode(String code) {
        return CODE_MAP.get(code);
    }
}