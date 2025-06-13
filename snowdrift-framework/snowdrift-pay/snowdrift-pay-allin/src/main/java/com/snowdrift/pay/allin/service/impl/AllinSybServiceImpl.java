package com.snowdrift.pay.allin.service.impl;


import com.alibaba.fastjson2.JSON;
import com.snowdrift.core.exception.BaseException;
import com.snowdrift.core.utils.HttpUtil;
import com.snowdrift.pay.allin.dto.*;
import com.snowdrift.pay.allin.enums.AllinApiEnum;
import com.snowdrift.pay.allin.enums.NotifyFieldEnum;
import com.snowdrift.pay.allin.enums.RetCodeEum;
import com.snowdrift.pay.allin.properties.AllinSybProperties;
import com.snowdrift.pay.allin.service.IAllinSybService;
import com.snowdrift.pay.allin.utils.SybUtil;
import com.snowdrift.pay.allin.vo.*;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.cglib.beans.BeanMap;
import org.springframework.lang.NonNull;

import java.net.URLEncoder;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.TreeMap;

/**
 * AllinSybServiceImpl
 *
 * @author gaoye
 * @date 2025/05/20 19:47:56
 * @description xxxxxxxx
 * @since 1.0
 */
public class AllinSybServiceImpl implements IAllinSybService {

    private final AllinSybProperties prop;

    public AllinSybServiceImpl(@NonNull AllinSybProperties prop) {
        this.prop = prop;
    }

    /**
     * 初始化参数
     */
    private <T extends AllinPayDto> void initDto(T t) {
        if (StringUtils.isNotBlank(prop.getOrgId())) {
            t.setOrgid(prop.getOrgId());
        }
        t.setAppid(prop.getAppId());
        t.setCusid(prop.getCusId());
    }

    /**
     * 签名
     */
    private String signDto(Object obj, String signType) {
        TreeMap<String, String> data = SybUtil.objToTreeMap(obj);
        try {
            if (StringUtils.equals(signType, SybUtil.MD5)) {
                return SybUtil.unionSign(data, prop.getMd5key(), SybUtil.MD5);
            }
            if (StringUtils.equals(signType, SybUtil.RSA)) {
                return SybUtil.unionSign(data, prop.getRsaPriKey(), SybUtil.RSA);
            }
        } catch (Exception e) {
            throw new BaseException("签名异常");
        }
        throw new BaseException("不支持的签名类型");
    }

    /**
     * 获取聚合码支付链接地址
     *
     * @param aggregateCodePayDto 聚合码支付参数
     * @return 支付链接地址
     */
    @Override
    public String getAggregateCodePayUrl(AggregateCodePayDto aggregateCodePayDto) {
        initDto(aggregateCodePayDto);
        String sign = signDto(aggregateCodePayDto, aggregateCodePayDto.getSigntype());
        try {
            aggregateCodePayDto.setSign(URLEncoder.encode(sign, "UTF-8"));
        } catch (Exception e) {
            throw new BaseException("签名URL编码异常");
        }
//        ObjUtil.validate(aggregateCodePayDto, true);
        return String.format("%s?%s", AllinApiEnum.AGGREGATE_PAY.getUrl(), SybUtil.treeMapToUrlParams(SybUtil.objToTreeMap(aggregateCodePayDto)));
    }

    /**
     * 微信小程序及APP下微信、支付宝的收银台支付参数准备
     *
     * @param cashierPayDto 支付参数
     * @return 加签后的参数
     */
    @Override
    public CashierPayVo initCashierPay(CashierPayDto cashierPayDto) {
        CashierPayVo cashierPayVo = new CashierPayVo();
        BeanUtils.copyProperties(cashierPayDto, cashierPayVo);
        initDto(cashierPayVo);
        String sign = signDto(cashierPayVo, cashierPayVo.getSigntype());
        cashierPayVo.setSign(sign);
        return cashierPayVo;
    }


    /**
     * 聚合支付退款
     *
     * @param refundDto 聚合支付退款参数
     * @return 退款响应数据
     */
    @Override
    public RefundVo refund(RefundDto refundDto) {
        initDto(refundDto);
        String sign = signDto(refundDto, refundDto.getSigntype());
        refundDto.setSign(sign);
//        ObjUtil.validate(refundDto, true);
        String response = HttpUtil.post(AllinApiEnum.REFUND.getUrl(), BeanMap.create(refundDto), 5000);
        return verifyResult(response, refundDto.getSigntype(), RefundVo.class);
    }

