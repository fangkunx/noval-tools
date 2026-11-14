package com.noval.gateway.interfaces.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.noval.gateway.interfaces.rest.dto.ImageGenRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for Image endpoint.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ImageControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String API_KEY = "gw-default-key-12345";

    @Test
    void generateImages_withoutAuth_returns401() throws Exception {
        ImageGenRequest request = createImageGenRequest();

        mockMvc.perform(post("/v1/images/generations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void generateImages_withValidApiKey_returns200() throws Exception {
        ImageGenRequest request = createImageGenRequest();

        mockMvc.perform(post("/v1/images/generations")
                        .header("Authorization", "Bearer " + API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void generateImages_withInvalidPrompt_returnsError() throws Exception {
        ImageGenRequest request = new ImageGenRequest();
        request.setModel("dall-e-3");
        request.setPrompt(""); // Empty prompt should fail validation

        mockMvc.perform(post("/v1/images/generations")
                        .header("Authorization", "Bearer " + API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    private ImageGenRequest createImageGenRequest() {
        ImageGenRequest request = new ImageGenRequest();
        request.setModel("dall-e-3");
        request.setPrompt("A cute cat playing with yarn");
        request.setN(1);
        request.setSize("1024x1024");
        request.setResponse_format("url");
        return request;
    }
}
