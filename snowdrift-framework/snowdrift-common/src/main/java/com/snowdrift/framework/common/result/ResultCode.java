package com.snowdrift.framework.common.result;

/**
 * ResultCode
 * @author gaoyzelov
 * @date 2026/4/30-10:57
 * @description 响应结果状态码（支持国际化）
 *              msg 字段存储国际化消息 key，而非硬编码文本
 * @since 1.0.0
 */
public record ResultCode(int code, String msg) {

    // ========== 通用状态码 ==========
    /** 成功 */
    public static final ResultCode OK = new ResultCode(1, "common.success");
    /** 失败 */
    public static final ResultCode ERR = new ResultCode(0, "common.error");

    // ========== 客户端错误 4xx ==========
    /** 请求参数错误 */
    public static final ResultCode BAD_REQUEST = new ResultCode(400, "common.bad.request");
    /** 未授权 */
    public static final ResultCode UNAUTHORIZED = new ResultCode(401, "common.unauthorized");
    /** 禁止访问 */
    public static final ResultCode FORBIDDEN = new ResultCode(403, "common.forbidden");
    /** 资源不存在 */
    public static final ResultCode NOT_FOUND = new ResultCode(404, "common.not.found");
    /** 不支持的请求方法 */
    public static final ResultCode METHOD_NOT_ALLOWED = new ResultCode(405, "common.method.not.allowed");
    /** 资源冲突 */
    public static final ResultCode CONFLICT = new ResultCode(409, "common.conflict");
    /** 请求体过大 */
    public static final ResultCode PAYLOAD_TOO_LARGE = new ResultCode(413, "common.payload.too.large");
    /** 不支持的媒体类型 */
    public static final ResultCode UNSUPPORTED_MEDIA_TYPE = new ResultCode(415, "common.media.type.not.supported");
    /** 请求过于频繁 */
    public static final ResultCode TOO_MANY_REQUESTS = new ResultCode(429, "common.too.many.requests");

    // ========== 服务端错误 5xx ==========
    /** 服务器内部错误 */
    public static final ResultCode INTERNAL_SERVER_ERROR = new ResultCode(500, "common.internal.error");
    /** 服务不可用 */
    public static final ResultCode SERVICE_UNAVAILABLE = new ResultCode(503, "common.service.unavailable");
}
