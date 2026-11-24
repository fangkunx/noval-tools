package com.noval.workbench.domain.model;

/**
 * 小说实体
 */
public class Novel {
    private final Long id;
    private final String title;
    private final String content;
    private final String source;
    private final boolean vip;
    private final String status;

    private Novel(Builder builder) {
        this.id = builder.id;
        this.title = builder.title;
        this.content = builder.content;
        this.source = builder.source;
        this.vip = builder.vip;
        this.status = builder.status;
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public String getSource() { return source; }
    public boolean isVip() { return vip; }
    public String getStatus() { return status; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long id;
        private String title;
        private String content;
        private String source;
        private boolean vip;
        private String status;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder title(String title) { this.title = title; return this; }
        public Builder content(String content) { this.content = content; return this; }
        public Builder source(String source) { this.source = source; return this; }
        public Builder vip(boolean vip) { this.vip = vip; return this; }
        public Builder status(String status) { this.status = status; return this; }
        public Novel build() { return new Novel(this); }
    }
}
