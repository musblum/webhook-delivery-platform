package com.musblum.webhookdelivery;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class WebhookWorkerIntegrationTest extends IntegrationTestBase {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    JdbcTemplate jdbcTemplate;

    HttpServer httpServer;

    AtomicReference<String> receivedBody;
    CountDownLatch requestReceived;

    int serverPort;

    @BeforeEach
    void startWebhookReceiver() throws Exception {
        receivedBody = new AtomicReference<>();
        requestReceived = new CountDownLatch(1);

        httpServer = HttpServer.create(
                new InetSocketAddress(0),
                0
        );

        httpServer.createContext("/webhook", exchange -> {

            String body = new String(
                    exchange.getRequestBody().readAllBytes(),
                    StandardCharsets.UTF_8
            );

            receivedBody.set(body);

            exchange.sendResponseHeaders(200, -1);
            exchange.close();

            requestReceived.countDown();
        });

        httpServer.start();

        serverPort = httpServer.getAddress().getPort();
    }

    @AfterEach
    void stopWebhookReceiver() {
        httpServer.stop(0);
    }

    @Test
    void workerDeliversWebhookAndMarksDeliverySucceeded() throws Exception {

        // Register our temporary local HTTP server as a webhook endpoint.
        String endpointResponse =
                mockMvc.perform(post("/api/v1/endpoints")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "url": "http://localhost:%d/webhook"
                                        }
                                        """.formatted(serverPort)))
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        String endpointId = objectMapper
                .readTree(endpointResponse)
                .get("id")
                .asText();

        // Submit an event.
        String eventResponse =
                mockMvc.perform(post("/api/v1/events")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "endpointId": "%s",
                                          "eventType": "order.paid",
                                          "payload": {
                                            "orderId": 123,
                                            "amount": 59.99
                                          }
                                        }
                                        """.formatted(endpointId)))
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        String deliveryId = objectMapper
                .readTree(eventResponse)
                .get("deliveryId")
                .asText();

        // Wait for the worker to actually call our webhook receiver.
        assertTrue(
                requestReceived.await(5, TimeUnit.SECONDS),
                "Worker never sent the webhook"
        );

        // Wait for the worker to update PostgreSQL.
        String deliveryStatus = null;
        long deadline = System.currentTimeMillis() + 5000;

        while (System.currentTimeMillis() < deadline) {

            deliveryStatus = jdbcTemplate.queryForObject(
                    """
                    SELECT status
                    FROM deliveries
                    WHERE id = ?
                    """,
                    String.class,
                    UUID.fromString(deliveryId)
            );

            if ("SUCCEEDED".equals(deliveryStatus)) {
                break;
            }

            Thread.sleep(100);
        }

        assertEquals("SUCCEEDED", deliveryStatus);

        // Verify the HTTP body the worker actually sent.
        var webhookBody =
                objectMapper.readTree(receivedBody.get());

        assertEquals(
                "order.paid",
                webhookBody.get("eventType").asText()
        );

        assertEquals(
                123,
                webhookBody.get("payload").get("orderId").asInt()
        );

        assertEquals(
                59.99,
                webhookBody.get("payload").get("amount").asDouble()
        );
    }
}
