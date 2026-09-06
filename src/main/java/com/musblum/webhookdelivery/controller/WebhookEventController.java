package com.musblum.webhookdelivery.controller;

import com.musblum.webhookdelivery.dto.CreateEventRequest;
import com.musblum.webhookdelivery.dto.EventResponse;
import com.musblum.webhookdelivery.model.WebhookEvent;
import com.musblum.webhookdelivery.service.WebhookEventService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/events")
public class WebhookEventController {

    private final WebhookEventService eventService;

    public WebhookEventController(WebhookEventService webhookEventService) {
        this.eventService = webhookEventService;}


    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EventResponse createEvent(
            @Valid @RequestBody CreateEventRequest request
            ){
        WebhookEvent event =
                eventService.createEvent
                        (request.endpointId(), request.eventType(), request.payload());
        return EventResponse.from(event);
    }

    @GetMapping("/{id}")
    public EventResponse getEvent(@PathVariable UUID id){
        WebhookEvent event = eventService.getEvent(id);
        return EventResponse.from(event);
    }
}
