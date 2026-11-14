package com.noval.gateway.interfaces.rest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for Models endpoint.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ModelControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private static final String API_KEY = "gw-default-key-12345";

    @Test
    void listModels_withoutAuth_returns401() throws Exception {
        mockMvc.perform(get("/v1/models"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listModels_withValidApiKey_returns200() throws Exception {
        mockMvc.perform(get("/v1/models")
                        .header("Authorization", "Bearer " + API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.object").value("list"))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void listModels_filterByCapability_chat() throws Exception {
        mockMvc.perform(get("/v1/models")
                        .header("Authorization", "Bearer " + API_KEY)
                        .param("capability", "CHAT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void listModels_filterByCapability_vision() throws Exception {
        mockMvc.perform(get("/v1/models")
                        .header("Authorization", "Bearer " + API_KEY)
                        .param("capability", "VISION"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void listModels_filterByCapability_imageGen() throws Exception {
        mockMvc.perform(get("/v1/models")
                        .header("Authorization", "Bearer " + API_KEY)
                        .param("capability", "IMAGE_GEN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void getModel_existingModel_returns200() throws Exception {
        mockMvc.perform(get("/v1/models/gpt-4o")
                        .header("Authorization", "Bearer " + API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("gpt-4o"));
    }

    @Test
    void getModel_nonExistingModel_returns404() throws Exception {
        mockMvc.perform(get("/v1/models/non-existing-model")
                        .header("Authorization", "Bearer " + API_KEY))
                .andExpect(status().isNotFound());
    }
}
