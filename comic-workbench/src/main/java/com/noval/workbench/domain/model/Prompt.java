package com.noval.workbench.domain.model;

/**
 * 提示词实体
 */
public class Prompt {
    private final Long id;
    private final String title;
    private final String category;
    private final String content;

    private Prompt(Builder builder) {
        this.id = builder.id;
        this.title = builder.title;
        this.category = builder.category;
        this.content = builder.content;
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getCategory() { return category; }
    public String getContent() { return content; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long id;
        private String title;
        private String category;
        private String content;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder title(String title) { this.title = title; return this; }
        public Builder category(String category) { this.category = category; return this; }
        public Builder content(String content) { this.content = content; return this; }
        public Prompt build() { return new Prompt(this); }
    }
}
