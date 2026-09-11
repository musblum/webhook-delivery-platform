package com.musblum.webhookdelivery.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RetryPolicyTest {

    private final RetryPolicy retryPolicy = new RetryPolicy();

    @Test
    void allowsRetryBeforeMaxAttempts() {
        assertTrue(retryPolicy.shouldRetry(4));
    }

    @Test
    void stopsRetryingAtMaxAttempts() {
        assertFalse(retryPolicy.shouldRetry(5));
    }
    @Test
    void usesRetryAfterHeaderFor429() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Retry-After", "30");

        RestClientException exception =
                HttpClientErrorException.create(
                        HttpStatus.TOO_MANY_REQUESTS,
                        "Too Many Requests",
                        headers,
                        new byte[0],
                        StandardCharsets.UTF_8
                );

        long delay =
                retryPolicy.calculateRetryDelaySeconds(
                        1,
                        exception
                );

        assertEquals(30, delay);
    }

}