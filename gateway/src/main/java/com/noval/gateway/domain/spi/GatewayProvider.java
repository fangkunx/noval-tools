package com.noval.gateway.domain.spi;

import com.noval.gateway.domain.model.*;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Service Provider Interface for AI gateway backends.
 * Each AI provider (OpenAI, Claude, Google, etc.) implements this interface.
 * Spring auto-discovers implementations via @Component.
 */
public interface GatewayProvider {

    /**
     * Which provider type this implementation handles.
     */
    ProviderType getProviderType();

    /**
     * Models supported by this provider.
     */
    List<ModelSpec> getSupportedModels();

    /**
     * Capabilities supported by this provider (union of all models' capabilities).
     */
    Set<Capability> getSupportedCapabilities();

    /**
     * Check if this provider supports a specific model.
     */
    boolean supportsModel(String modelId);

    /**
     * Submit a chat/vision request (non-streaming).
     */
    ChatResult chat(ChatCommand command, String modelName);

    /**
     * Submit a chat/vision request (streaming).
     * The callback receives ChatStreamChunk for each chunk.
     */
    void chatStream(ChatCommand command, String modelName, Consumer<ChatStreamChunk> onChunk, Consumer<Throwable> onError);

    /**
     * Submit an image generation request.
     */
    ImageGenResult imageGen(ImageGenCommand command, String modelName);

    /**
     * Represents a single chunk in a streaming response.
     */
    class ChatStreamChunk {
        private final String id;
        private final String content;
        private final String finishReason;
        private final ChatResult.UsageInfo usage;

        public ChatStreamChunk(String id, String content, String finishReason, ChatResult.UsageInfo usage) {
            this.id = id;
            this.content = content;
            this.finishReason = finishReason;
            this.usage = usage;
        }

        public String getId() {
            return id;
        }

        public String getContent() {
            return content;
        }

        public String getFinishReason() {
            return finishReason;
        }

        public ChatResult.UsageInfo getUsage() {
            return usage;
        }
    }
}
