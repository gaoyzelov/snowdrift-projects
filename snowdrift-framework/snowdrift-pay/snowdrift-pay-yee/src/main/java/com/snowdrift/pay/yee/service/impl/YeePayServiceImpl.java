package com.snowdrift.pay.yee.service.impl;

import com.alibaba.fastjson2.JSON;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.snowdrift.core.exception.BaseException;
import com.snowdrift.pay.yee.dto.*;
import com.snowdrift.pay.yee.enums.ApiEnum;
import com.snowdrift.pay.yee.enums.CodeEnum;
import com.snowdrift.pay.yee.properties.YeeProperties;
import com.snowdrift.pay.yee.service.IYeePayService;
import com.snowdrift.pay.yee.vo.*;
import com.yeepay.yop.sdk.inter.utils.RSAKeyUtils;
import com.yeepay.yop.sdk.service.common.YopClient;
import com.yeepay.yop.sdk.service.common.YopClientBuilder;
import com.yeepay.yop.sdk.service.common.request.YopRequest;
import com.yeepay.yop.sdk.service.common.response.YopResponse;
import com.yeepay.yop.sdk.utils.DigitalEnvelopeUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.security.PrivateKey;
import java.util.Map;

/**
 * YeePayServiceImpl
 *
 * @author gaoye
 * @date 2025/06/05 17:44:48
 * @description xxxxxxxx
 * @since 1.0
 */
@Slf4j
public class YeePayServiceImpl implements IYeePayService {

    private final YeeProperties prop;
    private final YopClient client;

    public YeePayServiceImpl(YeeProperties prop) {
        this.prop = prop;
        client = YopClientBuilder.builder().build();
    }

    /**
     * 聚合扫码支付
     *
     * @param aggregateCodeDto 聚合支付参数
     * @return AggregatePayVo
     */
    @Override
    public AggregateCodeVo aggregateCode(AggregateCodeDto aggregateCodeDto) {
        return doExecute(ApiEnum.AGGREGATE_CODE, aggregateCodeDto, AggregateCodeVo.class);
    }

    /**
     * 异步通知处理
     *
     * @param orderNotifyDto 订单通知参数
     */
    @Override
    public <T extends INotifyVo> T handleOrderNotify(OrderNotifyDto orderNotifyDto, Class<T> clazz) {
        try {
            PrivateKey privateKey = RSAKeyUtils.string2PrivateKey(prop.getPrivateKey());
            String decrypt = DigitalEnvelopeUtils.decrypt(orderNotifyDto.getResponse(), privateKey);
            return JSON.parseObject(decrypt, clazz);
        } catch (Exception e) {
            log.error("订单异步通知解析失败：{}", e.getMessage(), e);
        }
        return null;
    }

    /**
     * 统一下单
     *
     * @param uniOrderDto 统一下单参数
     * @return UniOrderVo
     */
    @Override
    public UniOrderVo uniOrder(UniOrderDto uniOrderDto) {
        return doExecute(ApiEnum.UNI_ORDER, uniOrderDto, UniOrderVo.class);
    }

    /**
     * 订单查询
     *
     * @param orderQueryDto 订单查询参数
     * @return OrderQueryVo
     */
    @Override
    public OrderQueryVo queryOrder(OrderQueryDto orderQueryDto) {
        return doExecute(ApiEnum.ORDER_QUERY, orderQueryDto, OrderQueryVo.class, CodeEnum.OPR_SUCCESS);
    }

    /**
     * 退款申请
     *
     * @param refundApplyDto 退款申请参数
     * @return RefundApplyVo
     */
    @Override
    public RefundApplyVo refundApply(RefundApplyDto refundApplyDto) {
        return doExecute(ApiEnum.REFUND_APPLY, refundApplyDto, RefundApplyVo.class, CodeEnum.OPR_SUCCESS);
    }

    /**
     * 退款查询
     *
     * @param refundQueryDto 退款查询参数
     * @return RefundQueryVo
     */
    @Override
    public RefundQueryVo refundQuery(RefundQueryDto refundQueryDto) {
        return doExecute(ApiEnum.REFUND_QUERY, refundQueryDto, RefundQueryVo.class, CodeEnum.OPR_SUCCESS);
    }