    /**
     * 交易状态查询
     *
     * @param payStatusDto 交易状态查询参数
     * @return 交易状态查询结果
     */
    @Override
    public PayStatusVo payStatus(PayStatusDto payStatusDto) {
        initDto(payStatusDto);
        String sign = signDto(payStatusDto, payStatusDto.getSigntype());
        payStatusDto.setSign(sign);
        String response = HttpUtil.post(AllinApiEnum.PAY_STATUS.getUrl(), BeanMap.create(payStatusDto), 5000);
        return verifyResult(response, payStatusDto.getSigntype(), PayStatusVo.class);
    }

    /**
     * 处理异步通知
     *
     * @param request 请求
     * @return 异步通知数据
     */
    @Override
    public TreeMap<String, String> handleNotify(HttpServletRequest request) {
        try {
            request.setCharacterEncoding("UTF-8");
            TreeMap<String, String> params = SybUtil.getRequestParams(request);
            boolean valid = false;
            String singType = params.get(NotifyFieldEnum.SIGN_TYPE.getCode());
            if (StringUtils.equals(SybUtil.MD5, singType)) {
                valid = SybUtil.validSign(params, prop.getMd5key(), SybUtil.MD5);
            }
            if (StringUtils.equals(SybUtil.RSA, singType)) {
                valid = SybUtil.validSign(params, prop.getTlsPubKey(), SybUtil.RSA);
            }
            if (valid) {
                return params;
            }
        } catch (Exception e) {
            log.error("处理异步通知异常：{}", e.getLocalizedMessage());
        }
        return null;
    }

    /**
     * 交易撤销
     *
     * @param cancelDto 撤销参数
     * @return 撤销结果
     */
    @Override
    public CancelVo cancel(CancelDto cancelDto) {
        initDto(cancelDto);
        String sign = signDto(cancelDto, cancelDto.getSigntype());
        cancelDto.setSign(sign);
        String response = HttpUtil.post(AllinApiEnum.CANCEL.getUrl(), BeanMap.create(cancelDto), 5000);
        return verifyResult(response, cancelDto.getSigntype(), CancelVo.class);
    }

    /**
     * 关闭订单
     *
     * @param closeDto 关闭参数
     * @return 关闭结果
     */
    @Override
    public CloseVo close(CloseDto closeDto) {
        initDto(closeDto);
        String sign = signDto(closeDto, closeDto.getSigntype());
        closeDto.setSign(sign);
        String response = HttpUtil.post(AllinApiEnum.CANCEL.getUrl(), BeanMap.create(closeDto), 5000);
        return verifyResult(response, closeDto.getSigntype(), CloseVo.class);
    }

    /**
     * 商户账户余额查询
     *
     * @param balanceDto 余额查询参数
     * @return 余额查询结果
     */
    @Override
    public BalanceVo balance(BalanceDto balanceDto) {
        initDto(balanceDto);
        String sign = signDto(balanceDto, balanceDto.getSigntype());
        balanceDto.setSign(sign);
        String response = HttpUtil.post(AllinApiEnum.BALANCE.getUrl(), BeanMap.create(balanceDto), 5000);
        return verifyResult(response, balanceDto.getSigntype(), BalanceVo.class);
    }

    /**
     * 商户账户余额结算
     *
     * @param balanceSettlementDto 余额结算参数
     * @return 结果
     */
    @Override
    public BalanceSettlementVo balanceSettlement(BalanceSettlementDto balanceSettlementDto) {
        initDto(balanceSettlementDto);
        String sign = signDto(balanceSettlementDto, balanceSettlementDto.getSigntype());
        balanceSettlementDto.setSign(sign);
        String response = HttpUtil.post(AllinApiEnum.BALANCE_SETTLEMENT.getUrl(), BeanMap.create(balanceSettlementDto), 5000);
        return verifyResult(response, balanceSettlementDto.getSigntype(), BalanceSettlementVo.class);
    }

