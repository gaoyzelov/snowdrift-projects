package com.snowdrift.orm.mybatisplus.enums;

import com.snowdrift.core.utils.DesensitizeUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * SensitiveEnum
 *
 * @author gaoye
 * @date 2025/03/20 14:26:29
 * @description 脱敏类型枚举
 * @since 1.0.0
 */
@Getter
@AllArgsConstructor
public enum DesensitizeTypeEnum {

    PASSWORD("密码") {
        @Override
        public String mask(String data) {
            return DesensitizeUtil.password(data);
        }
    },
    EMAIL("邮箱") {
        @Override
        public String mask(String data) {
            return DesensitizeUtil.email(data);
        }
    },
    ADDRESS("地址") {
        @Override
        public String mask(String data) {
            return DesensitizeUtil.address(data, 6);
        }
    },
    NAME("姓名") {
        @Override
        public String mask(String data) {
            return DesensitizeUtil.chineseName(data);
        }
    },
    ID_CARD("证件号") {
        @Override
        public String mask(String data) {
            return DesensitizeUtil.idCard(data);
        }
    },
    BANK_CARD("银行卡号") {
        @Override
        public String mask(String data) {
            return DesensitizeUtil.bankCard(data);
        }
    },
    MOBILE("手机号") {
        @Override
        public String mask(String data) {
            return DesensitizeUtil.mobilePhone(data);
        }
    },
    TELEPHONE("座机号") {
        @Override
        public String mask(String data) {
            return DesensitizeUtil.fixedPhone(data);
        }
    },
    IPV4("IPV4地址") {
        @Override
        public String mask(String data) {
            return DesensitizeUtil.ipv4(data);
        }
    },
    IPV6("IPV6地址") {
        @Override
        public String mask(String data) {
            return DesensitizeUtil.ipv6(data);
        }
    },
    VEHICLE_LICENSE("车牌") {
        @Override
        public String mask(String data) {
            return DesensitizeUtil.carLicense(data);
        }
    };

    private final String note;

    public abstract String mask(String data);

}