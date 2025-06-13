package com.snowdrift.pay.allin.vo.brand;

import com.alibaba.fastjson2.JSON;
import com.snowdrift.pay.allin.dto.brand.bo.CardInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

/**
 * ReceiverBindCardVo
 *
 * @author gaoye
 * @date 2025/06/10 10:53:06
 * @description xxxxxxxx
 * @since 1.0
 */
@Getter
@Setter
@Schema(title = "收款方绑定银行卡查询返回结果")
public class ReceiverBindCardVo extends AllinBrandVo {

    @Schema(title = "系统编号")
    private String sysId;

    @Schema(title = "收款方编号")
    private String bizUserId;

    @Schema(title = "已绑定银行卡信息列表,JSONArray字符串")
    private String bindCardList;

    public List<CardInfo> getBindCards() {
        if (StringUtils.isBlank(bindCardList) || StringUtils.equals(bindCardList, "[]")) {
            return null;
        }
        try {
            return JSON.parseArray(bindCardList, CardInfo.class);
        } catch (Exception e) {
            return null;
        }
    }
}