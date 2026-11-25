package com.noval.workbench.interfaces.rest.dto;

import com.noval.workbench.domain.model.Novel;

/**
 * 小说请求 DTO
 */
public class NovelRequest {
    private String title;
    private String content;
    private String source;
    /** 包装类型：null 表示请求未提供该字段（用于局部更新时保留原值） */
    private Boolean vip;
    private String status;

    public Novel toDomain() {
        return Novel.builder()
                .title(title)
                .content(content)
                .source(source)
                .vip(vip != null && vip)
                .status(status)
                .build();
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public Boolean getVip() { return vip; }
    public void setVip(Boolean vip) { this.vip = vip; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
