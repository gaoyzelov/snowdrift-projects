package com.snowdrift.pay.allin.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.snowdrift.core.exception.BaseException;
import com.snowdrift.core.utils.HttpUtil;
import com.snowdrift.pay.allin.dto.brand.*;
import com.snowdrift.pay.allin.dto.brand.bo.TrxQueryDto;
import com.snowdrift.pay.allin.enums.brand.BrandServiceEnum;
import com.snowdrift.pay.allin.properties.AllinBrandProperties;
import com.snowdrift.pay.allin.service.IAllinBrandService;
import com.snowdrift.pay.allin.utils.BrandUtil;
import com.snowdrift.pay.allin.vo.AsyncNotifyVo;
import com.snowdrift.pay.allin.vo.PayerIdConfirmVo;
import com.snowdrift.pay.allin.vo.brand.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * IAllinBrandServiceImpl
 *
 * @author gaoye
 * @date 2025/05/29 13:11:45
 * @description xxxxxxxx
 * @since 1.0
 */
@Slf4j
public class AllinBrandServiceImpl implements IAllinBrandService {

    private AllinBrandProperties prop;

    public AllinBrandServiceImpl(AllinBrandProperties prop) {
        this.prop = prop;
    }

    /**
     * 创建收款方
     */
    @Override
    public ReceiverCreateVo createReceiver(ReceiverCreateDto receiverCreateDto) {
        return doExecute(BrandServiceEnum.MEMBER_REGISTER, receiverCreateDto, ReceiverCreateVo.class);
    }

    /**
     * 收款方实名认证
     */
    @Override
    public ReceiverRealNameVo setReceiverRealName(ReceiverRealNameDto receiverRealNameDto) {
        return doExecute(BrandServiceEnum.MEMBER_REAL_NAME, receiverRealNameDto, ReceiverRealNameVo.class);
    }

    /**
     * 收款方企业信息设置
     */
    @Override
    public ReceiverCompanyInfoSetVo setReceiverCompanyInfo(ReceiverCompanyInfoSetDto receiverCompanyInfoSetDto) {
        return doExecute(BrandServiceEnum.MEMBER_SET_COMPANY_INFO, receiverCompanyInfoSetDto, ReceiverCompanyInfoSetVo.class);
    }

    /**
     * 收款方短信认证触发
     */
    @Override
    public ReceiverBindPhoneApplyVo receiverBindPhoneApply(ReceiverBindPhoneApplyDto receiverBindPhoneApplyDto) {
        return doExecute(BrandServiceEnum.MEMBER_BIND_PHONE_APPLY, receiverBindPhoneApplyDto, ReceiverBindPhoneApplyVo.class);
    }

    /**
     * 收款方短信认证回填
     */
    @Override
    public ReceiverBindPhoneConfirmVo receiverBindPhoneConfirm(ReceiverBindPhoneConfirmDto receiverBindPhoneConfirmDto) {
        return doExecute(BrandServiceEnum.MEMBER_BIND_PHONE_CONFIRM, receiverBindPhoneConfirmDto, ReceiverBindPhoneConfirmVo.class);
    }

    /**
     * 收款方银行卡验证
     */
    @Override
    public ReceiverBindCardApplyVo receiverBindCardApply(ReceiverBindCardApplyDto receiverBindCardApplyDto) {
        return doExecute(BrandServiceEnum.MEMBER_BIND_CARD_APPLY, receiverBindCardApplyDto, ReceiverBindCardApplyVo.class);
    }

    /**
     * 收款方银行卡解绑
     */
    @Override
    public ReceiverUnbindCardVo receiverUnbindCard(ReceiverUnbindCardDto receiverUnbindCardDto) {
        return doExecute(BrandServiceEnum.MEMBER_UNBIND_CARD, receiverUnbindCardDto, ReceiverUnbindCardVo.class);
    }

    /**
     * 收款方电子协议签约
     */
    @Override
    public ReceiverSignContractVo receiverSignContract(ReceiverSignContractDto receiverSignContractDto) {
        return doExecute(BrandServiceEnum.MEMBER_SIGN_CONTRACT, receiverSignContractDto, ReceiverSignContractVo.class);
    }

    /**
     * 查询收款方信息
     */
    @Override
    public ReceiverVo queryReceiver(ReceiverDto receiverDto) {
        return doExecute(BrandServiceEnum.MEMBER_QUERY, receiverDto, ReceiverVo.class);
    }

