package com.snowdrift.pay.allin.dto.brand;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * PayerIdDto
 *
 * @author gaoye
 * @date 2025/06/05 09:35:18
 * @description xxxxxxxx
 * @since 1.0
 */
@Data
@Schema(title = "获取付款方id参数")
public class PayerIdDto implements Serializable {

    @Schema(title = "系统编号", required = true)
    @NotBlank(message = "系统编号不能为空")
    private String sysId;

    @Schema(title = "外部系统付款方编号", required = true)
    @NotBlank(message = "外部系统付款方编号不能为空")
    private String customerUserId;

    /**
     * weChatPublic-微信公众号
     * weChatMiniProgram -微信小程序
     * aliPayService -支付宝生活号
     * unionPayjs -银联JS
     * phone-手机号
     */
    @Schema(title = "支付账户类型", required = true)
    @NotBlank(message = "支付账户类型不能为空")
    private String accountType;

    /**
     * 微信公众号支付openid
     * 微信小程序支付openid
     * 支付宝生活号支付user_id
     * 银联JS支付user_id
     * 手机号
     */
    @Schema(title = "支付账户", required = true)
    @NotBlank(message = "支付账户不能为空")
    private String account;
}