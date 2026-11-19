package com.snowdrift.social.dingtalk.model;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

/**
 *
 * @author gaoyzelov
 * @date 2026/7/3-18:09
 * @description
 * @since 1.0.0
 */
@Data
@Builder
public class UserInfoVO implements Serializable {

    /**
     * 头像URL
     */
    public String avatarUrl;

    /**
     * 用户的个人邮箱
     */
    public String email;

    /**
     * 用户的手机号
     */
    public String mobile;

    /**
     * 用户的钉钉昵称
     */
    public String nick;

    /**
     * 用户的openId
     */
    public String openId;

    /**
     * 手机号对应的国家号
     */
    public String stateCode;

    /**
     * 用户的unionId
     */
    public String unionId;
}
