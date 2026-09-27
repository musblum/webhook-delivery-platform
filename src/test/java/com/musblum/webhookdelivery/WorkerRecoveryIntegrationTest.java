package com.musblum.webhookdelivery;

import com.musblum.webhookdelivery.repository.WebhookDeliveryRepository;
import com.musblum.webhookdelivery.service.RetryPolicy;
import com.musblum.webhookdelivery.worker.WebhookWorker;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "app.worker.enabled=false",
        "app.dispatcher.enabled=false"
})
@AutoConfigureMockMvc
class WorkerRecoveryIntegrationTest extends IntegrationTestBase {

    private static final String STREAM = "webhook-deliveries";
    private static final String GROUP = "webhook-workers";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    StringRedisTemplate redisTemplate;

    @Autowired
    WebhookDeliveryRepository deliveryRepository;

    @Autowired
    RestClient.Builder restClientBuilder;

    @Autowired
    RetryPolicy retryPolicy;

    HttpServer httpServer;
    AtomicInteger requestCount;
    AtomicReference<String> lastDeliveryIdHeader;
    WebhookWorker worker;

    int serverPort;

    @BeforeEach
    void setup() throws Exception {

        requestCount = new AtomicInteger();



        httpServer = HttpServer.create(
                new InetSocketAddress(0),
                0
        );

        lastDeliveryIdHeader = new AtomicReference<>();

        httpServer.createContext("/webhook", exchange -> {
            requestCount.incrementAndGet();

            lastDeliveryIdHeader.set(
                    exchange.getRequestHeaders()
                            .getFirst("X-Webhook-Delivery-Id")
            );

            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });

        httpServer.start();
        serverPort = httpServer.getAddress().getPort();

        worker = new WebhookWorker(
                redisTemplate,
                deliveryRepository,
                restClientBuilder,
                retryPolicy
        );

        ReflectionTestUtils.setField(
                worker,
                "staleThreshold",
                Duration.ofSeconds(1)
        );

        redisTemplate.delete(STREAM);
    }

    @AfterEach
    void cleanup() {
        httpServer.stop(0);
        redisTemplate.delete(STREAM);
    }

    @Test
    void recoversStalePendingMessageFromDeadWorker() throws Exception {

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
                                            "orderId": 999
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

        /*
         * Put a message into Valkey manually.
         */
        redisTemplate.opsForStream().add(
                MapRecord.create(
                        STREAM,
                        Map.of(
                                "deliveryId",
                                deliveryId.toString()
                        )
                )
        );

        /*
         * Create the worker consumer group.
         */
        redisTemplate.opsForStream().createGroup(
                STREAM,
                ReadOffset.from("0-0"),
                GROUP
        );

        /*
         * Pretend a worker received the message.
         *
         * We deliberately DO NOT ACK it.
         * That leaves the message pending under "dead-worker".
         */
        var deadWorkerMessages =
                redisTemplate.<String, String>opsForStream().read(
                        Consumer.from(
                                GROUP,
                                "dead-worker"
                        ),
                        StreamReadOptions.empty().count(1),
                        StreamOffset.create(
                                STREAM,
                                ReadOffset.lastConsumed()
                        )
                );

        assertNotNull(deadWorkerMessages);
        assertEquals(1, deadWorkerMessages.size());

        var pendingBeforeRecovery =
                redisTemplate.opsForStream()
                        .pending(STREAM, GROUP);

        assertEquals(
                1,
                pendingBeforeRecovery.getTotalPendingMessages()
        );

        /*
         * Our test threshold is only one second.
         * Let the message become stale.
         */
        Thread.sleep(1200);

        /*
         * This represents another healthy worker
         * detecting and claiming the abandoned message.
         */
        worker.recoverStaleMessages();

        String deliveryStatus =
                jdbcTemplate.queryForObject(
                        """
                        SELECT status
                        FROM deliveries
                        WHERE id = ?
                        """,
                        String.class,
                        deliveryId
                );

        var pendingAfterRecovery =
                redisTemplate.opsForStream()
                        .pending(STREAM, GROUP);

        assertEquals("SUCCEEDED", deliveryStatus);
        assertEquals(1, requestCount.get());
        assertEquals(
                0,
                pendingAfterRecovery.getTotalPendingMessages()
        );
    }

