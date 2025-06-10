package com.snowdrift.pay.yee.provider;

import com.snowdrift.pay.yee.properties.YeeProperties;
import com.yeepay.yop.sdk.base.auth.credentials.provider.YopFixedCredentialsProvider;
import com.yeepay.yop.sdk.base.config.YopAppConfig;
import com.yeepay.yop.sdk.config.enums.CertStoreType;
import com.yeepay.yop.sdk.config.provider.file.YopCertConfig;
import com.yeepay.yop.sdk.security.CertTypeEnum;

import java.util.ArrayList;
import java.util.List;

/**
 * CredentialsProvider
 *
 * @author gaoye
 * @date 2025/06/05 19:30:59
 * @description xxxxxxxx
 * @since 1.0
 */
public class CredentialsProvider extends YopFixedCredentialsProvider {

    private final YeeProperties prop;

    public CredentialsProvider(YeeProperties prop) {
        this.prop = prop;
    }

    @Override
    protected YopAppConfig loadAppConfig(String appKey) {
        YopAppConfig yopAppConfig = new YopAppConfig();
        yopAppConfig.setAppKey(prop.getAppKey());
        // RSA2048 example
        YopCertConfig certConfig = new YopCertConfig();
        certConfig.setCertType(CertTypeEnum.RSA2048);
        certConfig.setStoreType(CertStoreType.STRING);
        certConfig.setValue(prop.getPrivateKey());
        // load into sdk config
        List<YopCertConfig> isvPrivateKeys = new ArrayList<>();
        isvPrivateKeys.add(certConfig);
        yopAppConfig.setIsvPrivateKey(isvPrivateKeys);
        return yopAppConfig;
    }
}