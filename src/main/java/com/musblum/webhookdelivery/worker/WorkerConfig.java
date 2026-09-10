package com.musblum.webhookdelivery.worker;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;

@Configuration
@ConditionalOnProperty(
        name = "app.worker.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class WorkerConfig {

    @Bean
    public StreamMessageListenerContainer<String,
            MapRecord<String, String, String>> workerListenerContainer(
            RedisConnectionFactory connectionFactory,
            WebhookWorker webhookWorker,
            StringRedisTemplate redisTemplate) {

        ensureConsumerGroup(redisTemplate);

        var container =
                StreamMessageListenerContainer.create(connectionFactory);

        container.receive(
                Consumer.from(
                        "webhook-workers",
                        webhookWorker.getConsumerName()
                ),
                StreamOffset.create(
                        "webhook-deliveries",
                        ReadOffset.lastConsumed()
                ),
                webhookWorker
        );

        return container;
    }

    private void ensureConsumerGroup(StringRedisTemplate redisTemplate) {
        try {
            redisTemplate.opsForStream().createGroup(
                    "webhook-deliveries",
                    ReadOffset.from("0-0"),
                    "webhook-workers"
            );
        } catch (RedisSystemException e) {
            if (e.getRootCause() == null ||
                    !e.getRootCause().getMessage().contains("BUSYGROUP")) {
                throw e;
            }
        }
    }
}
