package com.example.common.api;

import lombok.Data;

/**
 * 统一 API 响应结果
 * <p>
 * 标准结构 {@code {code, message, data}},所有 Controller 统一返回此类型。
 * 避免在多个微服务中各自维护一份拷贝,降低维护成本。
 *
 * @param <T> data 字段的业务类型
 */
@Data
public class Result<T> {

    /** 状态码:200 成功,4xx 客户端错误,5xx 服务端错误,其他业务自定义 */
    private Integer code;

    /** 提示消息(可直接展示给前端) */
    private String message;

    /** 业务数据 */
    private T data;

    /**
     * 成功返回(带数据)
     */
    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMessage("操作成功");
        result.setData(data);
        return result;
    }

    /**
     * 成功返回(无数据)
     */
    public static <T> Result<T> success() {
        return success(null);
    }

    /**
     * 失败返回(默认 500)
     */
    public static <T> Result<T> error(String message) {
        Result<T> result = new Result<>();
        result.setCode(500);
        result.setMessage(message);
        return result;
    }

    /**
     * 失败返回(自定义状态码)
     */
    public static <T> Result<T> error(Integer code, String message) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMessage(message);
        return result;
    }
}
