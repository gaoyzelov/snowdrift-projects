package com.snowdrift.framework.web.handler;

import com.snowdrift.framework.base.exception.BizException;
import com.snowdrift.framework.base.result.Result;
import com.snowdrift.framework.base.result.ResultCode;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpMethod;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * WebExceptionHandlerTest
 *
 * @author gaoyzelov
 * @date 2026/10/10
 * @description 全局异常处理黑盒测试：逐一验证各 @ExceptionHandler 返回的 Result.code 与 msg（业务码透传、校验类归一为
 * BAD_REQUEST、资源/方法/媒体类映射对应码、兜底 INTERNAL_SERVER_ERROR），含多字段拼接与空校验结果默认文案
 * @since 1.0.0
 */
class WebExceptionHandlerTest {

    private final WebExceptionHandler handler = new WebExceptionHandler();
    private final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");

    @Test
    @DisplayName("BizException：透传业务码与消息")
    void bizException() {
        Result<Void> result = handler.handleBizException(new BizException(1234, "自定义业务错误"), request);

        assertThat(result.getCode()).isEqualTo(1234);
        assertThat(result.getMsg()).isEqualTo("自定义业务错误");
    }

    @Test
    @DisplayName("BizException(仅消息)：默认错误码 ERR(0)")
    void bizExceptionDefaultCode() {
        Result<Void> result = handler.handleBizException(new BizException("plain"), request);

        assertThat(result.getCode()).isEqualTo(ResultCode.ERR.code());
        assertThat(result.getMsg()).isEqualTo("plain");
    }

    @Test
    @DisplayName("MethodArgumentNotValidException：多字段消息以逗号拼接，码 BAD_REQUEST")
    @SuppressWarnings("unchecked")
    void methodArgumentNotValid() {
        BindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "dto");
        bindingResult.addError(new FieldError("dto", "name", "不能为空"));
        bindingResult.addError(new FieldError("dto", "age", "必须为正数"));
        MethodArgumentNotValidException e = mock(MethodArgumentNotValidException.class);
        when(e.getBindingResult()).thenReturn(bindingResult);

        Result<Void> result = handler.handleMethodArgumentNotValidException(e, request);

