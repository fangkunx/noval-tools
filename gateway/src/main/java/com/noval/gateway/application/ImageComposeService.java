package com.noval.gateway.application;

import com.noval.gateway.domain.model.*;
import com.noval.gateway.domain.spi.GatewayProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Orchestrates image composition requests.
 * Receives public URLs and delegates to the provider.
 */
@Service
public class ImageComposeService {

    private static final Logger log = LoggerFactory.getLogger(ImageComposeService.class);

    private final RouterService routerService;

    public ImageComposeService(RouterService routerService) {
        this.routerService = routerService;
    }

    /**
     * Execute an image composition request.
     * Images should already be uploaded to OSS with public URLs.
     */
    public ImageGenResult compose(ImageComposeCommand command) {
        String modelHint = command.getModel();

        log.info("Image compose: model={}, imageCount={}, aspectRatio={}, resolution={}",
                command.getModel(),
                command.getImageUrls().size(),
                command.getAspectRatio(),
                command.getResolution());

        // Resolve provider
        RouterService.ProviderResolution resolution;
        try {
            resolution = routerService.resolve(Capability.IMAGE_COMPOSE, modelHint);
        } catch (Exception e) {
            return ImageGenResult.builder().error(e.getMessage()).build();
        }

        // Execute request on the resolved provider
        try {
            log.debug("Image compose with provider: {}, model: {}, imageCount: {}",
                    resolution.getProvider().getProviderType(), resolution.getModelName(),
                    command.getImageUrls().size());
            return resolution.getProvider().imageCompose(command, resolution.getModelName());
        } catch (Exception e) {
            log.error("Provider {} threw exception", resolution.getProvider().getProviderType(), e);
            return ImageGenResult.builder().error(e.getMessage()).build();
        }
    }
}
