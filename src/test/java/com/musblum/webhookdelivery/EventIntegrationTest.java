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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class EventIntegrationTest {

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
    void createsEventAndPendingDelivery() throws Exception {

        // First create an endpoint that the event can be delivered to.
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

        // Now submit an event using that endpoint.
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
                        .andExpect(jsonPath("$.eventId").exists())
                        .andExpect(jsonPath("$.deliveryId").exists())
                        .andExpect(jsonPath("$.status").value("PENDING"))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        String eventId = objectMapper
                .readTree(eventResponse)
                .get("eventId")
                .asText();

        // Verify that submitting the event also created a PENDING delivery.
        Integer pendingDeliveryCount = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM deliveries
                WHERE event_id = ?
                  AND endpoint_id = ?
                  AND status = 'PENDING'
                """,
                Integer.class,
                UUID.fromString(eventId),
                UUID.fromString(endpointId)
        );

        assertEquals(1, pendingDeliveryCount);
    }

    @Test
    void getsEventById() throws Exception {

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
                .get("eventId")
                .asText();

        mockMvc.perform(get("/api/v1/events/" + eventId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(eventId))
                .andExpect(jsonPath("$.eventType").value("order.paid"))
                .andExpect(jsonPath("$.payload.orderId").value(123))
                .andExpect(jsonPath("$.createdAt").exists());
    }


    @Test
    void returns404WhenEventDoesNotExist() throws Exception {
        UUID missingEventId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/events/" + missingEventId))
                .andExpect(status().isNotFound());
    }

    @Test
    void returns404WhenCreatingEventForMissingEndpoint() throws Exception {
        UUID missingEndpointId = UUID.randomUUID();

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
                            """.formatted(missingEndpointId)))
                .andExpect(status().isNotFound());
    }
}