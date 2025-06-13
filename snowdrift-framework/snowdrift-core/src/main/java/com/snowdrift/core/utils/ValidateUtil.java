package com.snowdrift.core.utils;

import com.snowdrift.core.constant.RegexConst;
import com.snowdrift.core.exception.BaseException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import lombok.NonNull;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.SetUtils;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * ValidateUtil
 *
 * @author gaoye
 * @date 2025/06/09 19:44:52
 * @description 校验工具类
 * @since 1.0
 */
public final class ValidateUtil {

    private ValidateUtil() {
    }

    // 地区编码
    public static final Set<String> AREA_SET = SetUtils.hashSet(
            "11", "12", "13", "14", "15", "21", "22", "23", "31", "32", "33", "34", "35", "36", "37", "41",
            "42", "43", "44", "45", "46", "50", "51", "52", "53", "54", "61", "62", "63", "64", "65", "71",
            "81", "82", "91");
    // 校验码权重
    public static final int[] WEIGHT = {7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2};
    // 校验码对应值
    public static final String[] CHECK_CODE = {"1", "0", "X", "9", "8", "7", "6", "5", "4", "3", "2"};

    /**
     * 校验身份证号码格式
     *
     * @param idCard 身份证号码
     */
    public static void checkIdCard(String idCard) {
        if (StringUtils.isBlank(idCard) || (idCard.length() != 15 && idCard.length() != 18)) {
            throw new BaseException("身份证号码有误，请检查");
        }
        boolean matches = Pattern.matches(RegexConst.CARD_NO, idCard);
        if (!matches) {
            throw new BaseException("身份证号码有误，请检查");
        }
        int cardLength = idCard.length();
        // 校验地区码
        String areaCode = idCard.substring(0, 2);
        if (!AREA_SET.contains(areaCode)) {
            throw new BaseException("身份证号码有误，请检查");
        }
        // 校验出生年月日
        String yearStr = cardLength == 15 ? "19" + idCard.substring(6, 8) : idCard.substring(6, 10);
        int year = Integer.parseInt(yearStr);
        if (year < 1900 || year > LocalDate.now().getYear()) {
            throw new BaseException("身份证号码有误，请检查");
        }
        String monthStr = cardLength == 15 ? idCard.substring(8, 10) : idCard.substring(10, 12);
        int month = Integer.parseInt(monthStr);
        if (month < 1 || month > 12) {
            throw new BaseException("身份证号码有误，请检查");
        }
        String dayStr = cardLength == 15 ? idCard.substring(10, 12) : idCard.substring(12, 14);
        int day = Integer.parseInt(dayStr);
        if (day < 1 || day > 31) {
            throw new BaseException("身份证号码有误，请检查");
        }
        // 合法日期校验
        String date = yearStr + monthStr + dayStr;
        try {
            DateTimeUtil.parseDate(date, "yyyyMMdd");
        } catch (Exception e) {
            throw new BaseException("身份证号码有误，请检查");
        }
        // 校验校验码
        if (cardLength == 15) {
            return;
        }
        int sum = 0;
        for (int i = 0; i < 17; i++) {
            char c = idCard.charAt(i);
            if (c < '0' || c > '9') {
                throw new BaseException("身份证号码有误，请检查");
            }
            sum += (c - '0') * WEIGHT[i];
        }
        int mod = sum % 11;
        String checkCode = CHECK_CODE[mod];
        if (!StringUtils.equalsIgnoreCase(checkCode, idCard.substring(17))) {
            throw new BaseException("身份证号码有误，请检查");
        }
    }


    /**
     * 校验手机号格式
     *
     * @param phone 手机号
     */
    public static void checkPhone(String phone) {
        if (StringUtils.isBlank(phone)) {
            throw new BaseException("手机号不能为空");
        }
        boolean matches = Pattern.matches(RegexConst.PHONE, phone);
        if (!matches) {
            throw new BaseException("手机号格式不正确");
        }
    }

    /**
     * 校验邮箱格式
     *
     * @param email 邮箱
     */
    public static void checkEmail(String email) {
        if (StringUtils.isBlank(email)) {
            throw new BaseException("邮箱不能为空");
        }
        boolean matches = Pattern.matches(RegexConst.EMAIL, email);
        if (!matches) {
            throw new BaseException("邮箱格式不正确");
        }
    }

    /**
     * 校验URL格式
     *
     * @param url URL
     */
    public static void checkUrl(String url) {
        if (StringUtils.isBlank(url)) {
            throw new BaseException("URL不能为空");
        }
        boolean matches = Pattern.matches(RegexConst.URL, url);
        if (!matches) {
            throw new BaseException("URL格式不正确");
        }
    }

    /**
     * 校验时间范围
     *
     * @param start 开始时间
     * @param end   结束时间
     */
    public static void checkTimeRange(LocalTime start, LocalTime end){
        if (Objects.isNull(start) || Objects.isNull(end)) {
            throw new BaseException("开始或结束时间不能为空");
        }
        if (start.isAfter(end)) {
            throw new BaseException("开始时间不能晚于结束时间");
        }
    }

    /**
     * 校验日期范围
     *
     * @param start 开始日期
     * @param end   结束日期
     */
    public static void checkDateRange(LocalDate start, LocalDate end) {
        if (Objects.isNull(start) || Objects.isNull(end)) {
            throw new BaseException("开始或结束日期不能为空");
        }
        if (start.isAfter(end)) {
            throw new BaseException("开始日期不能晚于结束日期");
        }
    }

    /**
     * 校验日期时间范围
     *
     * @param start 开始时间
     * @param end   结束时间
     */
    public static void checkDateTimeRange(LocalDateTime start, LocalDateTime end) {
        if (Objects.isNull(start) || Objects.isNull(end)) {
            throw new BaseException("开始或结束日期时间不能为空");
        }
        if (start.isAfter(end)) {
            throw new BaseException("开始日期时间不能晚于结束日期时间");
        }
    }

    /**
     * 校验对象参数
     *
     * @param t 对象
     * @return 错误信息
     */
    public static <T> Set<String> validate(@NonNull T t){
        Set<String> errors = Collections.emptySet();
        try (ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory()){
            Validator validator = validatorFactory.getValidator();
            Set<ConstraintViolation<T>> cvs = validator.validate(t);
            if (CollectionUtils.isNotEmpty(cvs)) {
                errors = cvs.stream().map(ConstraintViolation::getMessage).collect(Collectors.toSet());
            }
        }
        return errors;
    }
}