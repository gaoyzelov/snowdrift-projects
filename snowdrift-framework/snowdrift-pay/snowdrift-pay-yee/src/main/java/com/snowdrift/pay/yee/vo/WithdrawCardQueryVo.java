package com.snowdrift.pay.yee.vo;

import com.snowdrift.pay.yee.bo.BankCardAccount;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * WithdrayCardQueryVo
 *
 * @author gaoye
 * @date 2025/06/06 15:07:11
 * @description xxxxxxxx
 * @since 1.0
 */
@Getter
@Setter
@Schema(title = "提现卡查询返回参数")
public class WithdrawCardQueryVo extends ResponseVo {

    @Schema(title = "商户编号")
    private String merchantNo;

    @Schema(title = "提现卡列表")
    private List<BankCardAccount> bankCardAccountList;
}