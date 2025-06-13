package com.snowdrift.pay.allin.vo;


import com.snowdrift.pay.allin.dto.AllinPayDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * WxPayVo
 *
 * @author gaoye
 * @date 2025/05/22 15:35:32
 * @description xxxxxxxx
 * @since 1.0
 */
@Getter
@Setter
@Schema
public class CashierPayVo extends AllinPayDto {

    @Schema(title = "付款金额，单位为分")
    private Long trxamt;

    @Schema(title = "商户订单号")
    private String reqsn;

    @Schema(title = "服务器异步通知页面路径")
    private String notify_url;

    @Schema(title = "订单标题")
    private String body;

    @Schema(title = "订单备注")
    private String remark;

    @Schema(title = "validtime")
    private String validtime;

    @Schema(title = "支付方式,微信：W06，支付宝：A02")
    private String paytype;

    @Schema(title = "支付限制")
    private String limit_pay;

    @Schema(title = "随机字符串")
    private String randomstr;

    @Schema(title = "签名类型")
    private String signtype;

    @Schema(title = "是否直接支付，直接支付 0或空时不变")
    private Integer isdirectpay;

    @Schema(title = "是否禁止分享，1禁止分享 0或空时支持分享")
    private Integer ishideshare;

    @Schema(title = "版本号，目前只支持12")
    private String version;

    @Schema(title = "签名")
    private String sign;
}