package com.example.common.exception;

/**
 * 业务异常
 * <p>
 * 用于在 Service 层主动抛出可预期的业务错误,
 * 由 {@link GlobalExceptionHandler} 统一捕获并转换为标准 API 响应。
 * 不应承载系统级/不可预期异常(后者让 Spring 包装为 RuntimeException 即可)。
 */
public class BusinessException extends RuntimeException {

    private final Integer code;

    public BusinessException(String message) {
        super(message);
        this.code = 500;
    }

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
        this.code = 500;
    }

    public Integer getCode() {
        return code;
    }
}
