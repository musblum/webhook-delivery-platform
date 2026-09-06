package com.musblum.webhookdelivery;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class DeliveryIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:17");

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void getsDeliveryById() throws Exception {

        String endpointResponse =
                mockMvc.perform(post("/api/v1/endpoints")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "url": "https://example.com/webhooks"
                                        }
                                        """))
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        String endpointId = objectMapper
                .readTree(endpointResponse)
                .get("id")
                .asText();

        String eventResponse =
                mockMvc.perform(post("/api/v1/events")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "endpointId": "%s",
                                          "eventType": "order.paid",
                                          "payload": {
                                            "orderId": 123
                                          }
                                        }
                                        """.formatted(endpointId)))
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        String eventId = objectMapper
                .readTree(eventResponse)
                .get("id")
                .asText();

        UUID deliveryId = jdbcTemplate.queryForObject(
                """
                SELECT id
                FROM deliveries
                WHERE event_id = ?
                  AND endpoint_id = ?
                """,
                UUID.class,
                UUID.fromString(eventId),
                UUID.fromString(endpointId)
        );

        mockMvc.perform(get("/api/v1/deliveries/" + deliveryId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(deliveryId.toString()))
                .andExpect(jsonPath("$.eventId").value(eventId))
                .andExpect(jsonPath("$.endpointId").value(endpointId))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());
    }


    @Test
    void returns404WhenDeliveryDoesNotExist() throws Exception {
        UUID missingDeliveryId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/deliveries/" + missingDeliveryId))
                .andExpect(status().isNotFound());
    }
}