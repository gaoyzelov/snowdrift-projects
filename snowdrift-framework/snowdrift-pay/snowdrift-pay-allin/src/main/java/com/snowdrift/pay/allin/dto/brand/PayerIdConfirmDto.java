package com.snowdrift.pay.allin.dto.brand;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * PayerIdConfirmDto
 *
 * @author gaoye
 * @date 2025/06/10 11:24:57
 * @description xxxxxxxx
 * @since 1.0
 */
@Data
@Schema(title = "确认验证付款方ID参数")
public class PayerIdConfirmDto implements Serializable {

    @Schema(title = "系统编号",required = true)
    @NotBlank(message = "系统编号不能为空")
    private String sysId;

    @Schema(title = "外部系统付款方编号,外部系统唯一",required = true)
    @NotBlank(message = "外部系统付款方编号不能为空")
    private String customerUserId;

    @Schema(title = "付款方ID",required = true)
    @NotBlank(message = "付款方ID不能为空")
    private String bizUserId;

    /**
     * 付款方账户类型
     * weChatPublic-微信公众号
     * weChatMiniProgram-微信小程序
     * aliPayService-支付宝生活号
     * unionPayjs-银联JS
     * phone-手机号
     */
    @Schema(title = "支付账户类型",required = true)
    @NotBlank(message = "支付账户类型不能为空")
    private String accountType;

    /**
     * 微信公众号支付openid——微信分配
     * 微信小程序支付openid——微信分配
     * 支付宝生活号支付user_id——支付宝分配
     * 银联JS支付user_id——银联分配
     * phone——手机号
     */
    @Schema(title = "支付账户",required = true)
    @NotBlank(message = "支付账户不能为空")
    private String account;

    @Schema(title = "验证码",required = true)
    @NotBlank(message = "验证码不能为空")
    private String code;
}