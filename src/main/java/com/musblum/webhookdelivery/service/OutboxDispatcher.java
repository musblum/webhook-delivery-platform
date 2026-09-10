package com.musblum.webhookdelivery.service;

import com.musblum.webhookdelivery.model.OutboxMessage;
import com.musblum.webhookdelivery.repository.OutboxMessageRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@ConditionalOnProperty(
        name = "app.dispatcher.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class OutboxDispatcher {

    private final OutboxMessageRepository outboxMessageRepository;
    private final StringRedisTemplate stringRedisTemplate;

    public OutboxDispatcher(OutboxMessageRepository outboxMessageRepository,
                            StringRedisTemplate stringRedisTemplate) {
        this.outboxMessageRepository = outboxMessageRepository;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Scheduled(fixedDelay = 1000)
    public void dispatchPendingMessages() {
        List<OutboxMessage> messages =
                outboxMessageRepository.findByPublishedAtIsNull();

        for (OutboxMessage message : messages) {
            stringRedisTemplate.opsForStream().add(
                    "webhook-deliveries",
                    Map.of(
                            "deliveryId",
                           message.getDelivery().getId().toString()
                    )
            );
            message.markPublished();
            outboxMessageRepository.save(message);
        }
    }
}
