package com.snowdrift.pay.allin.service;


import com.snowdrift.pay.allin.dto.brand.*;
import com.snowdrift.pay.allin.dto.brand.bo.TrxQueryDto;
import com.snowdrift.pay.allin.vo.AsyncNotifyVo;
import com.snowdrift.pay.allin.vo.PayerIdConfirmVo;
import com.snowdrift.pay.allin.vo.brand.*;

/**
 * IAllinBrandService
 *
 * @author gaoye
 * @date 2025/05/29 13:11:28
 * @description xxxxxxxx
 * @since 1.0
 */
public interface IAllinBrandService {

    ReceiverCreateVo createReceiver(ReceiverCreateDto receiverCreateDto);

    ReceiverRealNameVo setReceiverRealName(ReceiverRealNameDto receiverRealNameDto);

    ReceiverCompanyInfoSetVo setReceiverCompanyInfo(ReceiverCompanyInfoSetDto receiverCompanyInfoSetDto);

    ReceiverBindPhoneApplyVo receiverBindPhoneApply(ReceiverBindPhoneApplyDto receiverBindPhoneApplyDto);

    ReceiverBindPhoneConfirmVo receiverBindPhoneConfirm(ReceiverBindPhoneConfirmDto receiverBindPhoneConfirmDto);

    ReceiverBindCardApplyVo receiverBindCardApply(ReceiverBindCardApplyDto receiverBindCardApplyDto);

    ReceiverUnbindCardVo receiverUnbindCard(ReceiverUnbindCardDto receiverUnbindCardDto);

    ReceiverSignContractVo receiverSignContract(ReceiverSignContractDto receiverSignContractDto);

    ReceiverVo queryReceiver(ReceiverDto receiverDto);

    ReceiverBindCardVo queryReceiverBindCard(ReceiverBindCardDto receiverBindCardDto);

    ReceiverBalanceVo queryReceiverBalance(ReceiverBalanceDto receiverBalanceDto);

    MerchantBalanceVo queryMerchantBalance(MerchantBalanceDto merchantBalanceDto);

    PayerIdVo queryPayerId(PayerIdDto payerIdDto);

    PayerIdConfirmVo payerIdConfirm(PayerIdConfirmDto payerIdConfirmDto);

    PayApplyVo payApply(PayApplyDto payApplyDto);

    PayRefundVo payRefund(PayRefundDto payRefundDto);

    TrxQueryVo trxQuery(TrxQueryDto trxQueryDto);

    TrxConfirmVo trxConfirm(TrxConfirmDto trxConfirmDto);

    AsyncNotifyVo handleAsyncNotify(AllinBrandDto<String> allinBrandDto);

    WithdrawApplyVo withdrawApply(WithdrawApplyDto withdrawApplyDto);

    WithdrawRefundNotifyVo handleWithdrawRefundNotify(AllinBrandDto<String> request);

    TransferApplyVo transferApply(TransferApplyDto transferApplyDto);

    TransferQueryVo transferQuery(TransferQueryDto transferQueryDto);

    FreezeMoneyVo freezeMoney(FreezeMoneyDto freezeMoneyDto);

    UnfreezeMoneyVo unfreezeMoney(UnfreezeMoneyDto unfreezeMoneyDto);

    AgentCollectVo agentCollect(AgentCollectDto agentCollectDto);

    AgentPayVo agentPay(AgentPayDto agentPayDto);

    DocumentSyncVo documentSync(DocumentSyncDto documentSyncDto);

    DocumentTypeVo queryDocumentType(DocumentTypeDto documentTypeDto);

    <T, R extends AllinBrandVo> R doExecute(AllinBrandDto<T> request, Class<R> clazz, int timeout);
}