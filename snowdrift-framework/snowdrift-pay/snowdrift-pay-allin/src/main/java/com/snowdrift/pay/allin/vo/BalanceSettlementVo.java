package com.snowdrift.pay.allin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * BalanceSettlementVo
 *
 * @author gaoye
 * @date 2025/05/23 10:25:38
 * @description xxxxxxxx
 * @since 1.0
 */
@Getter
@Setter
@Schema(title = "商户账户余额结算结果")
public class BalanceSettlementVo extends AllinPayVo {

    @Schema(title = "商户号")
    private String cusid;

    @Schema(title = "应用ID")
    private String appid;

    @Schema(title = "随机字符串")
    private String randomstr;

    @Schema(title = "实际结算到账金额，单位分")
    private Long trxamt;

    @Schema(title = "结算手续费，单位分")
    private Long fee;

    @Schema(title = "通联单号")
    private String trxid;

    @Schema(title = "请求流水号")
    private String reqsn;

    /**
     * 0000/4000为成功
     * 2000为处理中
     */
    @Schema(title = "结算状态")
    private String trxstatus;

    @Schema(title = "交易描述信息")
    private String errmsg;

    @Schema(title = "签名")
    private String sign;
}