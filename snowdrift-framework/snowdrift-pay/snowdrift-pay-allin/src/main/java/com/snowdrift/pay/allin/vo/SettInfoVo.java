package com.snowdrift.pay.allin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * TrxInfoVo
 *
 * @author gaoye
 * @date 2025/05/23 11:08:40
 * @description xxxxxxxx
 * @since 1.0
 */
@Data
@Schema(title = "结算单信息")
public class SettInfoVo implements Serializable {

    @Schema(title = "结算单生成时间,yyyyMMddHHmmss")
    private String clearsplittime;

    @Schema(title = "应结算日期,yyyyMMddHHmm")
    private String expectclearday;

    @Schema(title = "结算单状态")
    private String iscleared;

    @Schema(title = "交易手续费")
    private Long fee;

    @Schema(title = "结算金额")
    private Long clearamt;

    @Schema(title = "结算手续费")
    private Long settfee;

    @Schema(title = "结算账户名称")
    private String acctname;

    @Schema(title = "结算银行名称")
    private String bankname;

    @Schema(title = "结算账户")
    private String acctno;

    @Schema(title = "结算摘要")
    private String summary;
}