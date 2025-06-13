package com.snowdrift.pay.allin.vo.brand;

import com.alibaba.fastjson2.JSONObject;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * ReceiverVo
 *
 * @author gaoye
 * @date 2025/06/03 17:23:50
 * @description xxxxxxxx
 * @since 1.0
 */
@Getter
@Setter
@Schema(title = "收款方信息返回结果")
public class ReceiverVo extends AllinBrandVo{

    @Schema(title = "系统编号")
    private String sysId;

    @Schema(title = "收款方编号")
    private String bizUserId;

    @Schema(title = "收款方类型,2-企业会员，3-个人会员")
    private Long memberType;

    @Schema(title = "绑定的手机号")
    private String phone;

    @Schema(title = "是否电子签约")
    private Boolean isSignContract;

    @Schema(title = "收款方状态,1-待审核，2-审核成功，3-审核失败")
    private Long memberStatus;

    @Schema(title = "收款方角色,00-门店，02-经销商，03-联营商，04-代理商，99-其他")
    private String memberRole;

    @Schema(title = "收款方信息")
    private JSONObject memberInfo;

    @Schema(title = "业务名称")
    private String businessName;
}