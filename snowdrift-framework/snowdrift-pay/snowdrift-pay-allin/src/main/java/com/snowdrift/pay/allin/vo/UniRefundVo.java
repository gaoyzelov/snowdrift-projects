package com.snowdrift.pay.allin.vo;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * UniRefundVo
 *
 * @author gaoye
 * @date 2025/05/23 17:07:11
 * @description xxxxxxxx
 * @since 1.0
 */
@Getter
@Setter
@Schema(description = "统一退款响应数据")
public class UniRefundVo extends AllinPayVo {

    @Schema(title = "商户号")
    private String cusid;

    @Schema(title = "应用ID")
    private String appid;

    @Schema(title = "交易单号")
    private String trxid;

    @Schema(title = "商户订单号")
    private String reqsn;

    @Schema(title = "交易状态")
    private String trxstatus;

    @Schema(title = "交易完成时间")
    private String fintime;

    @Schema(title = "错误原因")
    private String errmsg;

    @Schema(title = "手续费")
    private Long fee;

    @Schema(title = "交易类型")
    private String trxcode;

    @Schema(title = "随机字符串")
    private String randomstr;

    @Schema(title = "渠道流水号")
    private String chnltrxid;

    @Schema(title = "渠道信息")
    private String chnldata;

    @Schema(title = "所属银行")
    private String bankcode;

    @Schema(title = "签名")
    private String sign;
}