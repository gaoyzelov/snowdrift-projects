package com.snowdrift.framework.web.handler;

import com.snowdrift.framework.base.constant.StrConst;
import com.snowdrift.framework.base.exception.BizException;
import com.snowdrift.framework.base.result.Result;
import com.snowdrift.framework.base.result.ResultCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * WebExceptionHandler
 *
 * @author gaoyzelov
 * @date 2026/5/8-15:16
 * @description Web统一异常处理
 * @since 1.0.0
 */
@Slf4j
@RestControllerAdvice
public class WebExceptionHandler {

    private static final long KB = 1024;
    private static final long MB = KB * 1024;
    private static final long GB = MB * 1024;

    /**
     * 业务异常处理
     */
    @ExceptionHandler(BizException.class)
    public Result<Void> handleBizException(BizException e, HttpServletRequest request) {
        log.warn("业务异常: uri={}, msg={}", request.getRequestURI(), e.getMessage(), e);
        return Result.err(e.getCode(), e.getMessage());
    }

    /**
     * 参数校验异常处理（@Valid/@Validated）
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException e, HttpServletRequest request) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(StrConst.COMMA));
        log.warn("参数校验失败: uri={}, msg={}", request.getRequestURI(), message, e);
        return Result.err(ResultCode.BAD_REQUEST.code(), message);
    }

    /**
     * 参数绑定异常处理
     */
    @ExceptionHandler(BindException.class)
    public Result<Void> handleBindException(BindException e, HttpServletRequest request) {
        String message = e.getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(StrConst.COMMA));
        log.warn("参数绑定失败: uri={}, msg={}", request.getRequestURI(), message, e);
        return Result.err(ResultCode.BAD_REQUEST.code(), message);
    }

    /**
     * 约束违反异常处理（@Validated on method parameters）
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public Result<Void> handleConstraintViolationException(ConstraintViolationException e, HttpServletRequest request) {
        Set<ConstraintViolation<?>> violations = e.getConstraintViolations();
        String message = violations.stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining(StrConst.COMMA));
        log.warn("约束违反: uri={}, msg={}", request.getRequestURI(), message, e);
        return Result.err(ResultCode.BAD_REQUEST.code(), message);
    }

    /**
     * 参数类型不匹配异常处理
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public Result<Void> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e, HttpServletRequest request) {
        String paramName = e.getName();
        String requiredType = e.getRequiredType() != null ? e.getRequiredType().getSimpleName() : "unknown";
        Object value = e.getValue();
        log.warn("参数类型不匹配: uri={}, paramName={}, requiredType={}, value={}", request.getRequestURI(), paramName, requiredType, value, e);
        return Result.err(ResultCode.BAD_REQUEST.code(), "参数类型不匹配：" + paramName);
    }

    /**
     * JSON 解析异常处理
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<Void> handleHttpMessageNotReadableException(HttpMessageNotReadableException e, HttpServletRequest request) {
        log.warn("请求数据解析失败: uri={}, msg={}", request.getRequestURI(), e.getMessage(), e);
        return Result.err(ResultCode.BAD_REQUEST.code(), "请求数据解析失败");
    }

    /**
     * 文件上传大小超限异常处理
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Result<Void> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e, HttpServletRequest request) {
        log.warn("文件上传大小超限: uri={}, msg={}", request.getRequestURI(), e.getMessage(), e);
        return Result.err(ResultCode.PAYLOAD_TOO_LARGE);
    }

    /**
     * 资源不存在异常处理（404）
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public Result<Void> handleNoResourceFoundException(NoResourceFoundException e, HttpServletRequest request) {
        log.warn("资源不存在: uri={}, msg={}", request.getRequestURI(), e.getMessage(), e);
        return Result.err(ResultCode.NOT_FOUND);
    }

    /**
     * 请求方法不支持异常处理（405）
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public Result<Void> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException e, HttpServletRequest request) {
        log.warn("请求方法不支持: uri={}, msg={}", request.getRequestURI(), e.getMethod(), e);
        return Result.err(ResultCode.METHOD_NOT_ALLOWED);
    }

    /**
     * 媒体类型不支持异常处理（415）
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public Result<Void> handleHttpMediaTypeNotSupportedException(HttpMediaTypeNotSupportedException e, HttpServletRequest request) {
        log.warn("媒体类型不支持: uri={}, msg={}", request.getRequestURI(), e.getContentType(), e);
        return Result.err(ResultCode.UNSUPPORTED_MEDIA_TYPE);
    }

    /**
     * 缺少请求参数异常处理
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Result<Void> handleMissingServletRequestParameterException(MissingServletRequestParameterException e, HttpServletRequest request) {
        log.warn("缺少请求参数: uri={}, msg={}", request.getRequestURI(), e.getParameterName(), e);
        return Result.err(ResultCode.BAD_REQUEST.code(), "缺少请求参数：" + e.getParameterName());
    }

    /**
     * 缺少路径变量异常处理
     */
    @ExceptionHandler(MissingPathVariableException.class)
    public Result<Void> handleMissingPathVariableException(MissingPathVariableException e, HttpServletRequest request) {
        log.warn("缺少路径变量: uri={}, msg={}", request.getRequestURI(), e.getVariableName(), e);
        return Result.err(ResultCode.BAD_REQUEST.code(), "缺少路径变量：" + e.getVariableName());
    }

    /**
     * 缺少请求部分异常处理（multipart/form-data）
     */
    @ExceptionHandler(MissingServletRequestPartException.class)
    public Result<Void> handleMissingServletRequestPartException(MissingServletRequestPartException e, HttpServletRequest request) {
        log.warn("缺少请求部分: uri={}, msg={}", request.getRequestURI(), e.getRequestPartName(), e);
        return Result.err(ResultCode.BAD_REQUEST.code(), "缺少请求部分：" + e.getRequestPartName());
    }

    /**
     * 非法参数异常处理
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public Result<Void> handleIllegalArgumentException(IllegalArgumentException e, HttpServletRequest request) {
        log.warn("非法参数: uri={}, msg={}", request.getRequestURI(), e.getMessage(), e);
        return Result.err(ResultCode.BAD_REQUEST.code(), "非法参数");
    }

    /**
     * 空指针异常处理
     */
    @ExceptionHandler(NullPointerException.class)
    public Result<Void> handleNullPointerException(NullPointerException e, HttpServletRequest request) {
        log.error("空指针异常: uri={}, msg={}", request.getRequestURI(), e.getMessage(), e);
        return Result.err(ResultCode.INTERNAL_SERVER_ERROR);
    }

    /**
     * 其他未捕获的异常处理
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e, HttpServletRequest request) {
        log.error("系统异常: uri={}, msg={}", request.getRequestURI(), e.getMessage(), e);
        return Result.err(ResultCode.INTERNAL_SERVER_ERROR);
    }
}
