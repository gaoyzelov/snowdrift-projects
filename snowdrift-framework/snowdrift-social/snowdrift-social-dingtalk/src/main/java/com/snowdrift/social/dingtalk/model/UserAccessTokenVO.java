package com.snowdrift.social.dingtalk.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 *
 * @author 83674
 * @date 2026/10/14-14:56
 * @description
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
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
