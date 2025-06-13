package com.snowdrift.pay.allin.dto.brand;


import com.alibaba.fastjson2.JSON;
import com.snowdrift.core.constant.DatePatternConst;
import com.snowdrift.core.utils.DateTimeUtil;
import com.snowdrift.pay.allin.utils.BrandUtil;
import com.snowdrift.pay.allin.utils.SybUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * AllinBrandDto
 *
 * @author gaoye
 * @date 2025/05/29 13:23:16
 * @description xxxxxxxx
 * @since 1.0
 */
@Data
@Schema(title = "AllinBrandDto", description = "智品牌请求参数")
public class AllinBrandDto<T> implements Serializable {

    @Schema(title = "应用ID", required = true)
    @NotBlank(message = "应用ID不能为空")
    private String appId;

    @Schema(title = "版本号", required = true)
    @NotBlank(message = "版本号不能为空")
    private String version = "1.0";

    @Schema(title = "请求使用的编码格式", required = true)
    @NotBlank(message = "请求使用的编码格式不能为空")
    private String charset = "UTF-8";

    @Schema(title = "签名方式,目前支持MD5", required = true)
    @NotBlank(message = "签名方式不能为空")
    private String signType = SybUtil.MD5;

    @Schema(title = "签名", required = true)
    @NotBlank(message = "签名不能为空")
    private String sign;

    @Schema(title = "接口名称", required = true)
    @NotBlank(message = "接口名称不能为空")
    private String service;

    @Schema(title = "发送请求的时间，格式yyyyMMddHHmmss", required = true)
    @NotBlank(message = "发送请求的时间不能为空")
    private String timestamp = DateTimeUtil.getDateTimeStr(LocalDateTime.now(), DatePatternConst.PURE_DATETIME_PATTERN);

    @Schema(title = "业务参数，JSON字符串", required = true)
    @NotNull(message = "业务参数不能为空")
    private T bizContent;

    /**
     * 签名
     *
     * @param key 密钥
     */
    public void sign(String key) {
        TreeMap<String, String> param = new TreeMap<>();
        param.put("appId", appId);
        param.put("version", version);
        param.put("charset", charset);
        param.put("signType", signType);
        param.put("timestamp", timestamp);
        param.put("service", service);
        param.put("bizContent", JSON.toJSONString(bizContent));
        param.put("key", key);
        this.sign = BrandUtil.sign(param);
    }

    /**
     * 转map
     */
    public Map<String, Object> toMap() {
        Map<String, Object> param = new HashMap<>();
        param.put("appId", appId);
        param.put("version", version);
        param.put("charset", charset);
        param.put("signType", signType);
        param.put("timestamp", timestamp);
        param.put("service", service);
        param.put("sign", sign);
        param.put("bizContent", JSON.toJSONString(bizContent));
        return param;
    }
}