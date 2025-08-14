package com.chich.maqoor.controller;

import com.chich.maqoor.dto.CleanCloudWebhookPayload;
import com.chich.maqoor.service.CleanCloudService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/webhook")
public class WebhookController {

    @Autowired
    private CleanCloudService cleanCloudService;

    @PostMapping("/cleancloud")
    public ResponseEntity<Map<String, String>> handleCleanCloudWebhook(@RequestBody CleanCloudWebhookPayload payload) {
        log.info("Received CleanCloud webhook: event={}, orderId={}, storeId={}", 
                payload.getEvent(), payload.getId(), payload.getStoreId());
        
        try {
            // Validate webhook payload
            if (payload.getId() <= 0) {
                log.error("Invalid webhook payload: order ID must be positive");
                return ResponseEntity.badRequest().body(createErrorResponse("Invalid order ID"));
            }
            
            if (payload.getEvent() == null || payload.getEvent().trim().isEmpty()) {
                log.error("Invalid webhook payload: event type is required");
                return ResponseEntity.badRequest().body(createErrorResponse("Event type is required"));
            }
            
            // Process webhook based on event type
            switch (payload.getEvent()) {
                case "order.created":
                    log.info("Processing order.created webhook for order: {}", payload.getId());
                    cleanCloudService.handleOrderCreated(payload.getId());
                    break;
                    
                case "order.status_changed":
                    if (payload.getData() != null && payload.getData().getNewStatus() != null) {
                        log.info("Processing order.status_changed webhook for order: {} with new status: {}", 
                                payload.getId(), payload.getData().getNewStatus());
                        cleanCloudService.handleOrderStatusChanged(payload.getId(), payload.getData().getNewStatus());
                    } else {
                        log.warn("Order status changed webhook missing new status data for order: {}", payload.getId());
                        return ResponseEntity.badRequest().body(createErrorResponse("New status is required for status change events"));
                    }
                    break;
                    
                default:
                    log.warn("Unsupported webhook event type: {} for order: {}", payload.getEvent(), payload.getId());
                    return ResponseEntity.ok().body(createSuccessResponse("Event type not supported, but webhook received"));
            }
            
            log.info("Successfully processed CleanCloud webhook for order: {}", payload.getId());
            return ResponseEntity.ok().body(createSuccessResponse("Webhook processed successfully"));
            
        } catch (Exception e) {
            log.error("Error processing CleanCloud webhook for order {}: {}", payload.getId(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Internal server error: " + e.getMessage()));
        }
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> healthCheck() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "healthy");
        response.put("service", "CleanCloud Webhook Handler");
        response.put("timestamp", java.time.LocalDateTime.now().toString());
        return ResponseEntity.ok(response);
    }

    private Map<String, String> createSuccessResponse(String message) {
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", message);
        response.put("timestamp", java.time.LocalDateTime.now().toString());
        return response;
    }

    private Map<String, String> createErrorResponse(String message) {
        Map<String, String> response = new HashMap<>();
        response.put("status", "error");
        response.put("message", message);
        response.put("timestamp", java.time.LocalDateTime.now().toString());
        return response;
    }
}
