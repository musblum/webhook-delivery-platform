package com.musblum.webhookdelivery.worker;

import com.musblum.webhookdelivery.dto.WebhookPayload;
import com.musblum.webhookdelivery.model.WebhookDelivery;
import com.musblum.webhookdelivery.repository.WebhookDeliveryRepository;
import com.musblum.webhookdelivery.service.RetryPolicy;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Instant;
import java.util.UUID;

@Service
@ConditionalOnProperty(
        name = "app.worker.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class WebhookWorker
        implements StreamListener<String, MapRecord<String, String, String>> {

    private static final String GROUP = "webhook-workers";
    private final String consumerName = "worker-" + UUID.randomUUID();
    private final RetryPolicy retryPolicy;

    private final StringRedisTemplate redisTemplate;
    private final WebhookDeliveryRepository deliveryRepository;
    private final RestClient restClient;

    public WebhookWorker(
            StringRedisTemplate redisTemplate,
            WebhookDeliveryRepository deliveryRepository,
            RestClient.Builder restClientBuilder,
            RetryPolicy retryPolicy) {

        this.redisTemplate = redisTemplate;
        this.deliveryRepository = deliveryRepository;
        this.restClient = restClientBuilder.build();
        this.retryPolicy = retryPolicy;
    }

    @Override
    public void onMessage(MapRecord<String, String, String> message) {
        String deliveryId = message.getValue().get("deliveryId");

        System.out.println(
                consumerName + " received deliveryId: " + deliveryId
        );

        WebhookDelivery delivery =
                deliveryRepository.findById(UUID.fromString(deliveryId))
                        .orElseThrow();


        var endpoint = delivery.getEndpoint();
        var event = delivery.getEvent();

        var webhookPayload = new WebhookPayload(
                event.getId(),
                event.getEventType(),
                event.getPayload()
        );

        try {
            var response = restClient.post()
                    .uri(endpoint.getUrl())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(webhookPayload)
                    .retrieve()
                    .toBodilessEntity();

            System.out.println("Webhook response: " + response.getStatusCode());

            delivery.markSucceeded();
            deliveryRepository.save(delivery);

            redisTemplate.opsForStream().acknowledge(
                    GROUP,
                    message
            );

        } catch (RestClientException e) {
            int completedAttempts = delivery.getAttemptCount() + 1;

            if (retryPolicy.isRetryable(e)
                    && retryPolicy.shouldRetry(completedAttempts)) {

                long delaySeconds =
                        retryPolicy.calculateRetryDelaySeconds(
                                completedAttempts,
                                e);

                Instant nextAttemptAt =
                        Instant.now().plusSeconds(delaySeconds);

                delivery.scheduleRetry(
                        nextAttemptAt,
                        e.getMessage()
                );

            } else {
                delivery.markFailed(e.getMessage());
            }

            deliveryRepository.save(delivery);

            redisTemplate.opsForStream().acknowledge(
                    GROUP,
                    message
            );
        }


    }
    public String getConsumerName() {
        return consumerName;
    }
}
