package com.snowdrift.framework.web.handler;

import com.snowdrift.framework.common.exception.BizException;
import com.snowdrift.framework.common.result.Result;
import com.snowdrift.framework.common.result.ResultCode;
import com.snowdrift.framework.web.i18n.I18nUtil;
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
        // 尝试使用国际化消息（如果 msg 是国际化 key）
        String message = I18nUtil.getMessage(e.getMessage());
        return Result.err(e.getCode(), message);
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
        // 使用国际化消息
        String i18nMessage = I18nUtil.getMessage("validation.failed", message);
        return Result.err(ResultCode.BAD_REQUEST.code(), i18nMessage);
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
        // 使用国际化消息
        String i18nMessage = I18nUtil.getMessage("validation.bind.failed", message);
        return Result.err(ResultCode.BAD_REQUEST.code(), i18nMessage);
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
        // 使用国际化消息
        String i18nMessage = I18nUtil.getMessage("validation.constraint.violated", message);
        return Result.err(ResultCode.BAD_REQUEST.code(), i18nMessage);
    }

    /**
     * 参数类型不匹配异常处理
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public Result<Void> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e) {
        String paramName = e.getName();
        String requiredType = e.getRequiredType() != null ? e.getRequiredType().getSimpleName() : "unknown";
        Object value = e.getValue();
        log.warn("参数类型不匹配: paramName={}, requiredType={}, value={}", paramName, requiredType, value);
        // 使用国际化消息
        String i18nMessage = I18nUtil.getMessage("validation.type.mismatch", paramName, requiredType, value);
        return Result.err(ResultCode.BAD_REQUEST.code(), i18nMessage);
    }

    /**
     * JSON 解析异常处理
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<Void> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        // 使用国际化消息
        String i18nMessage = I18nUtil.getMessage("validation.json.parse.error");
        log.warn("请求数据解析失败: {}", e.getMessage());
        return Result.err(ResultCode.BAD_REQUEST.code(), i18nMessage);
    }

    /**
     * 文件上传大小超限异常处理
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Result<Void> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e) {
        long maxSize = e.getMaxUploadSize();
        String maxSizeStr = formatFileSize(maxSize);
        // 使用国际化消息
        String message = I18nUtil.getMessage("file.size.exceeded", maxSizeStr);
        log.warn("文件上传大小超限: {}", message);
        return Result.err(ResultCode.PAYLOAD_TOO_LARGE.code(), message);
    }

    /**
     * 将 ResultCode 的 msg 转换为国际化消息
     *
     * @param resultCode 结果码
     * @return 国际化后的消息
     */
    private String getI18nMessage(ResultCode resultCode) {
        return I18nUtil.getMessage(resultCode.msg());
    }

    /**
     * 资源不存在异常处理（404）
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public Result<Void> handleNoResourceFoundException(NoResourceFoundException e) {
        log.warn("资源不存在: {}", e.getResourcePath());
        // 使用国际化消息
        String i18nMessage = I18nUtil.getMessage("common.not.found");
        return Result.err(ResultCode.NOT_FOUND.code(), i18nMessage);
    }

    /**
     * 请求方法不支持异常处理（405）
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public Result<Void> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException e) {
        log.warn("请求方法不支持: {}", e.getMethod());
        // 使用国际化消息
        String i18nMessage = I18nUtil.getMessage("common.method.not.allowed");
        return Result.err(ResultCode.METHOD_NOT_ALLOWED.code(), i18nMessage);
    }

    /**
     * 媒体类型不支持异常处理（415）
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public Result<Void> handleHttpMediaTypeNotSupportedException(HttpMediaTypeNotSupportedException e) {
        log.warn("媒体类型不支持: {}", e.getContentType());
        // 使用国际化消息
        String i18nMessage = I18nUtil.getMessage("common.media.type.not.supported");
        return Result.err(ResultCode.UNSUPPORTED_MEDIA_TYPE.code(), i18nMessage);
    }

    /**
     * 缺少请求参数异常处理
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Result<Void> handleMissingServletRequestParameterException(MissingServletRequestParameterException e) {
        log.warn("缺少请求参数: {}", e.getParameterName());
        // 使用国际化消息
        String i18nMessage = I18nUtil.getMessage("validation.parameter.missing", e.getParameterName());
        return Result.err(ResultCode.BAD_REQUEST.code(), i18nMessage);
    }

    /**
     * 缺少路径变量异常处理
     */
    @ExceptionHandler(MissingPathVariableException.class)
    public Result<Void> handleMissingPathVariableException(MissingPathVariableException e) {
        log.warn("缺少路径变量: {}", e.getVariableName());
        // 使用国际化消息
        String i18nMessage = I18nUtil.getMessage("validation.path.variable.missing", e.getVariableName());
        return Result.err(ResultCode.BAD_REQUEST.code(), i18nMessage);
    }

    /**
     * 缺少请求部分异常处理（multipart/form-data）
     */
    @ExceptionHandler(MissingServletRequestPartException.class)
    public Result<Void> handleMissingServletRequestPartException(MissingServletRequestPartException e) {
        log.warn("缺少请求部分: {}", e.getRequestPartName());
        // 使用国际化消息
        String i18nMessage = I18nUtil.getMessage("validation.request.part.missing", e.getRequestPartName());
        return Result.err(ResultCode.BAD_REQUEST.code(), i18nMessage);
    }

    /**
     * 非法参数异常处理
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public Result<Void> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("非法参数: {}", e.getMessage());
        // 使用国际化消息
        String i18nMessage = I18nUtil.getMessage("common.illegal.argument", e.getMessage());
        return Result.err(ResultCode.BAD_REQUEST.code(), i18nMessage);
    }

    /**
     * 空指针异常处理
     */
    @ExceptionHandler(NullPointerException.class)
    public Result<Void> handleNullPointerException(NullPointerException e) {
        log.error("空指针异常", e);
        // 使用国际化消息
        String i18nMessage = I18nUtil.getMessage("common.internal.error");
        return Result.err(ResultCode.INTERNAL_SERVER_ERROR.code(), i18nMessage);
    }

    /**
     * 其他未捕获的异常处理
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("系统异常: {}", e.getMessage(), e);
        // 使用国际化消息
        String i18nMessage = I18nUtil.getMessage("common.error");
        return Result.err(ResultCode.INTERNAL_SERVER_ERROR.code(), i18nMessage);
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
