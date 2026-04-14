package dev.sonarqube.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RecommendationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void defaultRequestUsesV1() throws Exception {
        mockMvc.perform(get("/recommendations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value("v1"));
    }

    @Test
    void headerV2UsesV2Logic() throws Exception {
        mockMvc.perform(get("/recommendations")
                        .header("X-Recommendation-Version", "v2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value("v2"));
    }
}