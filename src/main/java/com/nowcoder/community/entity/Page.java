package com.nowcoder.community.entity;

public class Page {

    private int current = 1; //current page
    private int limit = 10; //num of data per page (default = 10)
    private int rows; //total rows of data
    private String path;

    public int getCurrent() {
        return current;
    }

    public void setCurrent(int current) {
        if (current >= 1) {
            this.current = current;
        }
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        // 限制传给浏览器的数据量
        if (limit >= 1 & limit <= 100) {
            this.limit = limit;
        }
    }

    public int getRows() {
        return rows;
    }

    public void setRows(int rows) {
        if (rows >= 0) {
            this.rows = rows;
        }
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    // 当前页起始行
    public int getOffset() {
        return (current - 1) * limit;
    }

    // 总页数
    public int getTotal() {
        return (rows + limit - 1) / limit;
    }

    // 页面上显示的 从第n页到第n页， 根据当前页计算
    public int getFrom() {
        int from = current - 2;
        return from < 1 ? 1 : from;
    }

    public int getTo() {
        int to = current + 2;
        int total = getTotal();
        return to > total ? total : to;
    }

}
