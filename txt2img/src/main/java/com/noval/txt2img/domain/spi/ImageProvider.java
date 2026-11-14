package com.noval.txt2img.domain.spi;

import com.noval.txt2img.domain.model.BackendType;
import com.noval.txt2img.domain.model.ImageGenCommand;
import com.noval.txt2img.domain.model.ImageGenResult;
import com.noval.txt2img.domain.model.ModelSpec;

import java.util.List;

/**
 * Service Provider Interface for AI image generation backends.
 * Each AI provider (lk888, tokenplan, etc.) implements this interface.
 * Spring auto-discovers implementations via @Component + List<ImageProvider>.
 */
public interface ImageProvider {

    /**
     * Which backend this provider handles.
     */
    BackendType getBackend();

    /**
     * Models supported by this provider.
     */
    List<ModelSpec> getSupportedModels();

    /**
     * Submit a generation request. For async backends, returns a SUBMITTED result with taskId.
     * For sync backends, returns SUCCESS/FAILED directly.
     */
    ImageGenResult generate(ImageGenCommand command);

    /**
     * Poll an async task. Return immediately if supportsAsync() is false.
     *
     * @param taskId         the task id from generate()
     * @param timeoutSeconds max seconds to wait before returning even if not final
     */
    ImageGenResult poll(String taskId, long timeoutSeconds);

    /**
     * Whether this backend supports async polling.
     */
    boolean supportsAsync();
}