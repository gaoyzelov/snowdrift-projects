package com.snowdrift.pay.yee.service;

import com.snowdrift.pay.yee.dto.*;
import com.snowdrift.pay.yee.vo.*;

/**
 * IYeePayService
 *
 * @author gaoye
 * @date 2025/06/05 17:44:24
 * @description xxxxxxxx
 * @since 1.0
 */
public interface IYeePayService {

    AggregateCodeVo aggregateCode(AggregateCodeDto aggregateCodeDto);

    <T extends INotifyVo> T handleOrderNotify(OrderNotifyDto orderNotifyDto, Class<T> clazz);

    UniOrderVo uniOrder(UniOrderDto uniOrderDto);

    OrderQueryVo queryOrder(OrderQueryDto orderQueryDto);

    RefundApplyVo refundApply(RefundApplyDto refundApplyDto);

    RefundQueryVo refundQuery(RefundQueryDto refundQueryDto);

    WithdrawApplyVo withdrawApply(WithdrawApplyDto withdrawApplyDto);

    WithdrawQueryVo withdrawQuery(WithdrawQueryDto withdrawQueryDto);

    WithdrawCardBindVo withdrawCardBind(WithdrawCardBindDto withdrawCardBindDto);

    WithdrawCardQueryVo withdrawCardQuery(WithdrawCardQueryDto withdrawCardQueryDto);

    WithdrawCardModifyVo withdrawCardModify(WithdrawCardModifyDto withdrawCardModifyDto);
}