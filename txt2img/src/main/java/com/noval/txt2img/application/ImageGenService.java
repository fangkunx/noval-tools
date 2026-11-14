package com.noval.txt2img.application;

import com.noval.txt2img.domain.model.*;
import com.noval.txt2img.domain.spi.ImageProvider;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Application service that orchestrates image generation across multiple backends.
 * Resolves the correct ImageProvider and delegates calls.
 */
@Service
public class ImageGenService {

    private final List<ImageProvider> providers;

    /**
     * Spring auto-discovers all ImageProvider beans.
     */
    public ImageGenService(List<ImageProvider> providers) {
        this.providers = providers;
    }

    /**
     * Submit an image generation request to the appropriate backend.
     */
    public ImageGenResult generate(ImageGenCommand command, BackendType backend) {
        ImageProvider provider = resolveProvider(backend);
        return provider.generate(command);
    }

    /**
     * Poll an async task from the appropriate backend.
     */
    public ImageGenResult poll(String taskId, BackendType backend, boolean wait) {
        ImageProvider provider = resolveProvider(backend);
        long timeout = wait ? 120 : 0;
        return provider.poll(taskId, timeout);
    }

    /**
     * Get all supported models across all backends, optionally filtered by backend.
     */
    public List<ModelSpec> getSupportedModels(BackendType backend) {
        List<ModelSpec> all = new ArrayList<>();
        for (ImageProvider provider : providers) {
            if (backend == null || provider.getBackend() == backend) {
                all.addAll(provider.getSupportedModels());
            }
        }
        all.sort(Comparator.comparing(ModelSpec::getName));
        return all;
    }

    private ImageProvider resolveProvider(BackendType backend) {
        if (backend == null) {
            backend = BackendType.LK888;
        }
        for (ImageProvider provider : providers) {
            if (provider.getBackend() == backend) {
                return provider;
            }
        }
        throw new IllegalArgumentException("No ImageProvider found for backend: " + backend);
    }
}