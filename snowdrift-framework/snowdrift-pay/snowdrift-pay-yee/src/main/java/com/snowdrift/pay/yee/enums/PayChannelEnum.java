package com.snowdrift.pay.yee.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/**
 * CodeEnum
 *
 * @author gaoye
 * @date 2025/06/06 09:21:25
 * @description xxxxxxxx
 * @since 1.0
 */
@Getter
@AllArgsConstructor
public enum PayChannelEnum {

    WECHAT("WECHAT", "微信"),
    ALIPAY("ALIPAY", "支付宝"),
    OPEN_UPOP("OPEN_UPOP", "银联"),
    APPLEPAY("APPLEPAY", "苹果支付仅聚合支付返回该参数"),
    NCPAY("NCPAY", "无卡支付"),
    ACCOUNTPAY("ACCOUNTPAY", "账户支付"),
    MEMBERPAY("MEMBERPAY", "会员支付"),
    NET("NET", "网银"),
    DCEP("DCEP", "数字人民币");

    private static final Map<String, PayChannelEnum> CODE_MAP;
    static {
        CODE_MAP = new HashMap<>();
        for (PayChannelEnum value : PayChannelEnum.values()) {
            CODE_MAP.put(value.getCode(), value);
        }
    }

    private final String code;

    private final String note;

    public PayChannelEnum getByCode(String code) {
        return CODE_MAP.get(code);
    }
}