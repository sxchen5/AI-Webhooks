package com.scanplatform.web;

import com.scanplatform.common.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;

import java.util.stream.Collectors;

/**
 * 将业务异常与校验异常转为统一 JSON 响应。
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> badRequest(IllegalArgumentException e) {
        log.warn("业务校验失败: {}", e.getMessage());
        return ApiResponse.fail(400, e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> valid(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        log.warn("参数校验失败: {}", msg);
        return ApiResponse.fail(400, msg);
    }

    /**
     * AI 问答等流式接口的 Accept 常为 {@code text/event-stream}，若仍用默认 JSON 写出 {@link ApiResponse}
     * 会触发 HttpMediaTypeNotAcceptableException；此处强制 application/json。
     */
    @ExceptionHandler(AsyncRequestTimeoutException.class)
    public ResponseEntity<ApiResponse<Void>> asyncRequestTimeout(AsyncRequestTimeoutException e) {
        log.warn("异步请求超时（常见于流式对话/agent 执行时间较长）: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT)
                .contentType(MediaType.APPLICATION_JSON)
                .body(ApiResponse.fail(408, "请求处理超时，请缩短问题或稍后重试"));
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> other(Exception e) {
        log.error("未处理异常", e);
        return ApiResponse.fail(500, "服务器内部错误");
    }
}
