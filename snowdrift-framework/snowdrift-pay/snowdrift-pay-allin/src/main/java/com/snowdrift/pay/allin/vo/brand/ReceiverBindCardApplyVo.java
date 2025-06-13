package com.snowdrift.pay.allin.vo.brand;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * ReceiverBindCardApplyVo
 *
 * @author gaoye
 * @date 2025/06/10 10:09:14
 * @description xxxxxxxx
 * @since 1.0
 */
@Getter
@Setter
@Schema(title = "收款方银行卡验证返回结果")
public class ReceiverBindCardApplyVo extends AllinBrandVo{

    @Schema(title = "系统编号")
    private String sysId;

    @Schema(title = "收款方编号")
    private String bizUserId;

    @Schema(title = "交易流水号")
    private String tranceNum;

    @Schema(title = "申请时间")
    private String transDate;

    @Schema(title = "银行卡号")
    private String cardNo;

    @Schema(title = "银行卡类型,0-个人银行卡，1-企业对公账户")
    private Long cardType;
}