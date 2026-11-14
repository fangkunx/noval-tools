package com.noval.gateway.interfaces.rest.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * OpenAI-compatible chat completion response.
 */
public class ChatResponse {

    private String id;
    private String object = "chat.completion";
    private long created;
    private String model;
    private List<Choice> choices = new ArrayList<>();
    private Usage usage;
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

    public static class Choice {
        private int index;
        private Message message;
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

    public static class Message {
        private String role;
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

    public static class Usage {
        private int prompt_tokens;
        private int completion_tokens;
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