    @Test
    void doesNotRedeliverWebhookWhenRecoveredDeliveryAlreadySucceeded()
            throws Exception {

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
                                        "orderId": 1000
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

        /*
         * Simulate that Worker A already successfully
         * delivered the webhook and saved that result.
         */
        var delivery = deliveryRepository
                .findById(deliveryId)
                .orElseThrow();

        delivery.markSucceeded();
        deliveryRepository.save(delivery);

        /*
         * The worker crashed BEFORE ACKing the Valkey message,
         * so the old message still exists.
         */
        redisTemplate.opsForStream().add(
                MapRecord.create(
                        STREAM,
                        Map.of(
                                "deliveryId",
                                deliveryId.toString()
                        )
                )
        );

        redisTemplate.opsForStream().createGroup(
                STREAM,
                ReadOffset.from("0-0"),
                GROUP
        );

        var deadWorkerMessages =
                redisTemplate.<String, String>opsForStream().read(
                        Consumer.from(
                                GROUP,
                                "dead-worker"
                        ),
                        StreamReadOptions.empty().count(1),
                        StreamOffset.create(
                                STREAM,
                                ReadOffset.lastConsumed()
                        )
                );

        assertNotNull(deadWorkerMessages);
        assertEquals(1, deadWorkerMessages.size());

        var pendingBeforeRecovery =
                redisTemplate.opsForStream()
                        .pending(STREAM, GROUP);

        assertEquals(
                1,
                pendingBeforeRecovery.getTotalPendingMessages()
        );

        /*
         * Let the abandoned message become stale.
         */
        Thread.sleep(1200);

        worker.recoverStaleMessages();

        var pendingAfterRecovery =
                redisTemplate.opsForStream()
                        .pending(STREAM, GROUP);

        /*
         * Most important assertion:
         * recovery should NOT send another HTTP webhook.
         */
        assertEquals(0, requestCount.get());

        assertEquals(
                0,
                pendingAfterRecovery.getTotalPendingMessages()
        );

        String deliveryStatus =
                jdbcTemplate.queryForObject(
                        """
                        SELECT status
                        FROM deliveries
                        WHERE id = ?
                        """,
                        String.class,
                        deliveryId
                );

        assertEquals("SUCCEEDED", deliveryStatus);
    }
    @Test
    void doesNotRedeliverWhenRetryIsAlreadyScheduled()
            throws Exception {

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
                                        "orderId": 1001
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

        /*
         * Simulate M5 already scheduling a retry.
         */
        var delivery = deliveryRepository
                .findById(deliveryId)
                .orElseThrow();

        delivery.scheduleRetry(
                java.time.Instant.now().plusSeconds(60),
                "503 Service Unavailable"
        );

        deliveryRepository.save(delivery);

        /*
         * Old message was never ACKed because the worker crashed.
         */
        redisTemplate.opsForStream().add(
                MapRecord.create(
                        STREAM,
                        Map.of(
                                "deliveryId",
                                deliveryId.toString()
                        )
                )
        );

        redisTemplate.opsForStream().createGroup(
                STREAM,
                ReadOffset.from("0-0"),
                GROUP
        );

        var deadWorkerMessages =
                redisTemplate.<String, String>opsForStream().read(
                        Consumer.from(
                                GROUP,
                                "dead-worker"
                        ),
                        StreamReadOptions.empty().count(1),
                        StreamOffset.create(
                                STREAM,
                                ReadOffset.lastConsumed()
                        )
                );

        assertNotNull(deadWorkerMessages);
        assertEquals(1, deadWorkerMessages.size());

        Thread.sleep(1200);

        worker.recoverStaleMessages();

        var pendingAfterRecovery =
                redisTemplate.opsForStream()
                        .pending(STREAM, GROUP);

        assertEquals(0, requestCount.get());

        assertEquals(
                0,
                pendingAfterRecovery.getTotalPendingMessages()
        );

        Boolean retryStillScheduled =
                jdbcTemplate.queryForObject(
                        """
                        SELECT next_attempt_at IS NOT NULL
                        FROM deliveries
                        WHERE id = ?
                        """,
                        Boolean.class,
                        deliveryId
                );

        assertTrue(retryStillScheduled);
    }
    @Test
    void sendsStableDeliveryIdHeader() throws Exception {

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
                                        "orderId": 2000
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

        redisTemplate.opsForStream().add(
                MapRecord.create(
                        STREAM,
                        Map.of(
                                "deliveryId",
                                deliveryId.toString()
                        )
                )
        );

        redisTemplate.opsForStream().createGroup(
                STREAM,
                ReadOffset.from("0-0"),
                GROUP
        );

        var deadWorkerMessages =
                redisTemplate.<String, String>opsForStream().read(
                        Consumer.from(
                                GROUP,
                                "dead-worker"
                        ),
                        StreamReadOptions.empty().count(1),
                        StreamOffset.create(
                                STREAM,
                                ReadOffset.lastConsumed()
                        )
                );

        assertNotNull(deadWorkerMessages);
        assertEquals(1, deadWorkerMessages.size());

        Thread.sleep(1200);

        worker.recoverStaleMessages();

        assertEquals(
                deliveryId.toString(),
                lastDeliveryIdHeader.get()
        );
    }
}