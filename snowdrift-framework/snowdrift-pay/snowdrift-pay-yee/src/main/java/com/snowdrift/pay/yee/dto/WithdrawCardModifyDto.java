package com.snowdrift.pay.yee.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * WithdrawCardModifyDto
 *
 * @author gaoye
 * @date 2025/06/06 15:24:06
 * @description xxxxxxxx
 * @since 1.0
 */
@Getter
@Setter
@Schema(title = "提现卡修改/注销参数")
public class WithdrawCardModifyDto extends RequestDto {

    @Schema(title = "操作类型,MODIFY-修改，CANCELLED-注销", required = true)
    private String bankCardOperateType;

    @Schema(title = "银行账号")
    private String accountNo;

    @Schema(title = "提现卡标识")
    private String bindId;

    @Schema(title = "银行编码")
    private String bankCode;

    @Schema(title = "银行支行编码")
    private String branchCode;
}