    /**
     * 收款方绑定银行卡查询
     */
    @Override
    public ReceiverBindCardVo queryReceiverBindCard(ReceiverBindCardDto receiverBindCardDto) {
        return doExecute(BrandServiceEnum.MEMBER_BIND_CARD_QUERY, receiverBindCardDto, ReceiverBindCardVo.class);
    }

    /**
     * 查询收款方余额
     */
    @Override
    public ReceiverBalanceVo queryReceiverBalance(ReceiverBalanceDto receiverBalanceDto) {
        return doExecute(BrandServiceEnum.MEMBER_BALANCE, receiverBalanceDto, ReceiverBalanceVo.class);
    }

    /**
     * 平台账户余额查询
     */
    @Override
    public MerchantBalanceVo queryMerchantBalance(MerchantBalanceDto merchantBalanceDto) {
        return doExecute(BrandServiceEnum.MERCHANT_BALANCE, merchantBalanceDto, MerchantBalanceVo.class);
    }

    /**
     * 查询付款方id
     */
    @Override
    public PayerIdVo queryPayerId(PayerIdDto payerIdDto) {
        return doExecute(BrandServiceEnum.QUERY_PAYER_ID, payerIdDto, PayerIdVo.class);
    }

    /**
     * 确认验证付款方ID
     */
    @Override
    public PayerIdConfirmVo payerIdConfirm(PayerIdConfirmDto payerIdConfirmDto) {
        return doExecute(BrandServiceEnum.PAYER_ID_CONFIRM, payerIdConfirmDto, PayerIdConfirmVo.class);
    }

    /**
     * 支付申请
     */
    @Override
    public PayApplyVo payApply(PayApplyDto payApplyDto) {
        return doExecute(BrandServiceEnum.PAY_APPLY, payApplyDto, PayApplyVo.class);
    }

    /**
     * 支付退款
     */
    @Override
    public PayRefundVo payRefund(PayRefundDto payRefundDto) {
        return doExecute(BrandServiceEnum.PAY_REFUND, payRefundDto, PayRefundVo.class);
    }

    /**
     * 交易结果查询
     */
    @Override
    public TrxQueryVo trxQuery(TrxQueryDto trxQueryDto) {
        return doExecute(BrandServiceEnum.TRX_QUERY, trxQueryDto, TrxQueryVo.class);
    }

    /**
     * 交易确认
     */
    @Override
    public TrxConfirmVo trxConfirm(TrxConfirmDto trxConfirmDto) {
        return doExecute(BrandServiceEnum.TRX_CONFIRM, trxConfirmDto, TrxConfirmVo.class);
    }

    /**
     * 异步通知处理
     */
    @Override
    public AsyncNotifyVo handleAsyncNotify(AllinBrandDto<String> allinBrandDto) {
        return handleNotify(allinBrandDto, AsyncNotifyVo.class);
    }

    /**
     * 提现申请
     */
    @Override
    public WithdrawApplyVo withdrawApply(WithdrawApplyDto withdrawApplyDto) {
        return doExecute(BrandServiceEnum.WITHDRAW_APPLY, withdrawApplyDto, WithdrawApplyVo.class);
    }

    /**
     * 提现退票订单通知
     */
    @Override
    public WithdrawRefundNotifyVo handleWithdrawRefundNotify(AllinBrandDto<String> allinBrandDto) {
        return handleNotify(allinBrandDto, WithdrawRefundNotifyVo.class);
    }

    /**
     * 转账汇款申请
     */
    @Override
    public TransferApplyVo transferApply(TransferApplyDto transferApplyDto) {
        return doExecute(BrandServiceEnum.TRANSFER_APPLY, transferApplyDto, TransferApplyVo.class);
    }

    /**
     * 转账汇款交易结果查询
     */
    @Override
    public TransferQueryVo transferQuery(TransferQueryDto transferQueryDto) {
        return doExecute(BrandServiceEnum.TRANSFER_QUERY, transferQueryDto, TransferQueryVo.class);
    }

    /**
     * 冻结金额
     */
    @Override
    public FreezeMoneyVo freezeMoney(FreezeMoneyDto freezeMoneyDto) {
        return doExecute(BrandServiceEnum.FREEZE_MONEY, freezeMoneyDto, FreezeMoneyVo.class);
    }

    /**
     * 解冻金额
     */
    @Override
    public UnfreezeMoneyVo unfreezeMoney(UnfreezeMoneyDto unfreezeMoneyDto) {
        return doExecute(BrandServiceEnum.UNFREEZE_MONEY, unfreezeMoneyDto, UnfreezeMoneyVo.class);
    }

