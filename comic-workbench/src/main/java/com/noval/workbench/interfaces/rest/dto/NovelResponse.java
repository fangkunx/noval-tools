package com.noval.workbench.interfaces.rest.dto;

import com.noval.workbench.domain.model.Novel;

/**
 * 小说响应 DTO
 */
public class NovelResponse {
    private Long id;
    private String title;
    private String content;
    private String source;
    private boolean vip;
    private String status;

    public static NovelResponse from(Novel novel) {
        NovelResponse resp = new NovelResponse();
        resp.id = novel.getId();
        resp.title = novel.getTitle();
        resp.content = novel.getContent();
        resp.source = novel.getSource();
        resp.vip = novel.isVip();
        resp.status = novel.getStatus();
        return resp;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
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
