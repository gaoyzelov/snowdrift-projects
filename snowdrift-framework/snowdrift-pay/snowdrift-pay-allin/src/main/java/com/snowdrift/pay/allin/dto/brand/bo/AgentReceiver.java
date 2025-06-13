package com.snowdrift.pay.allin.dto.brand.bo;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * Receiver
 *
 * @author gaoye
 * @date 2025/06/10 13:36:05
 * @description xxxxxxxx
 * @since 1.0
 */
@Data
@Schema(title = "委托收款方")
public class AgentReceiver implements Serializable {

    @Schema(title = "源托收订单号")
    private String bizOrderNo;

    @Schema(title = "金额")
    private Long amount;
}