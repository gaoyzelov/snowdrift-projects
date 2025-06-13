package com.snowdrift.pay.allin.vo;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * UniPayStatusVo
 *
 * @author gaoye
 * @date 2025/05/23 17:26:22
 * @description xxxxxxxx
 * @since 1.0
 */
@Getter
@Setter
@Schema(description = "统一查询返回参数")
public class UniPayStatusVo extends AllinPayVo {

    @Schema(title = "商户号")
    private String cusid;

    @Schema(title = "应用ID")
    private String appid;

    @Schema(title = "交易单号")
    private String trxid;

    @Schema(title = "支付渠道交易单号")
    private String chnltrxid;

    @Schema(title = "商户订单号")
    private String reqsn;

    @Schema(title = "交易类型")
    private String trxcode;

    @Schema(title = "交易金额，单位为分")
    private Long trxamt;

    @Schema(title = "交易状态，如果trxstatus为空,则交易正在处理中尚未完成")
    private String trxstatus;

    @Schema(title = "支付平台用户标识")
    private String acct;

    @Schema(title = "交易完成时间，格式为yyyyMMddHHmmss")
    private String fintime;

    @Schema(title = "随机字符串")
    private String randomstr;

    @Schema(title = "错误原因")
    private String errmsg;

    @Schema(title = "渠道子商户号")
    private String cmid;

    @Schema(title = "渠道号")
    private String chnlid;

    @Schema(title = "原交易金额")
    private Long initamt;

    @Schema(title = "手续费")
    private Long fee;

    @Schema(title = "渠道信息")
    private String chnldata;

    @Schema(title = "借贷标识")
    private String accttype;

    @Schema(title = "所属银行")
    private String bankcode;

    @Schema(title = "买家账号")
    private String logonid;

    @Schema(title = "分期数")
    private String fqnum;

    @Schema(title = "通联渠道侧OPENID")
    private String tlopenid;

    @Schema(title = "交易备注")
    private String trxreserve;

    @Schema(title = "结算周期")
    private String feecycle;

    @Schema(title = "签名")
    private String sign;
}