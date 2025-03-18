package com.snowdrift.core.context;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * SecurityContext
 *
 * @author gaoye
 * @date 2025/03/18 16:50:58
 * @description 安全上下文
 * @since 1.0.0
 */
@Data
@Builder
public class SecurityContext implements Serializable {

    /**
     * ID
     */
    private Object id;

    /**
     * 名称
     */
    private String name;

    /**
     * 租户ID
     */
    private Object tenantId;

    /**
     * 额外信息
     */
    private Map<String,Object> extra;
}