package com.snowdrift.pay.allin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * BalanceSettlementQueryVo
 *
 * @author gaoye
 * @date 2025/05/23 10:47:54
 * @description xxxxxxxx
 * @since 1.0
 */
@Getter
@Setter
@Schema(title = "商户账户余额结算查询结果")
public class BalanceSettlementQueryVo extends AllinPayVo{

    @Schema(title = "商户号")
    private String cusid;

    @Schema(title = "应用ID")
    private String appid;

    @Schema(title = "随机字符串")
    private String randomstr;

    @Schema(title = "交易类型,结算：300003")
    private String trxcode;

    @Schema(title = "渠道流水号")
    private String chnltrxid;

    @Schema(title = "发起结算金额，单位分")
    private Long initamt;

    @Schema(title = "实际结算到账金额，单位分")
    private Long trxamt;

    @Schema(title = "结算手续费，单位分")
    private Long fee;

    @Schema(title = "通联单号")
    private String trxid;

    @Schema(title = "请求流水号")
    private String reqsn;

    @Schema(title = "结算状态")
    private String trxstatus;

    @Schema(title = "结算完成时间")
    private String fintime;

    @Schema(title = "结算到账账号")
    private String acct;

    @Schema(title = "交易描述信息")
    private String errmsg;

    /**
     * 00-借记卡
     * 02-信用卡
     * 99-其他（花呗/余额等）
     */
    @Schema(title = "借贷标识")
    private String accttype;

    @Schema(title = "所属银行")
    private String bankcode;

    /**
     * 1-预收
     * 2-日结
     * 4-月结
     * 8-向持卡人收
     */
    @Schema(title = "结算周期")
    private String feecycle;

    @Schema(title = "签名")
    private String sign;
}