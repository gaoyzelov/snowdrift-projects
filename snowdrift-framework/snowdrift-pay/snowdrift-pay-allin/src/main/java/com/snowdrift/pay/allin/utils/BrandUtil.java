package com.snowdrift.pay.allin.utils;

import com.snowdrift.core.exception.BaseException;
import com.snowdrift.core.utils.SpringUtil;
import com.snowdrift.pay.allin.properties.AllinBrandProperties;
import com.snowdrift.pay.allin.service.IAllinBrandService;
import com.snowdrift.pay.allin.service.impl.AllinBrandServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * BrandUtil
 *
 * @author gaoye
 * @date 2025/05/29 13:48:46
 * @description xxxxxxxx
 * @since 1.0
 */
@Slf4j
public class BrandUtil {

    private static final Map<String, IAllinBrandService> SERVICE_MAP = new ConcurrentHashMap<>();

    /**
     * 获取服务
     *
     * @param appId 应用ID
     * @return 服务
     */
    public static IAllinBrandService getService(String appId,String appKey) {
        if (StringUtils.isBlank(appId) || StringUtils.isBlank(appKey)) {
            throw new BaseException("应用ID或应用Key不能为空");
        }
        IAllinBrandService allinBrandService = SERVICE_MAP.get(appId);
        if (Objects.isNull(allinBrandService)) {
            synchronized (AllinUtil.class) {
                allinBrandService = SERVICE_MAP.get(appId);
                if (Objects.isNull(allinBrandService)) {
                    AllinBrandProperties prop = SpringUtil.getBean(AllinBrandProperties.class);
                    if (Objects.isNull(prop)){
                        prop = new AllinBrandProperties();
                    }
                    // 拷贝一份
                    AllinBrandProperties copyProp = BeanUtil.copyProperties(prop, AllinBrandProperties.class);
                    copyProp.setAppId(appId);
                    copyProp.setAppKey(appKey);
                    allinBrandService = new AllinBrandServiceImpl(copyProp);
                    SERVICE_MAP.put(appId, allinBrandService);
                }
            }
        }
        return allinBrandService;
    }

    /**
     * 获取服务
     *
     * @param prop 通联智品牌配置
     * @return 服务
     */
    public static IAllinBrandService getService(AllinBrandProperties prop) {
        if (Objects.isNull(prop)) {
            throw new BaseException("通联收银宝配置不能为空");
        }
        IAllinBrandService allinBrandService = SERVICE_MAP.get(prop.getAppId());
        if (Objects.isNull(allinBrandService)) {
            synchronized (AllinUtil.class) {
                allinBrandService = SERVICE_MAP.get(prop.getAppId());
                if (Objects.isNull(allinBrandService)) {
                    allinBrandService = new AllinBrandServiceImpl(prop);
                    SERVICE_MAP.put(prop.getAppId(), allinBrandService);
                }
            }
        }
        return allinBrandService;
    }

    /**
     * 签名
     *
     * @param param 参数
     */
    public static String sign(TreeMap<String, String> param) {
        StringJoiner joiner = new StringJoiner("&");
        for (Map.Entry<String, String> each : param.entrySet()) {
            joiner.add(StringUtils.join(each.getKey(), "=", each.getValue()));
        }
        log.info("签名原文 : [{}]", joiner.toString());
        return DigestUtils.md5Hex(joiner.toString());
    }

    /**
     * 验签
     *
     * @param param 参数
     */
    public static boolean verify(TreeMap<String, String> param) {
        String originalSign = param.remove("sign");
        String sign = sign(param);
        log.info("原密文 : [{}], MD5签名结果 : [{}]", originalSign, sign);
        boolean verify = StringUtils.equals(originalSign, sign);
        log.info("签名验证结果: [{}]", verify ? "通过" : "不通过");
        return verify;
    }
}