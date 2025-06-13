package com.snowdrift.pay.allin.vo.brand;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * WithdrawApplyVo
 *
 * @author gaoye
 * @date 2025/05/29 13:41:07
 * @description xxxxxxxx
 * @since 1.0
 */
@Getter
@Setter
@Schema(title = "提现申请接口返回结果")
public class WithdrawApplyVo extends AllinBrandVo {

    @Schema(title = "系统编号")
    private String sysId;

    @Schema(title = "会员编号")
    private String bizUserId;

    @Schema(title = "系统提现订单号")
    private String orderNo;

    @Schema(title = "商户提现订单号")
    private String bizOrderNo;

    /**
     * 支付状态
     * 00 交易成功
     * 10 待处理
     * 20 交易处理中
     * 99 交易失败
     */
    @Schema(title = "状态")
    private String payStatus;

    @Schema(title = "状态描述")
    private String payStatusMsg;
}