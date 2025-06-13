package com.snowdrift.pay.allin.dto.brand;


import com.snowdrift.pay.allin.dto.brand.bo.AgentReceiver;
import com.snowdrift.pay.allin.dto.brand.bo.PaySplit;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * AgentPayDto
 *
 * @author gaoye
 * @date 2025/06/10 13:22:43
 * @description xxxxxxxx
 * @since 1.0
 */
@Data
@Schema(title = "托管代付申请参数")
public class AgentPayDto implements Serializable {

    @Schema(title = "系统编号", required = true)
    @NotBlank(message = "系统编号不能为空")
    private String sysId;

    @Schema(title = "商户支付单号", required = true)
    @NotBlank(message = "商户支付单号不能为空")
    private String bizOrderNo;

    @Schema(title = "订单金额，单位：分", required = true)
    @NotNull(message = "订单金额不能为空")
    private Long amount;

    @Schema(title = "收款方,托收订单的收款方列表内的收款方编号bizUserId", required = true)
    @NotBlank(message = "收款方不能为空")
    private String receiver;

    @Schema(title = "异步通知地址")
    private String backUrl;

    @Schema(title = "备注")
    private String remark;

    @Schema(title = "源托收列表")
    private List<AgentReceiver> agentCollectList;

    @Schema(title = "分账信息")
    private PaySplit paySplit;
}