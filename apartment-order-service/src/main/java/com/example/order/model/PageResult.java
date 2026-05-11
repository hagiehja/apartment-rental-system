package com.example.order.model;

import lombok.Data;
import java.util.List;

/**
 * 分页结果
 */
@Data
public class PageResult<T> {
    private Long total; // 总记录数
    private Integer pageNum; // 当前页码
    private Integer pageSize; // 每页大小
    private List<T> records; // 数据列表

    public PageResult(Long total, Integer pageNum, Integer pageSize, List<T> records) {
        this.total = total;
        this.pageNum = pageNum;
        this.pageSize = pageSize;
        this.records = records;
    }

    /**
     * 便捷构造函数（只传总数和记录列表）
     */
    public PageResult(Long total, List<T> records) {
        this.total = total;
        this.records = records;
    }
}
