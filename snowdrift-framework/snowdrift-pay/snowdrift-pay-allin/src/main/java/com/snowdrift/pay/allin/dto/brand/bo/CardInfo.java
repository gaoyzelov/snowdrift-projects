package com.snowdrift.pay.allin.dto.brand.bo;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * CardInfo
 *
 * @author gaoye
 * @date 2025/06/10 10:56:47
 * @description xxxxxxxx
 * @since 1.0
 */
@Data
@Schema(title = "银行卡信息")
public class CardInfo implements Serializable {

    @Schema(title = "银行卡号")
    private String cardNo;

    @Schema(title = "银行名称")
    private String bankName;

    @Schema(title = "银行卡类型,1-借记卡")
    private Long cardType;

    @Schema(title = "银行卡/账户属性,0-个人银行卡，1-企业对公账户")
    private Long bankCardPro;

    @Schema(title = "开户行地区代码")
    private String bankCityNo;

    @Schema(title = "开户行支行名称")
    private String branchBankName;

    @Schema(title = "支付行号")
    private String unionBank;

    @Schema(title = "开户行所在省")
    private String province;

    @Schema(title = "开户行所在市")
    private String city;
}