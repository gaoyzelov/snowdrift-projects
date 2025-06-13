package com.snowdrift.pay.allin.dto;

import com.snowdrift.core.utils.RandomUtil;
import com.snowdrift.pay.allin.utils.SybUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.Length;

/**
 * AggregatePayDto
 *
 * @author gaoye
 * @date 2025/05/21 09:51:46
 * @description xxxxxxxx
 * @since 1.0
 */
@Getter
@Setter
@Schema(description = "聚合码支付参数")
public class AggregateCodePayDto extends AllinPayDto {

    @Schema(title = "字符编码", required = true)
    @NotBlank(message = "字符编码不能为空")
    private String charset = "UTF-8";

    @Schema(title = "付款金额，单位为分", required = true)
    @NotNull(message = "付款金额不能为空")
    private Long trxamt;

    @Schema(title = "交易类型，分期付：F02")
    private String paytype;

    /**
     * 3  花呗分期3期
     * 6  花呗分期6期
     * 12 花呗分期12期
     * 24 花呗分期24期
     * <p>
     * 3-cc 支付宝信用卡分期3期
     * 6-cc 支付宝信用卡分期6期
     * 12-cc 支付宝信用卡分期12期
     * 24-cc 支付宝信用卡分期24期
     * <p>
     * 3,1 花呗分期3期(商户贴息)
     * 6,1   花呗分期6期(商户贴息)
     * 12,1  花呗分期12期(商户贴息)
     * 24,1 花呗分期24期(商户贴息)
     * <p>
     * 3-cc,1  支付宝信用卡分期3期(商户贴息)
     * 6-cc,1  支付宝信用卡分期6期(商户贴息)
     * 12-cc,1  支付宝信用卡分期12期(商户贴息)
     * 24-cc,1  支付宝信用卡分期24期(商户贴息)
     */
    @Schema(title = "分期期数，分期付必填")
    private String fqnum;

    @Schema(title = "唯一订单号，最大长度50", required = true)
    @NotNull(message = "唯一订单号不能为空")
    @Length(max = 50, message = "唯一订单号，最大长度50")
    private String reqsn;

    @Schema(title = "页面跳转同步通知页面路径，必须https，最大长度128", required = true)
    @NotNull(message = "页面跳转同步通知页面路径不能为空")
    @Length(max = 128, message = "页面跳转同步通知页面路径，最大长度128")
    @Pattern(regexp = "^https://.*$", message = "页面跳转同步通知页面路径必须HTTPS")
    private String returl;

    @Schema(title = "异步通知地址，最大长度256")
    @Length(max = 256, message = "异步通知地址，最大长度256")
    private String notify_url;

    @Schema(title = "订单描述信息，最大长度100", required = true)
    @NotBlank(message = "订单描述信息不能为空")
    @Length(max = 100, message = "订单描述信息，最大长度100")
    private String body;

    @Schema(title = "订单备注信息，最大长度300")
    @Length(max = 300, message = "订单备注信息，最大长度300")
    private String remark;

    @Schema(title = "随机字符串，最大长度32", required = true)
    @NotBlank(message = "随机字符串不能为空")
    @Length(max = 32, message = "随机字符串，最大长度32")
    private String randomstr = RandomUtil.randomNumbers(8);

    @Schema(title = "有效时间，默认5")
    private Integer validtime;

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

    @Schema(title = "门店号")
    private String subbranch;

    @Schema(title = "是否直接支付，0：否，1：是")
    private Integer ishide;

    @Schema(title = "取消支付是否通知,0-否，1-是")
    private Integer ispwdcancel;

    @Schema(title = "版本号，目前只支持12", required = true)
    @NotBlank(message = "版本号不能为空")
    private String version = "12";

    @Schema(title = "签名方式", required = true)
    @NotBlank(message = "签名方式不能为空")
    private String signtype = SybUtil.RSA;

    @Schema(title = "签名", required = true)
    @NotBlank(message = "签名不能为空")
    private String sign;

    @Schema(title = "银联pid")
    private String unpid;

    @Schema(title = "付款人真实姓名")
    private String truename;

    @Schema(title = "付款人证件号码")
    private String idno;

    @Schema(title = "扩展参数")
    private String extendparams;
}