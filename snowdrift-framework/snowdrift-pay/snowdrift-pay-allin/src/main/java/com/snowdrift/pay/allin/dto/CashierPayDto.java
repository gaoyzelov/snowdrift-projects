package com.snowdrift.pay.allin.dto;


import com.snowdrift.core.utils.RandomUtil;
import com.snowdrift.pay.allin.utils.SybUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.io.Serializable;

/**
 * WxPayDto
 *
 * @author gaoye
 * @date 2025/05/22 15:29:29
 * @description xxxxxxxx
 * @since 1.0
 */
@Data
@Schema(description = "小程序收银台参数")
public class CashierPayDto implements Serializable {

    @Schema(title = "付款金额，单位为分", required = true)
    @NotNull(message = "付款金额不能为空")
    private Long trxamt;

    @Schema(title = "商户订单号", required = true)
    @NotBlank(message = "商户订单号不能为空")
    private String reqsn;

    @Schema(title = "服务器异步通知页面路径", required = true)
    @NotBlank(message = "服务器异步通知页面路径不能为空")
    private String notify_url;

    @Schema(title = "订单标题", required = true)
    @NotBlank(message = "订单标题不能为空")
    private String body;

    @Schema(title = "订单备注")
    private String remark;

    @Schema(title = "validtime")
    private String validtime;

    @Schema(title = "支付方式，微信：W06，支付宝：A02", required = true)
    @NotBlank(message = "支付方式不能为空")
    @Pattern(regexp = "^(W06|A02)$", message = "支付方式只能是W06或A02")
    private String paytype;

    @Schema(title = "支付限制")
    private String limit_pay;

    /**
     * 格式:cusid:type:amount;cusid:type:amount
     * <p>
     * cusid:接收分账的通联商户号
     * type分账类型（01：按金额  02：按比率）
     * amount:
     * 分账类型为01表示分账金额，单位元
     * 分账类型为02表示分账比例，范围0~1
     */
    @Schema(title = "分账信息")
    private String asinfo;

    @Schema(title = "随机字符串", required = true)
    @NotBlank(message = "随机字符串不能为空")
    private String randomstr = RandomUtil.randomNumbers(8);

    @Schema(title = "门店号")
    private String subbranch;

    @Schema(title = "签名类型", required = true)
    @NotBlank(message = "签名类型不能为空")
    private String signtype = SybUtil.RSA;

    @Schema(title = "是否直接支付，直接支付 0或空时不变")
    private Integer isdirectpay;

    @Schema(title = "是否禁止分享，1禁止分享 0或空时支持分享")
    private Integer ishideshare;

    @Schema(title = "版本号，目前只支持12", required = true)
    @NotBlank(message = "版本号不能为空")
    private String version = "12";

    @Schema(title = "签名", required = true)
    @NotBlank(message = "签名不能为空")
    private String sign;

    @Schema(title = "付款人真实姓名")
    private String truename;

    @Schema(title = "证件号码")
    private String idno;
}