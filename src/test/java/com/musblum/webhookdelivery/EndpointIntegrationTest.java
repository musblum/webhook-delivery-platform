package com.musblum.webhookdelivery;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class EndpointIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer posdtgre =
            new PostgreSQLContainer("postgres:17");

    @Autowired
    private MockMvc mockMvc;

    @Test
    void createsEndpoint() throws Exception {
        mockMvc.perform(post("/api/v1/endpoints")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "url": "https://example.com/webhooks"
                            }
                            """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.url").value("https://example.com/webhooks"))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());
    }

    @Test
    void rejectsBlankUrl() throws Exception {
        mockMvc.perform(post("/api/v1/endpoints")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "url": ""
                            }
                            """))
                .andExpect(status().isBadRequest());
    }

}