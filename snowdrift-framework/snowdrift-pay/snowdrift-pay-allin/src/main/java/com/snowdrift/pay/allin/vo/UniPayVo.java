package com.snowdrift.pay.allin.vo;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * UniPayVo
 *
 * @author gaoye
 * @date 2025/05/23 15:08:10
 * @description xxxxxxxx
 * @since 1.0
 */
@Getter
@Setter
@Schema(description = "统一支付响应数据")
public class UniPayVo extends AllinPayVo {

    @Schema(title = "商户号")
    private String cusid;

    @Schema(title = "应用ID")
    private String appid;

    @Schema(title = "交易单号")
    private String trxid;

    @Schema(title = "渠道平台交易单号")
    private String chnltrxid;

    @Schema(title = "商户交易单号")
    private String reqsn;

    @Schema(title = "随机字符串")
    private String randomstr;

    @Schema(title = "交易状态")
    private String trxstatus;

    @Schema(title = "交易完成时间")
    private String fintime;

    @Schema(title = "错误原因")
    private String errmsg;

    /**
     * 扫码支付则返回二维码串，js支付则返回json字符串
     * 云闪付的JS支付返回支付的链接,商户只需跳转到此链接即可完成支付
     * 支付宝App支付返回支付信息串
     */
    @Schema(title = "支付串")
    private String payinfo;

    @Schema(title = "交易类型")
    private String trxcode;

    @Schema(title = "签名")
    private String sign;
}