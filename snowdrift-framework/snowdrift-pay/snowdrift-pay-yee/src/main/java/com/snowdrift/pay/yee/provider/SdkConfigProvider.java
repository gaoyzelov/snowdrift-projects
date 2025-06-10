package com.snowdrift.pay.yee.provider;

import com.snowdrift.pay.yee.properties.YeeProperties;
import com.yeepay.yop.sdk.base.config.provider.YopFixedSdkConfigProvider;
import com.yeepay.yop.sdk.config.YopSdkConfig;

/**
 * SdkConfigProvider
 *
 * @author gaoye
 * @date 2025/06/05 19:30:06
 * @description xxxxxxxx
 * @since 1.0
 */
public class SdkConfigProvider extends YopFixedSdkConfigProvider {

    private final YeeProperties prop;
    public SdkConfigProvider(YeeProperties prop) {
        this.prop = prop;
    }


    @Override
    protected YopSdkConfig loadSdkConfig() {
        YopSdkConfig yopSdkConfig = new YopSdkConfig();
        yopSdkConfig.setServerRoot(prop.getServerRoot());
        yopSdkConfig.setYosServerRoot(prop.getServerRoot());
        return yopSdkConfig;
    }

    @Override
    public void removeConfig(String key) {

    }
}