package com.noval.gateway.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.ArrayList;
import java.util.List;

/**
 * OpenAI-compatible chat completion response.
 */
@Schema(description = "聊天补全响应 - 兼容 OpenAI 协议")
public class ChatResponse {

    @Schema(description = "响应唯一标识", example = "chatcmpl-abc123")
    private String id;

    @Schema(description = "对象类型", example = "chat.completion")
    private String object = "chat.completion";

    @Schema(description = "创建时间戳（Unix 秒）", example = "1700000000")
    private long created;

    @Schema(description = "使用的模型", example = "gpt-4o")
    private String model;

    @Schema(description = "响应选项列表")
    private List<Choice> choices = new ArrayList<>();

    @Schema(description = "Token 使用统计")
    private Usage usage;

    @Schema(description = "错误信息（如果有）")
    private String error;

    public static ChatResponse success(String id, String model, String content, String finishReason, Usage usage) {
        ChatResponse resp = new ChatResponse();
        resp.id = id;
        resp.model = model;
        resp.created = System.currentTimeMillis() / 1000;

        Choice choice = new Choice();
        choice.index = 0;
        Message msg = new Message();
        msg.role = "assistant";
        msg.content = content;
        choice.message = msg;
        choice.finish_reason = finishReason;
        resp.choices.add(choice);

        resp.usage = usage;
        return resp;
    }

    public static ChatResponse error(String message) {
        ChatResponse resp = new ChatResponse();
        resp.error = message;
        return resp;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getObject() {
        return object;
    }

    public long getCreated() {
        return created;
    }

    public void setCreated(long created) {
        this.created = created;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public List<Choice> getChoices() {
        return choices;
    }

    public Usage getUsage() {
        return usage;
    }

    public void setUsage(Usage usage) {
        this.usage = usage;
    }

    public String getError() {
        return error;
    }

    @Schema(description = "响应选项")
    public static class Choice {
        @Schema(description = "选项索引", example = "0")
        private int index;

        @Schema(description = "响应消息")
        private Message message;

        @Schema(description = "结束原因：stop, length, content_filter", example = "stop")
        private String finish_reason;

        public int getIndex() {
            return index;
        }

        public Message getMessage() {
            return message;
        }

        public String getFinish_reason() {
            return finish_reason;
        }
    }

    @Schema(description = "消息")
    public static class Message {
        @Schema(description = "角色：assistant", example = "assistant")
        private String role;

        @Schema(description = "回复内容", example = "你好！有什么我可以帮助你的吗？")
        private String content;

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }
    }

    @Schema(description = "Token 使用统计")
    public static class Usage {
        @Schema(description = "提示词 token 数", example = "20")
        private int prompt_tokens;

        @Schema(description = "完成 token 数", example = "15")
        private int completion_tokens;

        @Schema(description = "总 token 数", example = "35")
        private int total_tokens;

        public int getPrompt_tokens() {
            return prompt_tokens;
        }

        public void setPrompt_tokens(int prompt_tokens) {
            this.prompt_tokens = prompt_tokens;
        }

        public int getCompletion_tokens() {
            return completion_tokens;
        }

        public void setCompletion_tokens(int completion_tokens) {
            this.completion_tokens = completion_tokens;
        }

        public int getTotal_tokens() {
            return total_tokens;
        }

        public void setTotal_tokens(int total_tokens) {
            this.total_tokens = total_tokens;
        }

        public static Usage from(com.noval.gateway.domain.model.ChatResult.UsageInfo domain) {
            if (domain == null) return null;
            Usage u = new Usage();
            u.prompt_tokens = domain.getPromptTokens();
            u.completion_tokens = domain.getCompletionTokens();
            u.total_tokens = domain.getTotalTokens();
            return u;
        }
    }
}
