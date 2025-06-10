package com.snowdrift.pay.yee.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * OrderNotifyDto
 *
 * @author gaoye
 * @date 2025/06/06 10:24:22
 * @description xxxxxxxx
 * @since 1.0
 */
@Data
@Schema(title = "订单异步通知参数")
public class OrderNotifyDto implements Serializable {

    @Schema(title = "应用标识(appKey)")
    private String customerIdentification;

    @Schema(title = "加密签名后的业务数据")
    private String response;
}