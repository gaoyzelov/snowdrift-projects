package com.snowdrift.pay.allin.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/**
 * PayTypeEnum
 *
 * @author gaoye
 * @date 2025/05/22 16:17:14
 * @description xxxxxxxx
 * @since 1.0
 */
@Getter
@AllArgsConstructor
public enum PayCauseEnum {
    BRAND_SHARE("00", "品牌方分账"),
    AGENT_SHARE("01", "代理商分账"),
    PARTNERS_SHARE("02", "联营商分账"),
    DEALER_SHARE("03", "经销商分账"),
    STORE_SHARE("04", "门店分账"),
    OTHER("99", "支付宝扫码支付");

    private static final Map<String, PayCauseEnum> CODE_MAP;
    static {
        CODE_MAP = new HashMap<>(PayCauseEnum.values().length);
        for (PayCauseEnum payTypeEnum : PayCauseEnum.values()) {
            CODE_MAP.put(payTypeEnum.getCode(), payTypeEnum);
        }
    }

    private final String code;
    private final String note;

    public static PayCauseEnum getByCode(String code){
        return CODE_MAP.get(code);
    }
}