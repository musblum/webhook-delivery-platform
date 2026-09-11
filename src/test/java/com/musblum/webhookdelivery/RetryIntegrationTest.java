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
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RetryIntegrationTest extends IntegrationTestBase {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    JdbcTemplate jdbcTemplate;

    HttpServer httpServer;

    AtomicInteger requestCount;
    CountDownLatch twoRequestsReceived;

    int serverPort;

    int firstResponseStatus;
    int laterResponseStatus;

    @BeforeEach
    void startWebhookReceiver() throws Exception {
        requestCount = new AtomicInteger(0);
        twoRequestsReceived = new CountDownLatch(2);

        firstResponseStatus = 503;
        laterResponseStatus = 200;

        httpServer = HttpServer.create(
                new InetSocketAddress(0),
                0
        );

        httpServer.createContext("/webhook", exchange -> {
            int attempt = requestCount.incrementAndGet();

            int responseStatus =
                    attempt == 1
                            ? firstResponseStatus
                            : laterResponseStatus;

            exchange.sendResponseHeaders(responseStatus, -1);
            exchange.close();

            twoRequestsReceived.countDown();
        });

        httpServer.start();
        serverPort = httpServer.getAddress().getPort();
    }

    @AfterEach
    void stopWebhookReceiver() {
        httpServer.stop(0);
    }

    @Test
    void retriesFailedWebhookAndEventuallySucceeds() throws Exception {

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

        UUID deliveryId = UUID.fromString(
                objectMapper
                        .readTree(eventResponse)
                        .get("deliveryId")
                        .asText()
        );

        // Wait until the first failed attempt is recorded.
        long retryDeadline = System.currentTimeMillis() + 5000;

        Integer attemptCount = null;
        Boolean retryScheduled = false;

        while (System.currentTimeMillis() < retryDeadline) {
            attemptCount = jdbcTemplate.queryForObject(
                    """
                    SELECT attempt_count
                    FROM deliveries
                    WHERE id = ?
                    """,
                    Integer.class,
                    deliveryId
            );

            retryScheduled = jdbcTemplate.queryForObject(
                    """
                    SELECT next_attempt_at IS NOT NULL
                    FROM deliveries
                    WHERE id = ?
                    """,
                    Boolean.class,
                    deliveryId
            );

            if (attemptCount == 1
                    && Boolean.TRUE.equals(retryScheduled)) {
                break;
            }

            Thread.sleep(100);
        }

        assertEquals(1, attemptCount);
        assertTrue(retryScheduled);

        // First request returned 503.
        // Wait for the retry, which should return 200.
        assertTrue(
                twoRequestsReceived.await(12, TimeUnit.SECONDS),
                "Worker never performed the retry"
        );

        String deliveryStatus = null;

        long successDeadline =
                System.currentTimeMillis() + 5000;

        while (System.currentTimeMillis() < successDeadline) {
            deliveryStatus = jdbcTemplate.queryForObject(
                    """
                    SELECT status
                    FROM deliveries
                    WHERE id = ?
                    """,
                    String.class,
                    deliveryId
            );

            if ("SUCCEEDED".equals(deliveryStatus)) {
                break;
            }

            Thread.sleep(100);
        }

        Integer finalAttemptCount =
                jdbcTemplate.queryForObject(
                        """
                        SELECT attempt_count
                        FROM deliveries
                        WHERE id = ?
                        """,
                        Integer.class,
                        deliveryId
                );

        assertEquals("SUCCEEDED", deliveryStatus);
        assertEquals(2, finalAttemptCount);
        assertEquals(2, requestCount.get());
    }

    @Test
    void nonRetryable404FailsImmediately() throws Exception {

        firstResponseStatus = 404;
        laterResponseStatus = 404;

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

        String eventResponse =
                mockMvc.perform(post("/api/v1/events")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "endpointId": "%s",
                                          "eventType": "order.paid",
                                          "payload": {
                                            "orderId": 456
                                          }
                                        }
                                        """.formatted(endpointId)))
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        UUID deliveryId = UUID.fromString(
                objectMapper
                        .readTree(eventResponse)
                        .get("deliveryId")
                        .asText()
        );

        String deliveryStatus = null;
        Integer attemptCount = null;
        Boolean retryScheduled = null;

        long deadline =
                System.currentTimeMillis() + 5000;

        while (System.currentTimeMillis() < deadline) {

            deliveryStatus = jdbcTemplate.queryForObject(
                    """
                    SELECT status
                    FROM deliveries
                    WHERE id = ?
                    """,
                    String.class,
                    deliveryId
            );

            attemptCount = jdbcTemplate.queryForObject(
                    """
                    SELECT attempt_count
                    FROM deliveries
                    WHERE id = ?
                    """,
                    Integer.class,
                    deliveryId
            );

            retryScheduled = jdbcTemplate.queryForObject(
                    """
                    SELECT next_attempt_at IS NOT NULL
                    FROM deliveries
                    WHERE id = ?
                    """,
                    Boolean.class,
                    deliveryId
            );

            if ("FAILED".equals(deliveryStatus)
                    && attemptCount == 1
                    && Boolean.FALSE.equals(retryScheduled)) {
                break;
            }

            Thread.sleep(100);
        }

        String lastError =
                jdbcTemplate.queryForObject(
                        """
                        SELECT last_error
                        FROM deliveries
                        WHERE id = ?
                        """,
                        String.class,
                        deliveryId
                );

        assertEquals("FAILED", deliveryStatus);
        assertEquals(1, attemptCount);
        assertFalse(retryScheduled);
        assertNotNull(lastError);

        // Give the scheduler another chance to run.
        // A non-retryable 404 should never create another attempt.
        Thread.sleep(1500);

        assertEquals(1, requestCount.get());
    }

    @Test
    void replaysFailedDeliveryAndEventuallySucceeds() throws Exception {

        // First delivery attempt fails permanently.
        // The replayed attempt will succeed.
        firstResponseStatus = 404;
        laterResponseStatus = 200;

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

        String eventResponse =
                mockMvc.perform(post("/api/v1/events")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                    {
                                      "endpointId": "%s",
                                      "eventType": "order.paid",
                                      "payload": {
                                        "orderId": 789
                                      }
                                    }
                                    """.formatted(endpointId)))
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        UUID deliveryId = UUID.fromString(
                objectMapper
                        .readTree(eventResponse)
                        .get("deliveryId")
                        .asText()
        );

        // Wait for the original 404 attempt to become FAILED.
        String deliveryStatus = null;
        long failedDeadline = System.currentTimeMillis() + 5000;

        while (System.currentTimeMillis() < failedDeadline) {
            deliveryStatus = jdbcTemplate.queryForObject(
                    """
                    SELECT status
                    FROM deliveries
                    WHERE id = ?
                    """,
                    String.class,
                    deliveryId
            );

            if ("FAILED".equals(deliveryStatus)) {
                break;
            }

            Thread.sleep(100);
        }

        assertEquals("FAILED", deliveryStatus);
        assertEquals(1, requestCount.get());


        // Manually put the FAILED delivery back into the pipeline.
        mockMvc.perform(
                        post("/api/v1/deliveries/{id}/replay", deliveryId)
                )
                .andExpect(status().isOk());

        // The second HTTP request returns 200.
        long successDeadline = System.currentTimeMillis() + 5000;

        while (System.currentTimeMillis() < successDeadline) {
            deliveryStatus = jdbcTemplate.queryForObject(
                    """
                    SELECT status
                    FROM deliveries
                    WHERE id = ?
                    """,
                    String.class,
                    deliveryId
            );

            if ("SUCCEEDED".equals(deliveryStatus)) {
                break;
            }

            Thread.sleep(100);
        }

        Integer attemptCount = jdbcTemplate.queryForObject(
                """
                SELECT attempt_count
                FROM deliveries
                WHERE id = ?
                """,
                Integer.class,
                deliveryId
        );

        assertEquals("SUCCEEDED", deliveryStatus);
        assertEquals(1, attemptCount);
        assertEquals(2, requestCount.get());
    }
}