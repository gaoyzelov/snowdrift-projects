package com.snowdrift.pay.allin.dto;


import com.snowdrift.core.exception.BaseException;
import com.snowdrift.core.utils.RandomUtil;
import com.snowdrift.pay.allin.enums.PayTypeEnum;
import com.snowdrift.pay.allin.utils.SybUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;

/**
 * UniPayDto
 *
 * @author gaoye
 * @date 2025/05/23 14:41:04
 * @description xxxxxxxx
 * @since 1.0
 */
@Getter
@Setter
@Schema(description = "统一支付参数")
public class UniPayDto extends AllinPayDto {

    @Schema(title = "版本号，目前只支持11", required = true)
    @NotBlank(message = "版本号不能为空")
    private String version = "11";

    @Schema(title = "交易金额，单位为分", required = true)
    @NotBlank(message = "交易金额不能为空")
    private Long trxamt;

    @Schema(title = "商户交易单号", required = true)
    @NotBlank(message = "商户交易单号不能为空")
    private String reqsn;

    @Schema(title = "支付方式", required = true)
    @NotBlank(message = "支付方式不能为空")
    private String paytype;

    @Schema(title = "随机字符串", required = true)
    @NotBlank(message = "随机字符串不能为空")
    private String randomstr = RandomUtil.randomNumbers(8);

    @Schema(title = "订单标题", required = true)
    private String body;

    @Schema(title = "订单备注")
    private String remark;

    @Schema(title = "有效时间")
    private Integer validtime;

    @Schema(title = "支付平台用户标识，JS支付时使用")
    private String acct;

    @Schema(title = "交易结果通知地址")
    private String notify_url;

    @Schema(title = "支付限制，no_credit--指定不能使用信用卡支付")
    private String limit_pay;

    @Schema(title = "微信子appid，微信小程序/微信公众号/APP的appid")
    private String sub_appid;

    @Schema(title = "渠道门店编号")
    private String chnlstoreid;

    @Schema(title = "门店号")
    private String subbranch;

    @Schema(title = "扩展参数")
    private String extendparams;

    @Schema(title = "终端IP")
    private String cusip;

    @Schema(title = "支付完成跳转")
    private String front_url;

    @Schema(title = "证件号")
    private String idno;

    @Schema(title = "付款人真实姓名")
    private String truename;

    @Schema(title = "分账信息")
    private String asinfo;

    @Schema(title = "分期")
    private String fqnum;

    @Schema(title = "签名方式", required = true)
    @NotBlank(message = "签名方式不能为空")
    private String signtype = SybUtil.RSA;

    @Schema(title = "银联pid")
    private String unpid;

    @Schema(title = "金融机构号")
    private String finorg;

    @Schema(title = "签名", required = true)
    @NotBlank(message = "签名不能为空")
    private String sign;

    @Schema(title = "收银员号")
    private String operatorid;

    public void selfCheck(){
        PayTypeEnum payTypeEnum = PayTypeEnum.getByCode(paytype);
        if (Objects.isNull(payTypeEnum)){
            throw new BaseException("不支持的支付方式");
        }
        switch (payTypeEnum){
            case WX_SCAN:
            case ALI_SCAN:
            case DIGITAL_SCAN:
            case UNION_SCAN:
            case WX_APP:
            case ALI_APP:
            case DIGITAL_APP_H5:
                if (StringUtils.isBlank(notify_url)){
                    throw new BaseException("异步通知地址不能为空");
                }
                break;
            case WX_JS:
            case ALI_JS:
            case UNION_JS:
            case WX_MINI_PROGRAM:
            case WX_ORDER:
                if (StringUtils.isBlank(notify_url)){
                    throw new BaseException("异步通知地址不能为空");
                }
                if (StringUtils.isBlank(acct)){
                    throw new BaseException("支付平台用户标识不能为空");
                }
                if (StringUtils.isBlank(sub_appid)){
                    throw new BaseException("微信子appid不能为空");
                }
                break;
            default:
                break;

        }
    }
}