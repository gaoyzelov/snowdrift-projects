package com.snowdrift.pay.allin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * SettlementDocumentVo
 *
 * @author gaoye
 * @date 2025/05/23 11:06:27
 * @description xxxxxxxx
 * @since 1.0
 */
@Getter
@Setter
@Schema(title = "获取结算单结果")
public class SettlementDocumentVo extends AllinPayVo {

    @Schema(title = "商户号")
    private String cusid;

    @Schema(title = "应用ID")
    private String appid;

    @Schema(title = "结算单列表")
    private List<SettInfoVo> trxlist;

    @Schema(title = "随机字符串")
    private String randomstr;

    @Schema(title = "签名")
    private String sign;
}