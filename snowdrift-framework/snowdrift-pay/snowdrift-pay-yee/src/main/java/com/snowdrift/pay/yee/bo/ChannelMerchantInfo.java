package com.snowdrift.pay.yee.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * ChannelMerchantInfo
 *
 * @author gaoye
 * @date 2025/06/06 11:47:47
 * @description xxxxxxxx
 * @since 1.0
 */
@Data
@Schema(title = "渠道商户号信息")
public class ChannelMerchantInfo implements Serializable {

    @Schema(title = "聚合商户号")
    private String channelMerchantNo;
}