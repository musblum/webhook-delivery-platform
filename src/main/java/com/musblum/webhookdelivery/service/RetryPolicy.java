package com.musblum.webhookdelivery.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.concurrent.ThreadLocalRandom;

@Service
public class RetryPolicy {
    private static final int MAX_ATTEMPTS = 5;
    private static final long BASE_DELAY_SECONDS = 5;

    public long calculateBackoffSeconds(int attemptCount) {
        return (long) (BASE_DELAY_SECONDS * Math.pow(2, attemptCount - 1));
    }

    public long calculateRetryDelaySeconds(int attemptCount) {
        long backoff = calculateBackoffSeconds(attemptCount);

        long maxJitter = Math.max(1, backoff / 4);

        long jitter = ThreadLocalRandom.current()
                .nextLong(0, maxJitter + 1);

        return backoff + jitter;
    }

    public long calculateRetryDelaySeconds(
            int attemptCount,
            RestClientException exception) {

        if(exception instanceof RestClientResponseException responseException
                && responseException.getStatusCode().value() == 429
                && responseException.getResponseHeaders() != null) {

            String retryAfter=
                    responseException.getResponseHeaders()
                            .getFirst("Retry-After");

            if(retryAfter!=null) {
                try {
                    return Long.parseLong(retryAfter);
                }catch (NumberFormatException ignored) {
                    calculateRetryDelaySeconds(attemptCount);
                }
            }
        }
        return calculateRetryDelaySeconds(attemptCount);

    }

    public boolean shouldRetry(int attemptCount) {
        return attemptCount < MAX_ATTEMPTS;
    }


    public boolean isRetryable(RestClientException exception){

        //Connection failures/timeouts
        if (exception instanceof ResourceAccessException) {
            return true;
        }

        //HTTP error responses
        if(exception instanceof RestClientResponseException responseException) {
            int status = responseException.getStatusCode().value();

            return status == 408
                    || status == 429
                    || status >= 500;
        }
        return false;
    }
}
