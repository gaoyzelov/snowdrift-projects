package com.snowdrift.core.utils;

import com.snowdrift.core.exception.BaseException;
import org.apache.commons.lang3.StringUtils;

/**
 * BaseUtil
 *
 * @author 83674
 * @version v1.0.0
 * @date 2024/9/14 9:36
 */
public class CommonUtil {

    /**
     * 升级身份证号码
     * 15位升级为18位
     *
     * @param idCard 旧身份证号码
     * @return 新身份证号码
     */
    public static String upgradeIdCard(String idCard) {
        if (StringUtils.isBlank(idCard) || idCard.length() != 15) {
            throw new BaseException("身份证号码有误，请检查");
        }
        ValidateUtil.checkIdCard(idCard);
        String newIdCard = idCard.substring(0, 6) + "19" + idCard.substring(6);
        int sum = 0;
        for (int i = 0; i < newIdCard.length(); i++) {
            char c = newIdCard.charAt(i);
            sum += (c - '0') * ValidateUtil.WEIGHT[i];
        }
        int mod = sum % 11;
        return newIdCard + ValidateUtil.CHECK_CODE[mod];
    }

}