package com.noval.gateway.domain.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Immutable image generation result object.
 */
public class ImageGenResult {

    private final String id;
    private final List<String> urls;
    private final List<String> base64Data;
    private final String error;
    private final boolean success;

    private ImageGenResult(Builder builder) {
        this.id = builder.id;
        this.urls = builder.urls != null ? Collections.unmodifiableList(new ArrayList<>(builder.urls)) : Collections.emptyList();
        this.base64Data = builder.base64Data != null ? Collections.unmodifiableList(new ArrayList<>(builder.base64Data)) : Collections.emptyList();
        this.error = builder.error;
        this.success = builder.error == null;
    }

    public String getId() {
        return id;
    }

    public List<String> getUrls() {
        return urls;
    }

    public List<String> getBase64Data() {
        return base64Data;
    }

    public String getError() {
        return error;
    }

    public boolean isSuccess() {
        return success;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String id;
        private List<String> urls;
        private List<String> base64Data;
        private String error;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder urls(List<String> urls) {
            this.urls = urls;
            return this;
        }

        public Builder base64Data(List<String> base64Data) {
            this.base64Data = base64Data;
            return this;
        }

        public Builder error(String error) {
            this.error = error;
            return this;
        }

        public ImageGenResult build() {
            return new ImageGenResult(this);
        }
    }
}
