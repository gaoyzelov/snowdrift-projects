package com.snowdrift.framework.base.result;

/**
 * ResultCode
 * @author gaoyzelov
 * @date 2026/4/30-10:57
 * @description 业务状态码。code 为业务码而非 HTTP 状态码（HTTP 状态由框架/容器单独决定，
 * 业务错误统一通过 body 中的 code 区分）。取值刻意避开 HTTP 状态码区间（100-599），避免语义混淆。
 * 约定：0/1 为通用成功/失败；1xxx 为调用方/资源侧错误；2xxx 为服务端错误。
 * msg 为返回给前端的用户友好文案（避免暴露 锁/媒体类型 等实现术语）。
 * @since 1.0.0
 */
public record ResultCode(int code, String msg) {

    // ========== 通用状态码 ==========
    /** 成功 */
    public static final ResultCode OK = new ResultCode(1, "操作成功");
    /** 失败 */
    public static final ResultCode ERR = new ResultCode(0, "操作失败");

    // ========== 调用方/资源侧错误（1xxx） ==========
    /** 请求参数错误 */
    public static final ResultCode BAD_REQUEST = new ResultCode(1000, "请求参数有误，请检查后重试");
    /** 未授权 */
    public static final ResultCode UNAUTHORIZED = new ResultCode(1001, "未登录或登录已过期，请重新登录");
    /** 禁止访问 */
    public static final ResultCode FORBIDDEN = new ResultCode(1002, "无权限执行该操作");
    /** 资源不存在 */
    public static final ResultCode NOT_FOUND = new ResultCode(1003, "请求的资源不存在");
    /** 不支持的请求方法 */
    public static final ResultCode METHOD_NOT_ALLOWED = new ResultCode(1004, "请求方式不被支持");
    /** 资源冲突 */
    public static final ResultCode CONFLICT = new ResultCode(1005, "数据已被他人修改或冲突，请刷新后重试");
    /** 分布式锁获取失败（锁被占用） */
    public static final ResultCode LOCK_FAILED = new ResultCode(1006, "操作正在处理中，请勿重复提交");
    /** 请求体过大 */
    public static final ResultCode PAYLOAD_TOO_LARGE = new ResultCode(1007, "提交内容过大，超出服务器限制");
    /** 不支持的媒体类型 */
    public static final ResultCode UNSUPPORTED_MEDIA_TYPE = new ResultCode(1008, "请求的内容类型不受支持");
    /** 请求过于频繁 */
    public static final ResultCode TOO_MANY_REQUESTS = new ResultCode(1009, "请求过于频繁，请稍后再试");

    // ========== 服务端错误（2xxx） ==========
    /** 服务器内部错误 */
    public static final ResultCode INTERNAL_SERVER_ERROR = new ResultCode(2000, "系统繁忙，请稍后重试");
    /** 服务不可用 */
    public static final ResultCode SERVICE_UNAVAILABLE = new ResultCode(2001, "服务暂时不可用，请稍后重试");
}
