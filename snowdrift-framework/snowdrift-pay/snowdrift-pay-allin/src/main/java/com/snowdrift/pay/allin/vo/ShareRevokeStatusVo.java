package com.snowdrift.pay.allin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * ShareRevokeStatusVo
 *
 * @author gaoye
 * @date 2025/05/23 13:40:35
 * @description xxxxxxxx
 * @since 1.0
 */
@Getter
@Setter
@Schema(description = "分账回退查询响应数据")
public class ShareRevokeStatusVo extends AllinPayVo {

    @Schema(title = "商户号")
    private String cusid;

    @Schema(title = "应用ID")
    private String appid;

    @Schema(title = "回退交易单号")
    private String rktrxid;

    @Schema(title = "分账回退流水号")
    private String reqsn;

    @Schema(title = "回退金额，单位:分")
    private Long trxamt;

    /**
     * 交易的状态,
     * 0000-交易成功
     * 2000，2008-需查询
     * 3开头错误码-交易失败，详情查看错误信息
     */
    @Schema(title = "交易状态")
    private String trxstatus;

    @Schema(title = "错误信息")
    private String errmsg;

    @Schema(title = "出金商户号")
    private String payercusid;

    @Schema(title = "交易类型")
    private String trxcode;

    @Schema(title = "交易完成时间")
    private String fintime;

    @Schema(title = "随机字符串")
    private String randomstr;

    @Schema(title = "签名")
    private String sign;
}