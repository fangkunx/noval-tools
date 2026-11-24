package com.noval.workbench.domain.model;

/**
 * 内容风格分类
 */
public enum ContentStyle {
    ANCIENT("古代"),
    MODERN("现代"),
    SCIFI("科幻"),
    FANTASY("玄幻");

    private final String label;

    ContentStyle(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
