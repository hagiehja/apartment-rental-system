package com.example.house.model;

import lombok.Data;
import java.io.Serializable;
import java.util.List;

/**
 * 分页返回结果
 * 实现 Serializable 以支持 Redis 缓存序列化
 */
@Data
public class PageResult<T> implements Serializable {
    private Long total; // 总记录数
    private Integer pageNum; // 当前页码
    private Integer pageSize; // 每页大小
    private List<T> records; // 数据列表

    // Redis JSON 反序列化需要无参构造函数
    public PageResult() {
    }

    public PageResult(Long total, Integer pageNum, Integer pageSize, List<T> records) {
        this.total = total;
        this.pageNum = pageNum;
        this.pageSize = pageSize;
        this.records = records;
    }
}
