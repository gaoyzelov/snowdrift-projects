package com.snowdrift.social.dingtalk.model;

import lombok.*;

import java.io.Serializable;

/**
 * AccessTokenVO
 * @author gaoyzelov
 * @date 2026/10/14-14:22
 * @description 钉钉AccessTokenVO
 * @since 1.0.0
 */
@Data
@Builder
public class AppAccessTokenVO implements Serializable {

    /**
     * 访问token
     */
    @ToString.Exclude
    private String accessToken;

    /**
     * 过期时间, 单位秒
     */
    private Long expiresIn;
}
