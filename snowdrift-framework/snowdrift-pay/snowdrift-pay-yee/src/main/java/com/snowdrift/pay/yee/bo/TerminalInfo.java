package com.snowdrift.pay.yee.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * TerminalInfo
 *
 * @author gaoye
 * @date 2025/06/06 10:19:14
 * @description xxxxxxxx
 * @since 1.0
 */
@Data
@Schema(title = "终端信息")
public class TerminalInfo implements Serializable {

    /**
     * YJF:易缴费
     * POS：POS机具
     * 台牌：台牌」
     * AUXILIARY：辅助终端
     */
    @Schema(title = "终端类型")
    private String terminalType;

    @Schema(title = "终端编号")
    private String terminalNo;

    @Schema(title = "终端名称")
    private String terminalName;

    @Schema(title = "网点编号")
    private String shopCustomerNumber;

    @Schema(title = "网点名称")
    private String shopName;

    @Schema(title = "机具序列号")
    private String serialNum;

    @Schema(title = "参考号")
    private String referenceId;
}