    /**
     * 商户账户余额结算查询
     *
     * @param balanceSettlementQueryDto 查询参数
     * @return 查询结果
     */
    @Override
    public BalanceSettlementQueryVo balanceSettlementQuery(BalanceSettlementQueryDto balanceSettlementQueryDto) {
        int hour = LocalDateTime.now().getHour();
        if (hour < 6 || hour == 23) {
            log.error("商户账户余额结算查询支持时间段6-23点");
        }
        initDto(balanceSettlementQueryDto);
        String sign = signDto(balanceSettlementQueryDto, balanceSettlementQueryDto.getSigntype());
        balanceSettlementQueryDto.setSign(sign);
        String response = HttpUtil.post(AllinApiEnum.BALANCE_SETTLEMENT_QUERY.getUrl(), BeanMap.create(balanceSettlementQueryDto), 5000);
        return verifyResult(response, balanceSettlementQueryDto.getSigntype(), BalanceSettlementQueryVo.class);
    }

    /**
     * 获取结算单
     *
     * @param settlementDocumentDto 获取结算单参数
     * @return 结算单数据
     */
    @Override
    public SettlementDocumentVo settlementDocument(SettlementDocumentDto settlementDocumentDto) {
        initDto(settlementDocumentDto);
        String sign = signDto(settlementDocumentDto, settlementDocumentDto.getSigntype());
        settlementDocumentDto.setSign(sign);
        String response = HttpUtil.post(AllinApiEnum.SETTLEMENT_DOCUMENT.getUrl(), BeanMap.create(settlementDocumentDto), 5000);
        return verifyResult(response, settlementDocumentDto.getSigntype(), SettlementDocumentVo.class);
    }

    /**
     * 获取对账单
     *
     * @param statementAccountDto 对账单获取参数
     * @return 对账单结果
     */
    @Override
    public StatementAccountVo statementOfAccount(StatementAccountDto statementAccountDto) {
        initDto(statementAccountDto);
        String sign = signDto(statementAccountDto, statementAccountDto.getSigntype());
        statementAccountDto.setSign(sign);
        String response = HttpUtil.post(AllinApiEnum.SETTLEMENT_DOCUMENT.getUrl(), BeanMap.create(statementAccountDto), 5000);
        return verifyResult(response, statementAccountDto.getSigntype(), StatementAccountVo.class);
    }

    /**
     * 交易分账
     *
     * @param shareDto 分账参数
     * @return 分账响应
     */
    @Override
    public ShareVo share(ShareDto shareDto) {
        initDto(shareDto);
        String sign = signDto(shareDto, shareDto.getSigntype());
        shareDto.setSign(sign);
        String response = HttpUtil.post(AllinApiEnum.SETTLEMENT_DOCUMENT.getUrl(), BeanMap.create(shareDto), 5000);
        return verifyResult(response, shareDto.getSigntype(), ShareVo.class);
    }

    /**
     * 交易分账回退
     *
     * @param shareRevokeDto 分账回退参数
     * @return 分账回退结果
     */
    @Override
    public ShareRevokeVo shareRevoke(ShareRevokeDto shareRevokeDto) {
        initDto(shareRevokeDto);
        String sign = signDto(shareRevokeDto, shareRevokeDto.getSigntype());
        shareRevokeDto.setSign(sign);
        String response = HttpUtil.post(AllinApiEnum.SETTLEMENT_DOCUMENT.getUrl(), BeanMap.create(shareRevokeDto), 5000);
        return verifyResult(response, shareRevokeDto.getSigntype(), ShareRevokeVo.class);
    }

    /**
     * 分账状态查询
     *
     * @param shareStatusDto 分账状态查询参数
     * @return 分账状态查询响应数据
     */
    @Override
    public ShareStatusVo shareStatus(ShareStatusDto shareStatusDto) {
        initDto(shareStatusDto);
        String sign = signDto(shareStatusDto, shareStatusDto.getSigntype());
        shareStatusDto.setSign(sign);
        String response = HttpUtil.post(AllinApiEnum.SETTLEMENT_DOCUMENT.getUrl(), BeanMap.create(shareStatusDto), 5000);
        return verifyResult(response, shareStatusDto.getSigntype(), ShareStatusVo.class);
    }

    /**
     * 分账回退查询
     *
     * @param shareRevokeStatusDto 分账回退查询参数
     * @return 分账回退查询响应数据
     */
    @Override
    public ShareRevokeStatusVo shareRevokeStatus(ShareRevokeStatusDto shareRevokeStatusDto) {
        initDto(shareRevokeStatusDto);
        String sign = signDto(shareRevokeStatusDto, shareRevokeStatusDto.getSigntype());
        shareRevokeStatusDto.setSign(sign);
        String response = HttpUtil.post(AllinApiEnum.SETTLEMENT_DOCUMENT.getUrl(), BeanMap.create(shareRevokeStatusDto), 5000);
        return verifyResult(response, shareRevokeStatusDto.getSigntype(), ShareRevokeStatusVo.class);
    }

