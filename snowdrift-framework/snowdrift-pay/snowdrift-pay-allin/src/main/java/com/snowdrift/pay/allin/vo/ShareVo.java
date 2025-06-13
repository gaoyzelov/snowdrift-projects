package com.snowdrift.pay.allin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * ShareVo
 *
 * @author gaoye
 * @date 2025/05/23 13:10:31
 * @description xxxxxxxx
 * @since 1.0
 */
@Getter
@Setter
@Schema(description = "分账响应数据")
public class ShareVo extends AllinPayVo {

    @Schema(title = "商户号")
    private String cusid;

    @Schema(title = "应用ID")
    private String appid;

    @Schema(title = "出金交易单号")
    private String payertrxid;

    @Schema(title = "入金商户入金交易单号")
    private String payeetrxid;

    @Schema(title = "分账流水号")
    private String reqsn;

    /**
     * 交易的状态,
     *
     * 0000-交易成功
     * 2000，2008-需查询
     * 3开头错误码-交易失败，详情查看错误信息
     */
    @Schema(title = "交易状态")
    private String trxstatus;

    @Schema(title = "错误原因")
    private String errmsg;

    @Schema(title = "交易类型")
    private String trxcode;

    @Schema(title = "交易完成时间")
    private String fintime;

    @Schema(title = "随机字符串")
    private String randomstr;

    @Schema(title = "签名")
    private String sign;
}