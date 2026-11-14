package com.noval.gateway.interfaces.rest.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * OpenAI-compatible image generation response.
 */
public class ImageGenResponse {

    private long created;
    private List<ImageData> data = new ArrayList<>();
    private String error;

    public static ImageGenResponse success(String id, List<String> urls, List<String> b64Data) {
        ImageGenResponse resp = new ImageGenResponse();
        resp.created = System.currentTimeMillis() / 1000;

        for (int i = 0; i < urls.size(); i++) {
            ImageData img = new ImageData();
            img.url = urls.get(i);
            resp.data.add(img);
        }
        for (int i = 0; i < b64Data.size(); i++) {
            ImageData img = new ImageData();
            img.b64_json = b64Data.get(i);
            resp.data.add(img);
        }

        return resp;
    }

    public static ImageGenResponse error(String message) {
        ImageGenResponse resp = new ImageGenResponse();
        resp.error = message;
        return resp;
    }

    public long getCreated() {
        return created;
    }

    public List<ImageData> getData() {
        return data;
    }

    public String getError() {
        return error;
    }

    public static class ImageData {
        private String url;
        private String b64_json;
        private String revised_prompt;

        public String getUrl() {
            return url;
        }

        public String getB64_json() {
            return b64_json;
        }

        public String getRevised_prompt() {
            return revised_prompt;
        }
    }
}
