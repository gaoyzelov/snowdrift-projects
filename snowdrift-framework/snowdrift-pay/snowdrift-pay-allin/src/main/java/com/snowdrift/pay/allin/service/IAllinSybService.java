package com.snowdrift.pay.allin.service;


import com.snowdrift.pay.allin.dto.*;
import com.snowdrift.pay.allin.vo.*;
import jakarta.servlet.http.HttpServletRequest;
import java.util.TreeMap;

/**
 * IAllinSybService
 *
 * @author gaoye
 * @date 2025/05/20 19:47:43
 * @description xxxxxxxx
 * @since 1.0
 */
public interface IAllinSybService {

    /**
     * 获取聚合扫码支付链接
     */
    String getAggregateCodePayUrl(AggregateCodePayDto aggregateCodePayDto);

    /**
     * 微信、支付宝收银台
     */
    CashierPayVo initCashierPay(CashierPayDto cashierPayDto);

    /**
     * 退款收银台
     */
    RefundVo refund(RefundDto refundDto);

    /**
     * 查询交易状态
     */
    PayStatusVo payStatus(PayStatusDto payStatusDto);

    /**
     * 异步通知处理
     */
    TreeMap<String,String> handleNotify(HttpServletRequest request);

    /**
     * 交易撤销
     */
    CancelVo cancel(CancelDto cancelDto);

    /**
     * 交易关闭
     */
    CloseVo close(CloseDto closeDto);

    /**
     * 商户余额查询
     */
    BalanceVo balance(BalanceDto balanceDto);

    /**
     * 账户余额结算
     */
    BalanceSettlementVo balanceSettlement(BalanceSettlementDto balanceSettlementDto);

    /**
     * 账户余额结算查询
     */
    BalanceSettlementQueryVo balanceSettlementQuery(BalanceSettlementQueryDto balanceSettlementQueryDto);

    /**
     * 获取结算单
     */
    SettlementDocumentVo settlementDocument(SettlementDocumentDto settlementDocumentDto);

    /**
     * 获取对账单
     */
    StatementAccountVo statementOfAccount(StatementAccountDto statementAccountDto);

    /**
     * 分账
     */
    ShareVo share(ShareDto shareDto);

    /**
     * 分账回退
     */
    ShareRevokeVo shareRevoke(ShareRevokeDto shareRevokeDto);

    /**
     * 分账查询
     */
    ShareStatusVo shareStatus(ShareStatusDto shareStatusDto);

    /**
     * 分账回退查询
     */
    ShareRevokeStatusVo shareRevokeStatus(ShareRevokeStatusDto shareRevokeStatusDto);

    /**
     * 统一支付
     */
    UniPayVo uniPay(UniPayDto uniPayDto);

    /**
     * 统一退款
     */
    UniRefundVo uniRefund(UniRefundDto uniRefundDto);

    /**
     * 统一查询
     */
    UniPayStatusVo uniPayStatus(UniPayStatusDto uniPayStatusDto);

    /**
     * 统一撤销
     */
    UniCancelVo uniCancel(UniCancelDto uniCancelDto);

    /**
     * 统一关闭
     */
    UniCloseVo uniClose(UniCloseDto uniCloseDto);
}