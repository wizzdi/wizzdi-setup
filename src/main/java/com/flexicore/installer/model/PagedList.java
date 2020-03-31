package com.flexicore.installer.model;

import java.util.ArrayList;

public class PagedList<T> extends ArrayList<T> {
    int pageSize=50;
    boolean showSearch=true;
    String title;

    public int getPageSize() {
        return pageSize;
    }

    public PagedList<T> setPageSize(int pageSize) {
        this.pageSize = pageSize;
        return this;
    }

    public boolean isShowSearch() {
        return showSearch;
    }

    public PagedList<T> setShowSearch(boolean showSearch) {
        this.showSearch = showSearch;
        return this;
    }

    public String getTitle() {
        return title;
    }

    public PagedList<T> setTitle(String title) {
        this.title = title;
        return this;
    }
}
