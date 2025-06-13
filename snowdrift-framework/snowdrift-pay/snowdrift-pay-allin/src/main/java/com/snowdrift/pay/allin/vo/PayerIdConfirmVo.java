package com.snowdrift.pay.allin.vo;

import com.snowdrift.pay.allin.vo.brand.AllinBrandVo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * PayerIdConfirmVo
 *
 * @author gaoye
 * @date 2025/06/10 11:32:19
 * @description xxxxxxxx
 * @since 1.0
 */
@Getter
@Setter
@Schema(title = "确认验证付款方ID返回结果")
public class PayerIdConfirmVo extends AllinBrandVo {

    @Schema(title = "系统编号")
    private String sysId;

    @Schema(title = "付款方ID")
    private String bizUserId;
}