        assertThat(result.getCode()).isEqualTo(ResultCode.BAD_REQUEST.code());
        assertThat(result.getMsg()).contains("不能为空").contains("必须为正数");
    }

    @Test
    @DisplayName("BindException：字段消息拼接，码 BAD_REQUEST")
    void bindException() {
        BindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "dto");
        bindingResult.addError(new FieldError("dto", "f", "绑定失败"));
        BindExceptionHolder holder = new BindExceptionHolder(bindingResult);

        Result<Void> result = handler.handleBindException(holder.exception(), request);

        assertThat(result.getCode()).isEqualTo(ResultCode.BAD_REQUEST.code());
        assertThat(result.getMsg()).isEqualTo("绑定失败");
    }

    /** 用真实 BindException（其构造需 BindingResult） */
    private record BindExceptionHolder(BindingResult bindingResult) {
        org.springframework.validation.BindException exception() {
            return new org.springframework.validation.BindException(bindingResult);
        }
    }

    @Test
    @DisplayName("ConstraintViolationException：违反消息拼接，码 BAD_REQUEST")
    @SuppressWarnings("unchecked")
    void constraintViolation() {
        ConstraintViolation<?> violation = mock(ConstraintViolation.class);
        when(violation.getMessage()).thenReturn("参数非法");
        ConstraintViolationException e = new ConstraintViolationException(Set.of(violation));

        Result<Void> result = handler.handleConstraintViolationException(e, request);

        assertThat(result.getCode()).isEqualTo(ResultCode.BAD_REQUEST.code());
        assertThat(result.getMsg()).isEqualTo("参数非法");
    }

    @Test
    @DisplayName("HandlerMethodValidationException：错误消息归并去重，码 BAD_REQUEST")
    void handlerMethodValidation() {
        MessageSourceResolvable error = mock(MessageSourceResolvable.class);
        when(error.getDefaultMessage()).thenReturn("arg 非法");
        HandlerMethodValidationException e = mock(HandlerMethodValidationException.class);
        when(e.getAllErrors()).thenReturn((List) List.of(error, error));

        Result<Void> result = handler.handleHandlerMethodValidationException(e, request);

        assertThat(result.getCode()).isEqualTo(ResultCode.BAD_REQUEST.code());
        assertThat(result.getMsg()).isEqualTo("arg 非法"); // distinct 去重后仅一条
    }

    @Test
    @DisplayName("HandlerMethodValidationException：无有效消息时用默认文案")
    void handlerMethodValidationEmpty() {
        HandlerMethodValidationException e = mock(HandlerMethodValidationException.class);
        when(e.getAllErrors()).thenReturn(List.of());

        Result<Void> result = handler.handleHandlerMethodValidationException(e, request);

        assertThat(result.getCode()).isEqualTo(ResultCode.BAD_REQUEST.code());
        assertThat(result.getMsg()).isEqualTo("请求参数校验失败，请检查后重试");
    }

    @Test
    @DisplayName("MethodArgumentTypeMismatchException：提示参数名，码 BAD_REQUEST")
    void methodArgumentTypeMismatch() {
        MethodArgumentTypeMismatchException e = mock(MethodArgumentTypeMismatchException.class);
        when(e.getName()).thenReturn("age");
        when(e.getRequiredType()).thenReturn((Class) Integer.class);
        when(e.getValue()).thenReturn("abc");

        Result<Void> result = handler.handleMethodArgumentTypeMismatchException(e, request);

        assertThat(result.getCode()).isEqualTo(ResultCode.BAD_REQUEST.code());
        assertThat(result.getMsg()).isEqualTo("参数类型不匹配：age");
    }

    @Test
    @DisplayName("HttpMessageNotReadableException：固定文案，码 BAD_REQUEST")
    void httpMessageNotReadable() {
        HttpMessageNotReadableException e = mock(HttpMessageNotReadableException.class);

        Result<Void> result = handler.handleHttpMessageNotReadableException(e, request);

        assertThat(result.getCode()).isEqualTo(ResultCode.BAD_REQUEST.code());
        assertThat(result.getMsg()).isEqualTo("请求数据解析失败");
    }

    @Test
    @DisplayName("MaxUploadSizeExceededException：码 PAYLOAD_TOO_LARGE")
    void maxUploadSizeExceeded() {
        Result<Void> result = handler.handleMaxUploadSizeExceededException(
                new MaxUploadSizeExceededException(1024L), request);

        assertThat(result.getCode()).isEqualTo(ResultCode.PAYLOAD_TOO_LARGE.code());
    }

    @Test
    @DisplayName("NoResourceFoundException：码 NOT_FOUND")
    void noResourceFound() {
        Result<Void> result = handler.handleNoResourceFoundException(
                new NoResourceFoundException(HttpMethod.GET, "missing"), request);

        assertThat(result.getCode()).isEqualTo(ResultCode.NOT_FOUND.code());
    }

    @Test
    @DisplayName("HttpRequestMethodNotSupportedException：码 METHOD_NOT_ALLOWED")
    void methodNotSupported() {
        Result<Void> result = handler.handleHttpRequestMethodNotSupportedException(
                new HttpRequestMethodNotSupportedException("POST"), request);

        assertThat(result.getCode()).isEqualTo(ResultCode.METHOD_NOT_ALLOWED.code());
    }

    @Test
    @DisplayName("HttpMediaTypeNotSupportedException：码 UNSUPPORTED_MEDIA_TYPE")
    void mediaTypeNotSupported() {
        Result<Void> result = handler.handleHttpMediaTypeNotSupportedException(
                new HttpMediaTypeNotSupportedException("unsupported"), request);

        assertThat(result.getCode()).isEqualTo(ResultCode.UNSUPPORTED_MEDIA_TYPE.code());
    }

    @Test
    @DisplayName("MissingServletRequestParameterException：提示参数名，码 BAD_REQUEST")
    void missingParam() {
        Result<Void> result = handler.handleMissingServletRequestParameterException(
                new MissingServletRequestParameterException("name", "String"), request);

        assertThat(result.getCode()).isEqualTo(ResultCode.BAD_REQUEST.code());
        assertThat(result.getMsg()).isEqualTo("缺少请求参数：name");
    }

    @Test
    @DisplayName("MissingPathVariableException：提示变量名，码 BAD_REQUEST")
    void missingPathVariable() {
        MissingPathVariableException e = mock(MissingPathVariableException.class);
        when(e.getVariableName()).thenReturn("id");

        Result<Void> result = handler.handleMissingPathVariableException(e, request);

        assertThat(result.getCode()).isEqualTo(ResultCode.BAD_REQUEST.code());
        assertThat(result.getMsg()).isEqualTo("缺少路径变量：id");
    }

    @Test
    @DisplayName("MissingServletRequestPartException：提示部分名，码 BAD_REQUEST")
    void missingPart() {
        Result<Void> result = handler.handleMissingServletRequestPartException(
                new MissingServletRequestPartException("file"), request);

        assertThat(result.getCode()).isEqualTo(ResultCode.BAD_REQUEST.code());
        assertThat(result.getMsg()).isEqualTo("缺少请求部分：file");
    }

    @Test
    @DisplayName("IllegalArgumentException：码 BAD_REQUEST，使用 ResultCode 内置文案（不泄露内部消息）")
    void illegalArgument() {
        Result<Void> result = handler.handleIllegalArgumentException(new IllegalArgumentException("内部细节"), request);

        assertThat(result.getCode()).isEqualTo(ResultCode.BAD_REQUEST.code());
        assertThat(result.getMsg()).isEqualTo(ResultCode.BAD_REQUEST.msg());
    }

    @Test
    @DisplayName("NullPointerException：码 INTERNAL_SERVER_ERROR")
    void nullPointer() {
        Result<Void> result = handler.handleNullPointerException(new NullPointerException("npe"), request);

        assertThat(result.getCode()).isEqualTo(ResultCode.INTERNAL_SERVER_ERROR.code());
    }

    @Test
    @DisplayName("其它 Exception 兜底：码 INTERNAL_SERVER_ERROR")
    void genericException() {
        Result<Void> result = handler.handleException(new RuntimeException("boom"), request);

        assertThat(result.getCode()).isEqualTo(ResultCode.INTERNAL_SERVER_ERROR.code());
        assertThat(result.getMsg()).isEqualTo(ResultCode.INTERNAL_SERVER_ERROR.msg());
    }
}
