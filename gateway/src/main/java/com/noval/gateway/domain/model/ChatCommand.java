package com.noval.gateway.domain.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Immutable chat request command object.
 */
public class ChatCommand {

    private final String model;
    private final List<Message> messages;
    private final boolean stream;
    private final Double temperature;
    private final Integer maxTokens;

    private ChatCommand(Builder builder) {
        this.model = builder.model;
        this.messages = Collections.unmodifiableList(new ArrayList<>(builder.messages));
        this.stream = builder.stream;
        this.temperature = builder.temperature;
        this.maxTokens = builder.maxTokens;
    }

    public String getModel() {
        return model;
    }

    public List<Message> getMessages() {
        return messages;
    }

    public boolean isStream() {
        return stream;
    }

    public Double getTemperature() {
        return temperature;
    }

    public Integer getMaxTokens() {
        return maxTokens;
    }

    /**
     * Determine the capability required for this request.
     */
    public Capability getRequiredCapability() {
        for (Message msg : messages) {
            for (ContentPart part : msg.getContent()) {
                if (part.getType() == ContentPart.Type.IMAGE_URL) {
                    return Capability.VISION;
                }
            }
        }
        return Capability.CHAT;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String model;
        private List<Message> messages = new ArrayList<>();
        private boolean stream = false;
        private Double temperature;
        private Integer maxTokens;

        public Builder model(String model) {
            this.model = model;
            return this;
        }

        public Builder messages(List<Message> messages) {
            this.messages = messages;
            return this;
        }

        public Builder stream(boolean stream) {
            this.stream = stream;
            return this;
        }

        public Builder temperature(Double temperature) {
            this.temperature = temperature;
            return this;
        }

        public Builder maxTokens(Integer maxTokens) {
            this.maxTokens = maxTokens;
            return this;
        }

        public ChatCommand build() {
            if (messages.isEmpty()) {
                throw new IllegalArgumentException("Messages cannot be empty");
            }
            return new ChatCommand(this);
        }
    }

    /**
     * Represents a single message in the conversation.
     */
    public static class Message {
        private final String role;
        private final List<ContentPart> content;

        public Message(String role, List<ContentPart> content) {
            this.role = role;
            this.content = content != null ? Collections.unmodifiableList(content) : Collections.emptyList();
        }

        public static Message ofText(String role, String text) {
            return new Message(role, Collections.singletonList(ContentPart.text(text)));
        }

        public String getRole() {
            return role;
        }

        public List<ContentPart> getContent() {
            return content;
        }

        public String getTextContent() {
            StringBuilder sb = new StringBuilder();
            for (ContentPart part : content) {
                if (part.getType() == ContentPart.Type.TEXT) {
                    if (sb.length() > 0) sb.append("\n");
                    sb.append(part.getText());
                }
            }
            return sb.toString();
        }
    }

    /**
     * Represents a content part (text or image).
     */
    public static class ContentPart {
        private final Type type;
        private final String text;
        private final String imageUrl;

        private ContentPart(Type type, String text, String imageUrl) {
            this.type = type;
            this.text = text;
            this.imageUrl = imageUrl;
        }

        public static ContentPart text(String text) {
            return new ContentPart(Type.TEXT, text, null);
        }

        public static ContentPart imageUrl(String url) {
            return new ContentPart(Type.IMAGE_URL, null, url);
        }

        public Type getType() {
            return type;
        }

        public String getText() {
            return text;
        }

        public String getImageUrl() {
            return imageUrl;
        }

        public enum Type {
            TEXT,
            IMAGE_URL
        }
    }
}
