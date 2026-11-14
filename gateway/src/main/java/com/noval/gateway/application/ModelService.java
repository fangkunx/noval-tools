package com.noval.gateway.application;

import com.noval.gateway.domain.model.Capability;
import com.noval.gateway.domain.model.ModelSpec;
import com.noval.gateway.domain.spi.GatewayProvider;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Provides access to available models across all providers.
 */
@Service
public class ModelService {

    private final RouterService routerService;

    public ModelService(RouterService routerService) {
        this.routerService = routerService;
    }

    /**
     * Get all available models across all providers.
     */
    public List<ModelSpec> getAllModels() {
        List<ModelSpec> all = new ArrayList<>();
        for (GatewayProvider provider : routerService.getAllProviders()) {
            all.addAll(provider.getSupportedModels());
        }
        return all;
    }

    /**
     * Get models filtered by capability.
     */
    public List<ModelSpec> getModelsByCapability(Capability capability) {
        List<ModelSpec> filtered = new ArrayList<>();
        for (GatewayProvider provider : routerService.getAllProviders()) {
            for (ModelSpec model : provider.getSupportedModels()) {
                if (model.supportsCapability(capability)) {
                    filtered.add(model);
                }
            }
        }
        return filtered;
    }

    /**
     * Check if a model exists and supports a capability.
     */
    public boolean supportsCapability(String modelId, Capability capability) {
        for (GatewayProvider provider : routerService.getAllProviders()) {
            if (provider.supportsModel(modelId)) {
                for (ModelSpec model : provider.getSupportedModels()) {
                    if (model.getId().equals(modelId) && model.supportsCapability(capability)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
