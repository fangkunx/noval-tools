package com.noval.workbench.interfaces.rest.dto;

import com.noval.workbench.domain.model.Novel;

/**
 * 小说请求 DTO
 */
public class NovelRequest {
    private String title;
    private String content;
    private String source;
    private boolean vip;
    private String status;

    public Novel toDomain() {
        return Novel.builder()
                .title(title)
                .content(content)
                .source(source)
                .vip(vip)
                .status(status)
                .build();
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public boolean isVip() { return vip; }
    public void setVip(boolean vip) { this.vip = vip; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
