package com.snowdrift.pay.allin.dto.brand;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.Range;

import java.io.Serializable;

/**
 * ReceiverCompanyInfoSetDto
 *
 * @author gaoye
 * @date 2025/06/03 16:49:36
 * @description xxxxxxxx
 * @since 1.0
 */
@Data
@Schema(title = "收款方公司信息设置参数")
public class ReceiverCompanyInfoSetDto implements Serializable {

    @Schema(title = "系统编号", required = true)
    @NotBlank(message = "系统编号不能为空")
    private String sysId;

    @Schema(title = "收款方编号", required = true)
    @NotBlank(message = "收款方编号不能为空")
    private String bizUserId;

    @Schema(title = "收款方公司名称,最大长度64", required = true)
    @NotBlank(message = "收款方公司名称不能为空")
    @Length(max = 64, message = "收款方公司名称,最大长度64")
    private String companyName;

    @Schema(title = "收款方公司地址,最大长度255")
    @Length(max = 255, message = "收款方公司地址,最大长度255")
    private String companyAddress;

    @Schema(title = "认证类型,1-三证，2-一证", required = true)
    @NotNull(message = "认证类型不能为空")
    @Range(min = 1, max = 2, message = "认证类型值有误，仅支持1或2")
    private Long authType;

    @Schema(title = "统一社会信用代码,认证类型为一证时必填，最大长度32")
    @Length(max = 32, message = "统一社会信用代码最大长度32")
    private String uniCredit;

    @Schema(title = "营业执照号,认证类型为三证时必填，最大长度32")
    @Length(max = 32, message = "营业执照号最大长度32")
    private String businessLicense;

    @Schema(title = "组织机构代码,认证类型为三证时必填，最大长度32")
    @Length(max = 32, message = "组织机构代码最大长度32")
    private String organizationCode;

    @Schema(title = "税务登记证,认证类型为三证时必填，最大长度32")
    @Length(max = 32, message = "税务登记证最大长度32")
    private String taxRegister;

    @Schema(title = "统一社会信用/营业执照号到期时间,yyyy-MM-dd")
    private String expLicense;

    @Schema(title = "联系电话，最大长度16")
    @Length(max = 16, message = "联系电话最大长度16")
    private String telephone;

    @Schema(title = "法人姓名,最大长度32", required = true)
    @NotBlank(message = "法人姓名不能为空")
    @Length(max = 32, message = "法人姓名最大长度32")
    private String legalName;

    /**
     * 法人证件类型
     * 1-身份证
     * 2-护照
     * 3-军官证
     * 4-回乡证
     * 5-台胞证
     * 6-警官证
     * 7-士兵证
     * 99-其他证件
     */
    @Schema(title = "法人证件类型", required = true)
    @NotNull(message = "法人证件类型不能为空")
    private Long identityType;

    @Schema(title = "法人证件号码,最大长度32", required = true)
    @NotBlank(message = "法人证件号码不能为空")
    @Length(max = 32, message = "法人证件号码最大长度32")
    private String legalIds;

    @Schema(title = "法人手机号码,最大长度16", required = true)
    @NotBlank(message = "法人手机号码不能为空")
    @Length(max = 16, message = "法人手机号码最大长度16")
    private String legalPhone;

    @Schema(title = "账户类型,0-对私，1-对公")
    private Long acctType;

    @Schema(title = "账户号,最大长度32",required = true)
    @NotBlank(message = "账户号不能为空")
    private String accountNo;

    @Schema(title = "开户行名称,最大长度32")
    @Length(max = 32, message = "开户行名称最大长度32")
    private String parentBankName;

    @Schema(title = "开户行地区代码,最大长度8")
    @Length(max = 8, message = "开户行地区代码最大长度8")
    private String bankCityNo;

    @Schema(title = "开户行支行名称,最大长度32")
    @Length(max = 32, message = "开户行支行名称最大长度32")
    private String bankName;

    @Schema(title = "支付行号,最大长度12")
    @Length(max = 12, message = "支付行号最大长度12")
    private String unionBank;

    @Schema(title = "开户行所在省,最大长度8")
    @Length(max = 8, message = "开户行所在省最大长度8")
    private String province;

    @Schema(title = "开户行所在市,最大长度8")
    @Length(max = 8, message = "开户行所在市最大长度8")
    private String city;

    @Schema(title = "公司性质,1-企业，2-个体工商户，3-事业单位")
    private String comproperty;

    @Schema(title = "银行预留手机,最大长度16")
    @Length(max = 16, message = "银行预留手机最大长度16")
    private String phone;
}