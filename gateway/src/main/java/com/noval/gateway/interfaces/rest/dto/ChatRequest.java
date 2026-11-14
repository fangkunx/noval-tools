package com.noval.gateway.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.ArrayList;
import java.util.List;

/**
 * OpenAI-compatible chat completion request.
 */
@Schema(description = "聊天补全请求 - 兼容 OpenAI 协议")
public class ChatRequest {

    @Schema(description = "模型 ID，例如 gpt-4o, claude-sonnet-5-5, gemini-2.0-flash", example = "gpt-4o")
    private String model;

    @Schema(description = "消息列表")
    private List<Message> messages = new ArrayList<>();

    @Schema(description = "是否使用流式响应", example = "false")
    private boolean stream = false;

    @Schema(description = "温度参数，控制随机性（0-2）", example = "0.7")
    private Double temperature;

    @Schema(description = "最大生成 token 数", example = "4096")
    private Integer max_tokens;

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public List<Message> getMessages() {
        return messages;
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages;
    }

    public boolean isStream() {
        return stream;
    }

    public void setStream(boolean stream) {
        this.stream = stream;
    }

    public Double getTemperature() {
        return temperature;
    }

    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }

    public Integer getMax_tokens() {
        return max_tokens;
    }

    public void setMax_tokens(Integer max_tokens) {
        this.max_tokens = max_tokens;
    }

    @Schema(description = "单条消息")
    public static class Message {
        @Schema(description = "角色：system, user, assistant", example = "user")
        private String role;

        @Schema(description = "消息内容，可以是字符串或内容片段数组（用于视觉）")
        private Object content; // Can be String or List<ContentPart>

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public Object getContent() {
            return content;
        }

        public void setContent(Object content) {
            this.content = content;
        }

        public String getTextContent() {
            if (content instanceof String) {
                return (String) content;
            } else if (content instanceof List) {
                StringBuilder sb = new StringBuilder();
                for (Object part : (List<?>) content) {
                    if (part instanceof ContentPart) {
                        ContentPart cp = (ContentPart) part;
                        if ("text".equals(cp.getType()) && cp.getText() != null) {
                            if (sb.length() > 0) sb.append("\n");
                            sb.append(cp.getText());
                        }
                    }
                }
                return sb.toString();
            }
            return "";
        }

        public boolean hasImageContent() {
            if (content instanceof List) {
                for (Object part : (List<?>) content) {
                    if (part instanceof ContentPart) {
                        ContentPart cp = (ContentPart) part;
                        if ("image_url".equals(cp.getType())) {
                            return true;
                        }
                    }
                }
            }
            return false;
        }
    }

    @Schema(description = "内容片段，用于构建多模态消息")
    public static class ContentPart {
        @Schema(description = "类型：text 或 image_url", example = "text")
        private String type;

        @Schema(description = "文本内容", example = "描述这张图片")
        private String text;

        @Schema(description = "图片 URL（当 type 为 image_url 时）")
        private ImageUrl image_url;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        public ImageUrl getImage_url() {
            return image_url;
        }

        public void setImage_url(ImageUrl image_url) {
            this.image_url = image_url;
        }

        public static class ImageUrl {
            private String url;

            public String getUrl() {
                return url;
            }

            public void setUrl(String url) {
                this.url = url;
            }
        }
    }
}
