package com.snowdrift.social.dingtalk.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * AccessTokenVO
 * @author gaoyzelov
 * @date 2026/10/14-14:22
 * @description 钉钉AccessTokenVO
 * @since 1.0.0
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
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
