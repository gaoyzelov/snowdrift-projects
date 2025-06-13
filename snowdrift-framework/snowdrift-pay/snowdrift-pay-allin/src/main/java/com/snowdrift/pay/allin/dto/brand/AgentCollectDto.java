package com.snowdrift.pay.allin.dto.brand;

import com.snowdrift.pay.allin.dto.brand.bo.OrderDetail;
import com.snowdrift.pay.allin.dto.brand.bo.Receiver;
import com.snowdrift.pay.allin.dto.brand.bo.TermInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * AgentCollectDto
 *
 * @author gaoye
 * @date 2025/06/10 13:22:32
 * @description xxxxxxxx
 * @since 1.0
 */
@Data
@Schema(title = "托管代收申请参数")
public class AgentCollectDto implements Serializable {

    @Schema(title = "系统编号", required = true)
    @NotBlank(message = "系统编号不能为空")
    private String sysId;

    @Schema(title = "商户支付单号", required = true)
    @NotBlank(message = "商户支付单号不能为空")
    private String bizOrderNo;

    @Schema(title = "订单金额", required = true)
    @NotNull(message = "订单金额不能为空")
    private Long amount;

    @Schema(title = "支付方式", required = true)
    @NotBlank(message = "支付方式不能为空")
    private String payMethod;

    @Schema(title = "外部账号,微信openid、支付宝userId")
    private String account;

    @Schema(title = "外部appid，微信公众号/小程序appid")
    private String subAppId;

    @Schema(title = "授权码，仅支付方式：CODEPAY_VSP，必填")
    private String authCode;

    @Schema(title = "异步通知地址")
    private String backUrl;

    @Schema(title = "前端跳转地址，H5收银台支付方式必填")
    private String frontUrl;

    @Schema(title = "交易发起IP",required = true)
    @NotBlank(message = "交易发起IP不能为空")
    private String consumerIp;

    @Schema(title = "订单有效时间,yyyy-MM-dd HH:mm:ss")
    private String orderExpireDatetime;

    @Schema(title = "备注")
    private String remark;

    @Schema(title = "商品名称")
    private String goodsName;

    @Schema(title = "交易类型",required = true)
    @NotBlank(message = "交易类型不能为空")
    private String tradeType;

    @Schema(title = "子商户号")
    private String vspCusId;

    @Schema(title = "订单详情",required = true)
    @NotEmpty(message = "订单详情不能为空")
    private List<OrderDetail> orderDetails;

    @Schema(title = "门店编号")
    private String storeNo;

    @Schema(title = "终端信息,支付方式为CODEPAY_VSP必填")
    private TermInfo termInfo;

    @Schema(title = "贷记卡标识")
    private String limitPay;

    @Schema(title = "渠道拓展参数")
    private String extendParams;

    @Schema(title = "订单支付标识")
    private String goodsTag;

    @Schema(title = "优惠信息")
    private String benefitDetail;

    @Schema(title = "渠道门店号")
    private String chnlStoreId;

    @Schema(title = "门店号")
    private String subBranch;

    @Schema(title = "付款方编号")
    private String payer;

    @Schema(title = "收款方列表",required = true)
    @NotEmpty(message = "收款方列表不能为空")
    private List<Receiver> receivers;

    @Schema(title = "微信应用名称，支付方式为【收银宝云微支付】时必传。填小程序ID/公众号ID")
    private String appName;

    @Schema(title = "微信应用类型，支付方式为【收银宝云微支付】时必传。03-小程序，04-公众号")
    private String appType;
}