    /**
     * 托管代收申请
     */
    @Override
    public AgentCollectVo agentCollect(AgentCollectDto agentCollectDto) {
        return doExecute(BrandServiceEnum.AGENT_COLLECT_APPLY, agentCollectDto, AgentCollectVo.class);
    }

    /**
     * 托管代付申请
     */
    @Override
    public AgentPayVo agentPay(AgentPayDto agentPayDto) {
        return doExecute(BrandServiceEnum.AGENT_PAY_APPLY, agentPayDto, AgentPayVo.class);
    }

    /**
     * 单据同步
     */
    @Override
    public DocumentSyncVo documentSync(DocumentSyncDto documentSyncDto) {
        return doExecute(BrandServiceEnum.DOCUMENT_SYNC, documentSyncDto, DocumentSyncVo.class);
    }

    /**
     * 单据类型查询
     */
    @Override
    public DocumentTypeVo queryDocumentType(DocumentTypeDto documentTypeDto) {
        return doExecute(BrandServiceEnum.DOCUMENT_TYPE_QUERY, documentTypeDto, DocumentTypeVo.class);
    }

    /**
     * 初始化参数
     */
    public <T, R extends AllinBrandVo> R doExecute(BrandServiceEnum service, T t, Class<R> clazz) {
        AllinBrandDto<T> dto = new AllinBrandDto<>();
        dto.setAppId(prop.getAppId());
        dto.setVersion(prop.getVersion());
        dto.setService(service.getCode());
        dto.setBizContent(t);
        dto.sign(prop.getAppKey());
        return doExecute(dto, clazz, 5000);
    }

    /**
     * 智品牌接口请求
     *
     * @param request 请求参数
     * @param clazz   返回结果类型
     * @param timeout 超时时间
     * @return 返回结果
     */
    @Override
    public <T, R extends AllinBrandVo> R doExecute(AllinBrandDto<T> request, Class<R> clazz, int timeout) {
        String url = Boolean.TRUE.equals(prop.getTestMode()) ? prop.getTestUrl() : prop.getProdUrl();
        String response = HttpUtil.post(url, request.toMap(), timeout);
        return verifyResult(response, clazz);
    }

    /**
     * 校验返回结果
     *
     * @param result 返回结果
     * @param clazz  返回结果类型
     */
    public <T> T verifyResult(String result, Class<T> clazz) {
        JSONObject jsonObject;
        try {
            jsonObject = JSON.parseObject(result);
            log.info("请求返回结果：{}", jsonObject);
        } catch (Exception e) {
            throw new BaseException("返回数据错误");
        }
        TreeMap<String, String> resultParam = new TreeMap<>();
        for (Map.Entry<String, Object> each : jsonObject.entrySet()) {
            resultParam.put(each.getKey(), Objects.nonNull(each.getValue()) ? String.valueOf(each.getValue()) : StringUtils.EMPTY);
        }
        resultParam.put("key", prop.getAppKey());
        if (!BrandUtil.verify(resultParam)) {
            throw new BaseException("验签不通过");
        }
        try {
            return JSON.parseObject(result, clazz);
        } catch (Exception e) {
            log.info("数据解析异常:{}", e.getLocalizedMessage());
            throw new BaseException("数据解析异常");
        }
    }

    /**
     * 处理异步通知
     *
     * @param allinBrandDto 通知数据
     * @return 通知结果
     */
    public <T> T handleNotify(AllinBrandDto<String> allinBrandDto, Class<T> clazz) {
        TreeMap<String, String> param = new TreeMap<>();
        param.put("appId", allinBrandDto.getAppId());
        param.put("version", allinBrandDto.getVersion());
        param.put("charset", allinBrandDto.getCharset());
        param.put("signType", allinBrandDto.getSignType());
        param.put("timestamp", allinBrandDto.getTimestamp());
        param.put("service", allinBrandDto.getService());
        param.put("bizContent", allinBrandDto.getBizContent());
        param.put("key", prop.getAppKey());
        String sign = BrandUtil.sign(param);
        if (!StringUtils.equals(sign, allinBrandDto.getSign())) {
            throw new BaseException("验签不通过");
        }
        try {
            return JSON.parseObject(allinBrandDto.getBizContent(), clazz);
        } catch (Exception e) {
            log.info("数据解析异常:{}", e.getLocalizedMessage());
            throw new BaseException("数据解析异常");
        }
    }
}