package com.snowdrift.pay.allin.vo.brand;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * WithdrawVo
 *
 * @author gaoye
 * @date 2025/05/29 14:54:58
 * @description xxxxxxxx
 * @since 1.0
 */
@Data
@Schema(title = "提现结果查询响应数据")
public class WithdrawVo implements Serializable {

    @Schema(title = "商户提现订单号")
    private String orderNo;

    @Schema(title = "提现银行卡号")
    private String bankCard;

    @Schema(title = "提现状态")
    private String status;

    @Schema(title = "提现状态描述")
    private String statusMsg;

    @Schema(title = "创建时间")
    private String createTime;
}