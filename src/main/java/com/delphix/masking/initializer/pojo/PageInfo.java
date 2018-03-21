package com.delphix.masking.initializer.pojo;

public class PageInfo {

    private Long numberOnPage;
    private Long total;

    public PageInfo() {
    }

    public Long getNumberOnPage() {
        return numberOnPage;
    }

    public PageInfo setNumberOnPage(Long numberOnPage) {
        this.numberOnPage = numberOnPage;
        return this;
    }

    public Long getTotal() {
        return total;
    }

    public PageInfo setTotal(Long total) {
        this.total = total;
        return this;
    }
}
