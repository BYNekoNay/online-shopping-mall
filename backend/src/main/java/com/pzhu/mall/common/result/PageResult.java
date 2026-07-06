package com.pzhu.mall.common.result;

import java.util.List;

public class PageResult<T> {

    private long total;
    private long pageNum;
    private long pageSize;
    private long pages;
    private List<T> records;

    public PageResult() {}
    public PageResult(long total, long pageNum, long pageSize, long pages, List<T> records) {
        this.total = total;
        this.pageNum = pageNum;
        this.pageSize = pageSize;
        this.pages = pages;
        this.records = records;
    }

    public static <T> PageResult<T> of(com.baomidou.mybatisplus.extension.plugins.pagination.Page<T> page) {
        return new PageResult<>(
                page.getTotal(), page.getCurrent(), page.getSize(), page.getPages(), page.getRecords()
        );
    }

    public long getTotal() { return total; }
    public void setTotal(long total) { this.total = total; }
    public long getPageNum() { return pageNum; }
    public void setPageNum(long pageNum) { this.pageNum = pageNum; }
    public long getPageSize() { return pageSize; }
    public void setPageSize(long pageSize) { this.pageSize = pageSize; }
    public long getPages() { return pages; }
    public void setPages(long pages) { this.pages = pages; }
    public List<T> getRecords() { return records; }
    public void setRecords(List<T> records) { this.records = records; }
}
