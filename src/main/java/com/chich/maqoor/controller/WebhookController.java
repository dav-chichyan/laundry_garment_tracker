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
import java.util.ArrayList;
import java.util.List;
import com.chich.maqoor.dto.CleanCloudOrderDetails;
import com.chich.maqoor.entity.Garments;
import com.chich.maqoor.entity.GarmentScan;
import com.chich.maqoor.entity.Orders;
import com.chich.maqoor.entity.User;
import com.chich.maqoor.repository.GarmentRepository;
import com.chich.maqoor.repository.GarmentScanRepository;
import com.chich.maqoor.repository.OrdersRepository;
import com.chich.maqoor.repository.UserRepository;

@Slf4j
@RestController
@RequestMapping("/webhook")
public class WebhookController {

    @Autowired
    private CleanCloudService cleanCloudService;

    @Autowired
    private OrdersRepository ordersRepository;

    @Autowired
    private GarmentRepository garmentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GarmentScanRepository garmentScanRepository;

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

    @GetMapping("/test-api/{orderId}")
    public ResponseEntity<Map<String, Object>> testCleanCloudAPI(@PathVariable int orderId) {
        log.info("Testing CleanCloud API directly for order: {}", orderId);
        
        try {
            // Call the service directly
            CleanCloudOrderDetails orderDetails = cleanCloudService.getOrder(orderId);
            
            Map<String, Object> response = new HashMap<>();
            if (orderDetails != null) {
                response.put("success", true);
                response.put("orderId", orderDetails.getOrderId());
                response.put("summary", orderDetails.getSummary());
                response.put("garmentsCount", orderDetails.getGarments() != null ? orderDetails.getGarments().size() : 0);
                
                if (orderDetails.getGarments() != null) {
                    List<Map<String, String>> garments = new ArrayList<>();
                    for (CleanCloudOrderDetails.CleanCloudGarment g : orderDetails.getGarments()) {
                        Map<String, String> garment = new HashMap<>();
                        garment.put("barcodeID", g.getBarcodeID());
                        garment.put("description", g.getDescription());
                        garment.put("type", g.getType());
                        garments.add(garment);
                    }
                    response.put("garments", garments);
                }
            } else {
                response.put("success", false);
                response.put("message", "Failed to fetch order details");
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error testing CleanCloud API for order {}: {}", orderId, e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/db-viewer")
    public ResponseEntity<Map<String, Object>> viewDatabase() {
        log.info("Database viewer accessed");
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Get orders
            List<Orders> orders = ordersRepository.findAll();
            Map<String, Object> ordersData = new HashMap<>();
            for (Orders order : orders) {
                Map<String, Object> orderInfo = new HashMap<>();
                orderInfo.put("orderId", order.getOrderId());
                orderInfo.put("cleanCloudOrderId", order.getCleanCloudOrderId());
                orderInfo.put("customerName", order.getCustomerName());
                orderInfo.put("status", order.getStatus());
                orderInfo.put("createdAt", order.getCreatedAt());
                ordersData.put("Order_" + order.getOrderId(), orderInfo);
            }
            response.put("orders", ordersData);
            
            // Get garments
            List<Garments> garments = garmentRepository.findAll();
            Map<String, Object> garmentsData = new HashMap<>();
            for (Garments garment : garments) {
                Map<String, Object> garmentInfo = new HashMap<>();
                garmentInfo.put("garmentId", garment.getGarmentId());
                garmentInfo.put("cleanCloudGarmentId", garment.getCleanCloudGarmentId());
                garmentInfo.put("description", garment.getDescription());
                garmentInfo.put("departmentId", garment.getDepartmentId());
                garmentInfo.put("orderId", garment.getOrder() != null ? garment.getOrder().getOrderId() : null);
                garmentInfo.put("createdAt", garment.getCreatedAt());
                garmentsData.put("Garment_" + garment.getGarmentId(), garmentInfo);
            }
            response.put("garments", garmentsData);
            
            // Get users
            List<User> users = userRepository.findAll();
            Map<String, Object> usersData = new HashMap<>();
            for (User user : users) {
                Map<String, Object> userInfo = new HashMap<>();
                userInfo.put("id", user.getId());
                userInfo.put("name", user.getName());
                userInfo.put("email", user.getEmail());
                userInfo.put("department", user.getDepartment());
                userInfo.put("role", user.getRole());
                usersData.put("User_" + user.getId(), userInfo);
            }
            response.put("users", usersData);
            
            // Get garment scans
            List<GarmentScan> scans = garmentScanRepository.findAll();
            Map<String, Object> scansData = new HashMap<>();
            for (GarmentScan scan : scans) {
                Map<String, Object> scanInfo = new HashMap<>();
                scanInfo.put("id", scan.getId());
                scanInfo.put("garmentId", scan.getGarment().getGarmentId());
                scanInfo.put("cleanCloudGarmentId", scan.getGarment().getCleanCloudGarmentId());
                scanInfo.put("userId", scan.getUser().getId());
                scanInfo.put("userName", scan.getUser().getName());
                scanInfo.put("department", scan.getDepartment());
                scanInfo.put("scannedAt", scan.getScannedAt());
                scansData.put("Scan_" + scan.getId(), scanInfo);
            }
            response.put("scans", scansData);
            
            response.put("success", true);
            response.put("message", "Database contents retrieved successfully");
            
        } catch (Exception e) {
            log.error("Error retrieving database contents: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Error: " + e.getMessage());
        }
        
        return ResponseEntity.ok(response);
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
