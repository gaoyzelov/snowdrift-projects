package com.snowdrift.framework.web.handler;

import com.snowdrift.framework.common.exception.BizException;
import com.snowdrift.framework.common.result.Result;
import com.snowdrift.framework.common.result.ResultCode;
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
 * @author 83674
 * @date 2026/5/8-15:16
 * @description Web统一异常处理
 * @since 1.0.0
 */
@Slf4j
@RestControllerAdvice
public class WebExceptionHandler {

    /**
     * 业务异常处理
     */
    @ExceptionHandler(BizException.class)
    public Result<Void> handleBizException(BizException e) {
        log.warn("业务异常: {}", e.getMessage(), e);
        return Result.err(e.getCode(), e.getMessage());
    }

    /**
     * 参数校验异常处理（@Valid/@Validated）
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        log.warn("参数校验失败: {}", message);
        return Result.err(ResultCode.BAD_REQUEST.code(), "参数校验失败: " + message);
    }

    /**
     * 参数绑定异常处理
     */
    @ExceptionHandler(BindException.class)
    public Result<Void> handleBindException(BindException e) {
        String message = e.getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        log.warn("参数绑定失败: {}", message);
        return Result.err(ResultCode.BAD_REQUEST.code(), "参数绑定失败: " + message);
    }

    /**
     * 约束违反异常处理（@Validated on method parameters）
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public Result<Void> handleConstraintViolationException(ConstraintViolationException e) {
        Set<ConstraintViolation<?>> violations = e.getConstraintViolations();
        String message = violations.stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining(", "));
        log.warn("约束违反: {}", message);
        return Result.err(ResultCode.BAD_REQUEST.code(), "参数约束违反: " + message);
    }

    /**
     * 参数类型不匹配异常处理
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public Result<Void> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e) {
        String paramName = e.getName();
        String requiredType = e.getRequiredType() != null ? e.getRequiredType().getSimpleName() : "unknown";
        Object value = e.getValue();
        String message = String.format("参数 '%s' 类型错误，期望类型: %s, 实际值: %s", paramName, requiredType, value);
        log.warn("参数类型不匹配: {}", message);
        return Result.err(ResultCode.BAD_REQUEST.code(), message);
    }

    /**
     * JSON 解析异常处理
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<Void> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        String message = "请求数据格式错误";
        // 尝试提取更详细的错误信息
        if (e.getCause() != null) {
            String causeMessage = e.getCause().getMessage();
            if (causeMessage != null && causeMessage.contains("JSON parse error")) {
                message = "JSON 格式错误，请检查请求体格式";
            }
        }
        log.warn("请求数据解析失败: {}", e.getMessage());
        return Result.err(ResultCode.BAD_REQUEST.code(), message);
    }

    /**
     * 文件上传大小超限异常处理
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Result<Void> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e) {
        long maxSize = e.getMaxUploadSize();
        String maxSizeStr = formatFileSize(maxSize);
        String message = String.format("文件大小超过限制，最大允许: %s", maxSizeStr);
        log.warn("文件上传大小超限: {}", message);
        return Result.err(ResultCode.PAYLOAD_TOO_LARGE.code(), message);
    }

    /**
     * 资源不存在异常处理（404）
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public Result<Void> handleNoResourceFoundException(NoResourceFoundException e) {
        String message = String.format("请求的资源不存在: %s", e.getResourcePath());
        log.warn("资源不存在: {}", message);
        return Result.err(ResultCode.NOT_FOUND.code(), message);
    }

    /**
     * 请求方法不支持异常处理（405）
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public Result<Void> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException e) {
        String supportedMethods = String.join(", ", e.getSupportedHttpMethods().toString());
        String message = String.format("不支持的请求方法: %s，支持的方法: %s", e.getMethod(), supportedMethods);
        log.warn("请求方法不支持: {}", message);
        return Result.err(ResultCode.METHOD_NOT_ALLOWED.code(), message);
    }

    /**
     * 媒体类型不支持异常处理（415）
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public Result<Void> handleHttpMediaTypeNotSupportedException(HttpMediaTypeNotSupportedException e) {
        String message = String.format("不支持的媒体类型: %s，支持的类型: %s", 
                e.getContentType(), e.getSupportedMediaTypes());
        log.warn("媒体类型不支持: {}", message);
        return Result.err(ResultCode.UNSUPPORTED_MEDIA_TYPE.code(), message);
    }

    /**
     * 缺少请求参数异常处理
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Result<Void> handleMissingServletRequestParameterException(MissingServletRequestParameterException e) {
        String message = String.format("缺少必填参数: %s (类型: %s)", e.getParameterName(), e.getParameterType());
        log.warn("缺少请求参数: {}", message);
        return Result.err(ResultCode.BAD_REQUEST.code(), message);
    }

    /**
     * 缺少路径变量异常处理
     */
    @ExceptionHandler(MissingPathVariableException.class)
    public Result<Void> handleMissingPathVariableException(MissingPathVariableException e) {
        String message = String.format("缺少路径变量: %s", e.getVariableName());
        log.warn("缺少路径变量: {}", message);
        return Result.err(ResultCode.BAD_REQUEST.code(), message);
    }

    /**
     * 缺少请求部分异常处理（multipart/form-data）
     */
    @ExceptionHandler(MissingServletRequestPartException.class)
    public Result<Void> handleMissingServletRequestPartException(MissingServletRequestPartException e) {
        String message = String.format("缺少请求部分: %s", e.getRequestPartName());
        log.warn("缺少请求部分: {}", message);
        return Result.err(ResultCode.BAD_REQUEST.code(), message);
    }

    /**
     * 非法参数异常处理
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public Result<Void> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("非法参数: {}", e.getMessage());
        return Result.err(ResultCode.BAD_REQUEST.code(), "非法参数: " + e.getMessage());
    }

    /**
     * 空指针异常处理
     */
    @ExceptionHandler(NullPointerException.class)
    public Result<Void> handleNullPointerException(NullPointerException e) {
        log.error("空指针异常", e);
        return Result.err(ResultCode.INTERNAL_SERVER_ERROR.code(), "系统内部错误，请联系管理员");
    }

    /**
     * 其他未捕获的异常处理
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("系统异常: {}", e.getMessage(), e);
        return Result.err(ResultCode.INTERNAL_SERVER_ERROR.code(), "系统异常，请稍后重试");
    }

    /**
     * 格式化文件大小
     */
    private String formatFileSize(long size) {
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format("%.2f KB", size / 1024.0);
        } else if (size < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", size / (1024.0 * 1024));
        } else {
            return String.format("%.2f GB", size / (1024.0 * 1024 * 1024));
        }
    }
}
