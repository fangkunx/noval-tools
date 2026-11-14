package com.noval.gateway.infrastructure.persistence;

import com.noval.gateway.infrastructure.persistence.entity.ApiKeyEntity;
import com.noval.gateway.infrastructure.persistence.entity.CapabilityRouteEntity;
import com.noval.gateway.infrastructure.persistence.repository.ApiKeyRepository;
import com.noval.gateway.infrastructure.persistence.repository.CapabilityRouteRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Initialize default data for development.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private final ApiKeyRepository apiKeyRepository;
    private final CapabilityRouteRepository routeRepository;

    public DataInitializer(ApiKeyRepository apiKeyRepository, CapabilityRouteRepository routeRepository) {
        this.apiKeyRepository = apiKeyRepository;
        this.routeRepository = routeRepository;
    }

    @Override
    public void run(String... args) {
        // Only initialize if empty
        if (apiKeyRepository.count() > 0) {
            return;
        }

        // Create default API key
        ApiKeyEntity defaultKey = new ApiKeyEntity();
        defaultKey.setApiKey("gw-default-key-12345");
        defaultKey.setName("Default");
        defaultKey.setEnabled(true);
        apiKeyRepository.save(defaultKey);

        // Create default capability routes
        // CHAT: DeepSeek 为主力（100%），其他备用
        createRoute("CHAT", "DEEPSEEK", "deepseek-chat", 100, 1);

        // VISION: DeepSeek 为主力
        createRoute("VISION", "DEEPSEEK", "deepseek-chat", 100, 1);

        // IMAGE_GEN: LK888 和 OpenAI 各 50%
        createRoute("IMAGE_GEN", "LK888", "tt-image-2.5", 50, 1);
        createRoute("IMAGE_GEN", "OPENAI", "dall-e-3", 50, 2);
    }

    private void createRoute(String capability, String provider, String model, int weight, int priority) {
        CapabilityRouteEntity route = new CapabilityRouteEntity();
        route.setCapability(capability);
        route.setProviderType(provider);
        route.setModelName(model);
        route.setWeight(weight);
        route.setPriority(priority);
        route.setEnabled(true);
        routeRepository.save(route);
    }
}