    /**
     * 统一支付
     *
     * @param uniPayDto 统一支付参数
     * @return 统一支付结果
     */
    @Override
    public UniPayVo uniPay(UniPayDto uniPayDto) {
        initDto(uniPayDto);
        String sign = signDto(uniPayDto, uniPayDto.getSigntype());
        uniPayDto.setSign(sign);
        String response = HttpUtil.post(AllinApiEnum.UNI_APY.getUrl(), BeanMap.create(uniPayDto), 5000);
        return verifyResult(response, uniPayDto.getSigntype(), UniPayVo.class);
    }

    /**
     * 统一退款
     *
     * @param uniRefundDto 统一退款参数
     * @return 统一退款结果
     */
    @Override
    public UniRefundVo uniRefund(UniRefundDto uniRefundDto) {
        initDto(uniRefundDto);
        String sign = signDto(uniRefundDto, uniRefundDto.getSigntype());
        uniRefundDto.setSign(sign);
        String response = HttpUtil.post(AllinApiEnum.UNI_REFUND.getUrl(), BeanMap.create(uniRefundDto), 5000);
        return verifyResult(response, uniRefundDto.getSigntype(), UniRefundVo.class);
    }

    /**
     * 统一查询
     *
     * @param uniPayStatusDto 查询参数
     * @return 结果
     */
    @Override
    public UniPayStatusVo uniPayStatus(UniPayStatusDto uniPayStatusDto) {
        initDto(uniPayStatusDto);
        String sign = signDto(uniPayStatusDto, uniPayStatusDto.getSigntype());
        uniPayStatusDto.setSign(sign);
        String response = HttpUtil.post(AllinApiEnum.UNI_PAY_STATUS.getUrl(), BeanMap.create(uniPayStatusDto), 5000);
        return verifyResult(response, uniPayStatusDto.getSigntype(), UniPayStatusVo.class);
    }

    /**
     * 统一撤销
     *
     * @param uniCancelDto 撤销参数
     * @return 撤销结果
     */
    @Override
    public UniCancelVo uniCancel(UniCancelDto uniCancelDto) {
        initDto(uniCancelDto);
        String sign = signDto(uniCancelDto, uniCancelDto.getSigntype());
        uniCancelDto.setSign(sign);
        String response = HttpUtil.post(AllinApiEnum.UNI_CANCEL.getUrl(), BeanMap.create(uniCancelDto), 5000);
        return verifyResult(response, uniCancelDto.getSigntype(), UniCancelVo.class);
    }

    /**
     * 统一关闭
     *
     * @param uniCloseDto 关闭参数
     * @return 关闭结果
     */
    @Override
    public UniCloseVo uniClose(UniCloseDto uniCloseDto) {
        initDto(uniCloseDto);
        String sign = signDto(uniCloseDto, uniCloseDto.getSigntype());
        uniCloseDto.setSign(sign);
        String response = HttpUtil.post(AllinApiEnum.UNI_CLOSE.getUrl(), BeanMap.create(uniCloseDto), 5000);
        return verifyResult(response, uniCloseDto.getSigntype(), UniCloseVo.class);
    }

    /**
     * 校验返回结果
     *
     * @param json     返回结果
     * @param signType 签名方式
     * @param clazz    返回结果类型
     */
    private <T> T verifyResult(String json, String signType, Class<T> clazz) {
        Map map = JSON.parseObject(json, Map.class);
        if (map == null) {
            throw new BaseException("返回数据错误");
        }
        if (!RetCodeEum.SUCCESS.getCode().equals(map.get("retcode"))) {
            throw new BaseException(map.get("retmsg").toString());
        }
        TreeMap data = new TreeMap(map);
        try {
            if (SybUtil.validSign(data, prop.getTlsPubKey(), signType)) {
                // 验签成功，返回数据
                return JSON.parseObject(json, clazz);
            }
        } catch (Exception e) {
            throw new BaseException("验证签名异常");
        }
        throw new BaseException("验证签名失败");
    }
}