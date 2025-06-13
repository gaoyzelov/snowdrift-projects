package com.snowdrift.pay.allin.utils;


import com.snowdrift.core.exception.BaseException;
import com.snowdrift.core.utils.SpringUtil;
import com.snowdrift.pay.allin.properties.AllinSybProperties;
import com.snowdrift.pay.allin.service.IAllinSybService;
import com.snowdrift.pay.allin.service.impl.AllinSybServiceImpl;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AllinUtil
 *
 * @author gaoye
 * @date 2025/05/26 09:40:06
 * @description 通联支付工具类，用于创建通联支付服务，适配多个商户配置
 * @since 1.0
 */
public class AllinUtil {

    private static final Map<String, IAllinSybService> SERVICE_MAP = new ConcurrentHashMap<>();

    /**
     * 获取服务
     *
     * @param cusId 商户号
     * @return 服务
     */
    public static IAllinSybService getService(String cusId) {
        if (StringUtils.isBlank(cusId)){
            throw new BaseException("商户号不能为空");
        }
        IAllinSybService allinSybService = SERVICE_MAP.get(cusId);
        if (Objects.isNull(allinSybService)) {
            synchronized (AllinUtil.class) {
                allinSybService = SERVICE_MAP.get(cusId);
                if (Objects.isNull(allinSybService)) {
                    AllinSybProperties prop = SpringUtil.getBean(AllinSybProperties.class);
                    if (Objects.isNull(prop)){
                        throw new BaseException("缺少AllinSybProperties配置");
                    }
                    // 拷贝一份，不影响默认配置
                    AllinSybProperties copyProp = BeanUtil.copyProperties(prop, AllinSybProperties.class);
                    copyProp.setCusId(cusId);
                    allinSybService = new AllinSybServiceImpl(copyProp);
                    SERVICE_MAP.put(cusId, allinSybService);
                }
            }
        }
        return allinSybService;
    }

    /**
     * 获取服务
     *
     * @param prop 通联收银宝配置
     * @return 服务
     */
    public static IAllinSybService getService(AllinSybProperties prop) {
        if (Objects.isNull(prop)){
            throw new BaseException("通联收银宝配置不能为空");
        }
        IAllinSybService allinSybService = SERVICE_MAP.get(prop.getAppId());
        if (Objects.isNull(allinSybService)) {
            synchronized (AllinUtil.class) {
                allinSybService = SERVICE_MAP.get(prop.getAppId());
                if (Objects.isNull(allinSybService)) {
                    allinSybService = new AllinSybServiceImpl(prop);
                    SERVICE_MAP.put(prop.getAppId(), allinSybService);
                }
            }
        }
        return allinSybService;
    }
}