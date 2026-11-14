package com.noval.gateway.application;

import com.noval.gateway.domain.model.Capability;
import com.noval.gateway.domain.model.ProviderType;
import com.noval.gateway.domain.spi.GatewayProvider;
import com.noval.gateway.infrastructure.persistence.entity.CapabilityRouteEntity;
import com.noval.gateway.infrastructure.persistence.repository.CapabilityRouteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Routes requests to the appropriate provider based on capability and model.
 */
@Service
public class RouterService {

    private static final Logger log = LoggerFactory.getLogger(RouterService.class);

    private final List<GatewayProvider> providers;
    private final CapabilityRouteRepository routeRepository;

    // In-memory cache of routes (refresh periodically if needed)
    private volatile Map<Capability, List<CapabilityRouteEntity>> routesCache;
    private volatile long lastCacheRefresh = 0;
    private static final long CACHE_TTL_MS = 30000; // 30 seconds

    public RouterService(List<GatewayProvider> providers, CapabilityRouteRepository routeRepository) {
        this.providers = providers;
        this.routeRepository = routeRepository;
    }

    /**
     * Resolve a provider for the given capability and optional model hint.
     * If model is specified, find exact provider for that model (no fallback).
     * If model is not specified, select from configured routes (weighted random).
     */
    public ProviderResolution resolve(Capability capability, String modelHint) {
        // If specific model requested, find provider that supports it
        if (modelHint != null && !modelHint.isEmpty()) {
            for (GatewayProvider provider : providers) {
                if (provider.supportsModel(modelHint)) {
                    return new ProviderResolution(provider, modelHint);
                }
            }
            // No provider found for this model - throw exception, no fallback
            throw new IllegalArgumentException("Model not found: " + modelHint);
        }

        // No model specified - use configured routes
        List<CapabilityRouteEntity> routes = getRoutesForCapability(capability);
        if (routes.isEmpty()) {
            throw new IllegalStateException("No routes configured for capability: " + capability);
        }

        // Weighted random selection from enabled routes
        return selectByWeight(routes, capability);
    }

    /**
     * Get all providers.
     */
    public List<GatewayProvider> getAllProviders() {
        return providers;
    }

    /**
     * Find a specific provider by type.
     */
    public GatewayProvider findProvider(ProviderType type) {
        for (GatewayProvider provider : providers) {
            if (provider.getProviderType() == type) {
                return provider;
            }
        }
        return null;
    }

    // --- Private helpers ---

    private List<CapabilityRouteEntity> getRoutesForCapability(Capability capability) {
        refreshCacheIfNeeded();
        return routesCache.getOrDefault(capability, Collections.emptyList());
    }

    private void refreshCacheIfNeeded() {
        long now = System.currentTimeMillis();
        if (routesCache == null || now - lastCacheRefresh > CACHE_TTL_MS) {
            synchronized (this) {
                if (routesCache == null || now - lastCacheRefresh > CACHE_TTL_MS) {
                    Map<Capability, List<CapabilityRouteEntity>> newCache = new EnumMap<>(Capability.class);
                    for (Capability cap : Capability.values()) {
                        newCache.put(cap, routeRepository.findByCapabilityAndEnabledTrueOrderByPriorityAsc(cap.name()));
                    }
                    routesCache = newCache;
                    lastCacheRefresh = now;
                }
            }
        }
    }

    private ProviderResolution selectByWeight(List<CapabilityRouteEntity> routes, Capability capability) {
        int totalWeight = routes.stream().mapToInt(CapabilityRouteEntity::getWeight).sum();
        if (totalWeight <= 0) {
            throw new IllegalStateException("Total weight is 0 for capability: " + capability);
        }

        int random = ThreadLocalRandom.current().nextInt(totalWeight);
        int cumulative = 0;

        for (CapabilityRouteEntity route : routes) {
            cumulative += route.getWeight();
            if (random < cumulative) {
                GatewayProvider provider = findProvider(ProviderType.from(route.getProviderType()));
                if (provider != null) {
                    return new ProviderResolution(provider, route.getModelName());
                }
            }
        }

        // Fallback to first available
        CapabilityRouteEntity first = routes.get(0);
        GatewayProvider provider = findProvider(ProviderType.from(first.getProviderType()));
        return new ProviderResolution(provider, first.getModelName());
    }

    /**
     * Resolution result containing the provider and model to use.
     */
    public static class ProviderResolution {
        private final GatewayProvider provider;
        private final String modelName;

        public ProviderResolution(GatewayProvider provider, String modelName) {
            this.provider = provider;
            this.modelName = modelName;
        }

        public GatewayProvider getProvider() {
            return provider;
        }

        public String getModelName() {
            return modelName;
        }
    }
}
