package com.noval.workbench.interfaces.rest.dto;

import com.noval.workbench.domain.model.Prompt;

/**
 * 提示词响应 DTO
 */
public class PromptResponse {
    private Long id;
    private String title;
    private String category;
    private String content;

    public static PromptResponse from(Prompt prompt) {
        PromptResponse resp = new PromptResponse();
        resp.id = prompt.getId();
        resp.title = prompt.getTitle();
        resp.category = prompt.getCategory();
        resp.content = prompt.getContent();
        return resp;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}
