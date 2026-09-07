package com.musblum.webhookdelivery;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class WebhookDeliveryPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(WebhookDeliveryPlatformApplication.class, args);
    }

}
