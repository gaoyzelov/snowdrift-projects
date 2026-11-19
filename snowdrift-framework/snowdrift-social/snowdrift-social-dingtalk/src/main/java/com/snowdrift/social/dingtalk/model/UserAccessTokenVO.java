package com.snowdrift.social.dingtalk.model;

import lombok.*;

import java.io.Serializable;

/**
 * UserAccessTokenVO
 * @author gaoyzelov
 * @date 2026/10/14-14:56
 * @description 钉钉用户访问令牌VO
 * @since 1.0.0
 */
@Data
@Builder
public class UserAccessTokenVO implements Serializable {

    /**
     * 访问token
     */
    @ToString.Exclude
    public String accessToken;
    /**
     * 刷新token
     */
    @ToString.Exclude
    public String refreshToken;
    /**
     * 企业corpId
     */
    public String corpId;
    /**
     * 过期时间, 单位秒
     */
    public Long expireIn;
}