    /**
     * 提现申请
     *
     * @param withdrawApplyDto 提现申请参数
     * @return WithdrawApplyVo
     */
    @Override
    public WithdrawApplyVo withdrawApply(WithdrawApplyDto withdrawApplyDto) {
        return doExecute(ApiEnum.WITHDRAW_APPLY, withdrawApplyDto, WithdrawApplyVo.class, CodeEnum.UA_SUCCESS);
    }

    /**
     * 提现查询
     * @param withdrawQueryDto 提现查询参数
     * @return WithdrawQueryVo
     */
    @Override
    public WithdrawQueryVo withdrawQuery(WithdrawQueryDto withdrawQueryDto) {
        return doExecute(ApiEnum.WITHDRAW_QUERY, withdrawQueryDto, WithdrawQueryVo.class, CodeEnum.UA_SUCCESS);
    }

    /**
     * 提现卡绑定
     *
     * @param withdrawCardBindDto 提现卡绑定参数
     * @return WithdrawCardBindVo
     */
    @Override
    public WithdrawCardBindVo withdrawCardBind(WithdrawCardBindDto withdrawCardBindDto) {
        return doExecute(ApiEnum.WITHDRAW_CARD_BIND, withdrawCardBindDto, WithdrawCardBindVo.class, CodeEnum.UA_SUCCESS);
    }

    /**
     * 提现卡查询
     *
     * @param withdrawCardQueryDto 提现卡查询参数
     * @return WithdrawCardQueryVo
     */
    @Override
    public WithdrawCardQueryVo withdrawCardQuery(WithdrawCardQueryDto withdrawCardQueryDto) {
        return doExecute(ApiEnum.WITHDRAW_CARD_QUERY, withdrawCardQueryDto, WithdrawCardQueryVo.class, CodeEnum.UA_SUCCESS);
    }

    /**
     * 提现卡修改/注销
     *
     * @param withdrawCardModifyDto 提现卡修改/注销参数
     * @return WithdrawCardModifyVo
     */
    @Override
    public WithdrawCardModifyVo withdrawCardModify(WithdrawCardModifyDto withdrawCardModifyDto) {
        return doExecute(ApiEnum.WITHDRAW_CARD_MODIFY, withdrawCardModifyDto, WithdrawCardModifyVo.class, CodeEnum.UA_SUCCESS);
    }

    /**
     * 执行请求
     *
     * @param api   api
     * @param t     参数
     * @param clazz 返回类型
     */
    private <T extends RequestDto, R extends ResponseVo> R doExecute(ApiEnum api, T t, Class<R> clazz) {
        return doExecute(api, t, clazz, CodeEnum.SUCCESS);
    }

    /**
     * 执行请求
     *
     * @param api   api
     * @param t     参数
     * @param clazz 返回类型
     */
    private <T extends RequestDto, R extends ResponseVo> R doExecute(ApiEnum api, T t, Class<R> clazz, CodeEnum ok) {
        // 用户未指定，则使用全局默认设置
        if (StringUtils.isBlank(t.getParentMerchantNo()) && StringUtils.isNotBlank(prop.getParentMerchantNo())){
            t.setParentMerchantNo(prop.getParentMerchantNo());
        }
        if (StringUtils.isBlank(t.getMerchantNo()) && StringUtils.isNotBlank(prop.getMerchantNo())){
            t.setMerchantNo(prop.getMerchantNo());
        }
        YopRequest request = new YopRequest(api.getUri(), api.getMethod());
        // TODO
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String,Object> map = objectMapper.convertValue(t, Map.class);
        map.forEach(request::addParameter);
        R r;
        try {
            YopResponse response = client.request(request);
            r = JSON.parseObject(response.getStringResult(), clazz);
        } catch (Exception ex) {
            log.error("易宝支付接口调用失败:{}", ex.getMessage(), ex);
            throw new BaseException("易宝支付接口调用异常");
        }
        if (StringUtils.equals(r.getCode(), ok.getCode())) {
            return r;
        }
        throw new BaseException(r.getMessage());
    }
}