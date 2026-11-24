package com.noval.workbench.interfaces.rest.dto;

import com.noval.workbench.domain.model.Prompt;

/**
 * 提示词请求 DTO
 */
public class PromptRequest {
    private String title;
    private String category;
    private String content;

    public Prompt toDomain() {
        return Prompt.builder()
                .title(title)
                .category(category)
                .content(content)
                .build();
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}
