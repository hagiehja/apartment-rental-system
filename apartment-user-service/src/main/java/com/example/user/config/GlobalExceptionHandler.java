package com.example.user.config;

import com.example.user.model.Result;
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
 * [VULN-10 修复] 全局异常处理
 * 不向前端暴露内部 path / stack trace / 数据库错误细节
 * 统一返回标准格式 {code, message, data:null}
 *
 * 显式拦截 Spring MVC 的 4xx 异常 (否则 Spring 默认 ResponseEntityExceptionHandler
 * 会返回带 "path"/"trace" 字段的 DefaultErrorAttributes)。
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

    /** 业务非法参数 */
    @ExceptionHandler(IllegalArgumentException.class)
    public Result<Void> handleIllegalArg(IllegalArgumentException e) {
        log.warn("业务参数异常: {}", e.getMessage());
        return Result.error(e.getMessage());
    }

    /**
     * 显式拦截 Spring MVC 标准的 400 异常
     * 覆盖 DefaultHandlerExceptionResolver 默认行为 (否则会回显 path/timestamp)
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

    /** 兜底: 任何未捕获的异常 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleAll(Exception e) {
        // 服务端只记日志, 不把堆栈/路径回给客户端
        log.error("未处理异常", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(body(500, "服务器内部错误, 请稍后重试"));
    }
}