package com.example.common.api;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 分页返回结果(公共版)
 * <p>
 * 实现 Serializable 以支持 Redis 缓存序列化。各服务统一使用此类,
 * 避免每个微服务维护一份拷贝。
 */
@Data
public class PageResult<T> implements Serializable {
    private Long total;
    private Integer pageNum;
    private Integer pageSize;
    private List<T> records;

    public PageResult() {
    }

    public PageResult(Long total, Integer pageNum, Integer pageSize, List<T> records) {
        this.total = total;
        this.pageNum = pageNum;
        this.pageSize = pageSize;
        this.records = records;
    }
}
