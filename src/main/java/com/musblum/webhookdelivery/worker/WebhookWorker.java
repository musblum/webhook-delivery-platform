package com.musblum.webhookdelivery.worker;

import com.musblum.webhookdelivery.dto.WebhookPayload;
import com.musblum.webhookdelivery.model.WebhookDelivery;
import com.musblum.webhookdelivery.repository.WebhookDeliveryRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.UUID;

@Service
@ConditionalOnProperty(
        name = "app.worker.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class WebhookWorker
        implements StreamListener<String, MapRecord<String, String, String>> {

    private static final String STREAM = "webhook-deliveries";
    private static final String GROUP = "webhook-workers";
    private final String consumerName = "worker-" + UUID.randomUUID();

    private final StringRedisTemplate redisTemplate;
    private final WebhookDeliveryRepository deliveryRepository;
    private final RestClient restClient;

    public WebhookWorker(
            StringRedisTemplate redisTemplate,
            WebhookDeliveryRepository deliveryRepository,
            RestClient.Builder restClientBuilder) {

        this.redisTemplate = redisTemplate;
        this.deliveryRepository = deliveryRepository;
        this.restClient = restClientBuilder.build();
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
            System.err.println(
                    consumerName + " failed delivery " + deliveryId
                            + ": " + e.getMessage()
            );
        }


    }
    public String getConsumerName() {
        return consumerName;
    }
}
