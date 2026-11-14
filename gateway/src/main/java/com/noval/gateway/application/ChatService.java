package com.noval.gateway.application;

import com.noval.gateway.domain.model.*;
import com.noval.gateway.domain.spi.GatewayProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.function.Consumer;

/**
 * Orchestrates chat/vision requests with automatic fallback.
 */
@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    private final RouterService routerService;

    public ChatService(RouterService routerService) {
        this.routerService = routerService;
    }

    /**
     * Execute a chat/vision request (non-streaming).
     */
    public ChatResult chat(ChatCommand command) {
        Capability capability = command.getRequiredCapability();
        String modelHint = command.getModel();

        // Resolve provider for the requested model (no fallback)
        RouterService.ProviderResolution resolution;
        try {
            resolution = routerService.resolve(capability, modelHint);
        } catch (Exception e) {
            return ChatResult.builder().error(e.getMessage()).build();
        }

        // Execute request on the resolved provider
        try {
            log.debug("Chat with provider: {}, model: {}",
                    resolution.getProvider().getProviderType(), resolution.getModelName());
            return resolution.getProvider().chat(command, resolution.getModelName());
        } catch (Exception e) {
            log.error("Provider {} threw exception", resolution.getProvider().getProviderType(), e);
            return ChatResult.builder().error(e.getMessage()).build();
        }
    }

    /**
     * Execute a chat/vision request (streaming).
     */
    public void chatStream(ChatCommand command, Consumer<GatewayProvider.ChatStreamChunk> onChunk,
                           Consumer<Throwable> onError, Runnable onComplete) {
        Capability capability = command.getRequiredCapability();
        String modelHint = command.getModel();

        // Resolve provider for the requested model (no fallback)
        RouterService.ProviderResolution resolution;
        try {
            resolution = routerService.resolve(capability, modelHint);
        } catch (Exception e) {
            onError.accept(e);
            return;
        }

        // Execute streaming request on the resolved provider
        try {
            log.debug("Stream with provider: {}, model: {}",
                    resolution.getProvider().getProviderType(), resolution.getModelName());

            resolution.getProvider().chatStream(
                    command,
                    resolution.getModelName(),
                    onChunk,
                    onError
            );

            onComplete.run();
        } catch (Exception e) {
            log.error("Provider {} threw exception", resolution.getProvider().getProviderType(), e);
            onError.accept(e);
        }
    }
}
