package com.noval.gateway.application;

import com.noval.gateway.domain.model.*;
import com.noval.gateway.domain.spi.GatewayProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Orchestrates image generation requests.
 */
@Service
public class ImageGenService {

    private static final Logger log = LoggerFactory.getLogger(ImageGenService.class);

    private final RouterService routerService;

    public ImageGenService(RouterService routerService) {
        this.routerService = routerService;
    }

    /**
     * Execute an image generation request.
     */
    public ImageGenResult generate(ImageGenCommand command) {
        String modelHint = command.getModel();

        // Resolve provider for the requested model (no fallback)
        RouterService.ProviderResolution resolution;
        try {
            resolution = routerService.resolve(Capability.IMAGE_GEN, modelHint);
        } catch (Exception e) {
            return ImageGenResult.builder().error(e.getMessage()).build();
        }

        // Execute request on the resolved provider
        try {
            log.debug("Image gen with provider: {}, model: {}",
                    resolution.getProvider().getProviderType(), resolution.getModelName());
            return resolution.getProvider().imageGen(command, resolution.getModelName());
        } catch (Exception e) {
            log.error("Provider {} threw exception", resolution.getProvider().getProviderType(), e);
            return ImageGenResult.builder().error(e.getMessage()).build();
        }
    }
}
