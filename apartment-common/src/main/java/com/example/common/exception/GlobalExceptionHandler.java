package com.example.common.exception;

import com.example.common.api.Result;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.HashMap;
import java.util.Map;

/**
 * 全局异常处理([VULN-10 修复] 安全基线)
 * <p>
 * 设计目标:
 * <ol>
 *   <li>不向前端暴露内部 path / stack trace / 数据库错误细节</li>
 *   <li>统一返回标准格式 {@code {code, message, data:null}}</li>
 *   <li>显式拦截 Spring MVC 的 4xx 异常,避免默认 ResponseEntityExceptionHandler
 *       回显 path/timestamp 字段</li>
 * </ol>
 * <p>
 * 各微服务直接通过 apartment-common 依赖获得此能力,无需各自复制。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static Map<String, Object> body(int code, String msg) {
        Map<String, Object> m = new HashMap<>();
        m.put("code", code);
        m.put("message", msg);
        m.put("data", null);
        return m;
    }

    /** 业务非法参数:透传 message,便于前端展示 */
    @ExceptionHandler(IllegalArgumentException.class)
    public Result<Void> handleIllegalArg(IllegalArgumentException e) {
        log.warn("业务参数异常: {}", e.getMessage());
        return Result.error(e.getMessage());
    }

    /** 自定义业务异常:透传 code 与 message */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Map<String, Object>> handleBusiness(BusinessException e) {
        log.warn("业务异常: {}", e.getMessage());
        int code = e.getCode() != null ? e.getCode() : 500;
        HttpStatus status = (code >= 400 && code < 600) ? HttpStatus.valueOf(code) : HttpStatus.INTERNAL_SERVER_ERROR;
        return ResponseEntity.status(status).body(body(code, e.getMessage()));
    }

    /**
     * 显式拦截 Spring MVC 标准的 400 异常
     * 覆盖 DefaultHandlerExceptionResolver 默认行为(否则会回显 path/timestamp)
     */
    @ExceptionHandler({
        MethodArgumentTypeMismatchException.class,
        MissingServletRequestParameterException.class,
        HttpMessageNotReadableException.class,
        BindException.class,
        MethodArgumentNotValidException.class,
        ConstraintViolationException.class
    })
    public ResponseEntity<Map<String, Object>> handleBadRequest(Exception e) {
        log.warn("请求参数错误: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body(400, "请求参数错误"));
    }

    /** 兜底:任何未捕获的异常 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleAll(Exception e) {
        // 服务端只记日志, 不把堆栈/路径回给客户端
        log.error("未处理异常", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(body(500, "服务器内部错误, 请稍后重试"));
    }
}
