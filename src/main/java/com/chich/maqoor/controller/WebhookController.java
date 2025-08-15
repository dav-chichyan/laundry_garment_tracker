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
import java.util.Arrays;
import com.chich.maqoor.dto.CleanCloudOrderDetails;
import com.chich.maqoor.entity.Garments;
import com.chich.maqoor.entity.GarmentScan;
import com.chich.maqoor.entity.Orders;
import com.chich.maqoor.entity.User;
import com.chich.maqoor.repository.GarmentRepository;
import com.chich.maqoor.repository.GarmentScanRepository;
import com.chich.maqoor.repository.OrdersRepository;
import com.chich.maqoor.repository.UserRepository;
import com.chich.maqoor.entity.constant.Departments;
import java.util.Optional;
import java.util.Date;
import org.springframework.security.crypto.password.PasswordEncoder;

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

    @Autowired
    private PasswordEncoder passwordEncoder;

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

    @PostMapping("/test-scan")
    public ResponseEntity<Map<String, Object>> testScan(@RequestBody Map<String, Object> request) {
        log.info("Test scan request: {}", request);
        
        try {
            String garmentId = (String) request.get("garmentId");
            Integer userId = (Integer) request.get("userId");
            String departmentStr = (String) request.get("department");
            
            if (garmentId == null || userId == null || departmentStr == null) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "Missing required parameters: garmentId, userId, department");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            // Find garment by cleanCloudGarmentId
            Garments garment = garmentRepository.findByCleanCloudGarmentId(garmentId);
            if (garment == null) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "Garment not found: " + garmentId);
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            // Find user
            Optional<User> userOpt = userRepository.findById(userId);
            if (!userOpt.isPresent()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "User not found: " + userId);
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            User user = userOpt.get();
            Departments department = Departments.valueOf(departmentStr.toUpperCase());
            
            // Create scan record
            GarmentScan scan = new GarmentScan();
            scan.setGarment(garment);
            scan.setUser(user);
            scan.setDepartment(department);
            scan.setScannedAt(new Date());
            garmentScanRepository.save(scan);
            
            // Update garment department
            garment.setDepartmentId(department);
            garment.setLastUpdate(new Date());
            garmentRepository.save(garment);
            
            Map<String, Object> successResponse = new HashMap<>();
            successResponse.put("success", true);
            successResponse.put("message", "Test scan completed successfully");
            successResponse.put("garmentId", garmentId);
            successResponse.put("newDepartment", department.name());
            successResponse.put("scannedBy", user.getName());
            successResponse.put("scanId", scan.getId());
            
            return ResponseEntity.ok(successResponse);
            
        } catch (Exception e) {
            log.error("Error in test scan: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @PostMapping("/test-scan-bypass")
    public ResponseEntity<Map<String, Object>> testScanBypass(@RequestBody Map<String, Object> request) {
        log.info("Test scan bypass request: {}", request);
        
        try {
            String garmentId = (String) request.get("garmentId");
            Integer userId = (Integer) request.get("userId");
            String departmentStr = (String) request.get("department");
            
            if (garmentId == null || userId == null || departmentStr == null) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "Missing required parameters: garmentId, userId, department");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            log.info("Testing scan for garment: {}, user: {}, department: {}", garmentId, userId, departmentStr);
            
            // Find garment by cleanCloudGarmentId
            Garments garment = garmentRepository.findByCleanCloudGarmentId(garmentId);
            if (garment == null) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "Garment not found: " + garmentId);
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            log.info("Found garment: garmentId={}, currentDepartment={}", garment.getGarmentId(), garment.getDepartmentId());
            
            // Find user
            Optional<User> userOpt = userRepository.findById(userId);
            if (!userOpt.isPresent()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "User not found: " + userId);
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            User user = userOpt.get();
            log.info("Found user: userId={}, name={}, department={}", user.getId(), user.getName(), user.getDepartment());
            
            Departments department = Departments.valueOf(departmentStr.toUpperCase());
            
            // Check if garment is already in the requested department
            if (garment.getDepartmentId() == department) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Garment " + garmentId + " is already in " + department + " department");
                response.put("errorCode", "ALREADY_IN_DEPARTMENT");
                return ResponseEntity.ok(response);
            }
            
            // Create scan record
            GarmentScan scan = new GarmentScan();
            scan.setGarment(garment);
            scan.setUser(user);
            scan.setDepartment(department);
            scan.setScannedAt(new Date());
            garmentScanRepository.save(scan);
            
            // Update garment department
            garment.setDepartmentId(department);
            garment.setLastUpdate(new Date());
            garmentRepository.save(garment);
            
            log.info("Successfully processed scan: garment {} moved from {} to {}", 
                    garmentId, garment.getDepartmentId(), department);
            
            Map<String, Object> successResponse = new HashMap<>();
            successResponse.put("success", true);
            successResponse.put("message", "Test scan completed successfully");
            successResponse.put("garmentId", garmentId);
            successResponse.put("newDepartment", department.name());
            successResponse.put("scannedBy", user.getName());
            successResponse.put("scanId", scan.getId());
            
            return ResponseEntity.ok(successResponse);
            
        } catch (Exception e) {
            log.error("Error in test scan bypass: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @PostMapping("/setup-test-data")
    public ResponseEntity<Map<String, Object>> setupTestData() {
        log.info("Setting up test data for order 8421");
        
        try {
            // Create order 8421
            Orders order = new Orders();
            order.setCleanCloudOrderId(8421);
            order.setOrderNumber("8421");
            order.setCustomerName("Blazer x 1<br>Dress x 1<br>Jacket x 1<br>");
            order.setCustomerPhone("+1234567890");
            order.setPickupDate("2025-01-15");
            order.setDeliveryDate("2025-01-17");
            order.setStatus("2");
            order.setType("DRY_CLEAN");
            order.setExpress(false);
            order.setCreatedAt(new Date());
            order.setUpdatedAt(new Date());
            Orders savedOrder = ordersRepository.save(order);
            log.info("Created test order: {}", savedOrder.getOrderId());

            // Create garments 21520, 21521, 21522
            List<Garments> garments = Arrays.asList(
                createTestGarment("21520", "", savedOrder),
                createTestGarment("21521", "Ունի գոտի", savedOrder),
                createTestGarment("21522", "", savedOrder)
            );
            
            garmentRepository.saveAll(garments);
            log.info("Created {} test garments", garments.size());

            // Create Vivyen user in EXAMINATION department if not exists
            Optional<User> vivyenOpt = userRepository.findByEmail("vivyen@gmail.com");
            User vivyen;
            if (!vivyenOpt.isPresent()) {
                vivyen = new User();
                vivyen.setName("Vivyen");
                vivyen.setEmail("vivyen@gmail.com");
                vivyen.setPassword("vivyen123"); // In production, this should be encoded
                vivyen.setRole(com.chich.maqoor.entity.constant.Role.USER);
                vivyen.setState(com.chich.maqoor.entity.constant.UserState.ACTIVE);
                vivyen.setDepartment(Departments.EXAMINATION);
                vivyen.setUsername("vivyen@gmail.com");
                vivyen = userRepository.save(vivyen);
                log.info("Created Vivyen user: {}", vivyen.getEmail());
            } else {
                vivyen = vivyenOpt.get();
                log.info("Vivyen user already exists: {}", vivyen.getEmail());
            }
            
            Map<String, Object> successResponse = new HashMap<>();
            successResponse.put("success", true);
            successResponse.put("message", "Test data setup completed successfully");
            successResponse.put("orderId", savedOrder.getOrderId());
            successResponse.put("garmentsCount", garments.size());
            successResponse.put("vivyenUserId", vivyen.getId());
            
            return ResponseEntity.ok(successResponse);
            
        } catch (Exception e) {
            log.error("Error setting up test data: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    @GetMapping("/test-order/{orderId}")
    public ResponseEntity<Map<String, Object>> testOrderProcessing(@PathVariable int orderId) {
        log.info("Testing order processing for order: {}", orderId);
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Check if order exists in database
            Optional<Orders> existingOrder = ordersRepository.findByCleanCloudOrderId(orderId);
            if (existingOrder.isPresent()) {
                Orders order = existingOrder.get();
                response.put("orderExists", true);
                response.put("orderId", order.getOrderId());
                response.put("cleanCloudOrderId", order.getCleanCloudOrderId());
                response.put("customerName", order.getCustomerName());
                response.put("status", order.getStatus());
                response.put("createdAt", order.getCreatedAt());
                
                // Check garments for this order
                List<Garments> garments = garmentRepository.findAllByOrder_OrderId(order.getOrderId());
                response.put("garmentsCount", garments.size());
                
                List<Map<String, Object>> garmentsData = new ArrayList<>();
                for (Garments garment : garments) {
                    Map<String, Object> garmentInfo = new HashMap<>();
                    garmentInfo.put("garmentId", garment.getGarmentId());
                    garmentInfo.put("cleanCloudGarmentId", garment.getCleanCloudGarmentId());
                    garmentInfo.put("description", garment.getDescription());
                    garmentInfo.put("departmentId", garment.getDepartmentId());
                    garmentInfo.put("createdAt", garment.getCreatedAt());
                    garmentsData.add(garmentInfo);
                }
                response.put("garments", garmentsData);
                
            } else {
                response.put("orderExists", false);
                
                // Try to fetch from CleanCloud
                log.info("Order not found in database, attempting to fetch from CleanCloud");
                try {
                    cleanCloudService.handleOrderCreated(orderId);
                    response.put("cleanCloudSync", "attempted");
                    response.put("message", "Order sync attempted. Check logs for details.");
                } catch (Exception e) {
                    response.put("cleanCloudSync", "failed");
                    response.put("error", e.getMessage());
                    log.error("Failed to sync order {} from CleanCloud: {}", orderId, e.getMessage(), e);
                }
            }
            
            response.put("success", true);
            
        } catch (Exception e) {
            log.error("Error testing order processing for order {}: {}", orderId, e.getMessage(), e);
            response.put("success", false);
            response.put("error", e.getMessage());
        }
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/create-test-order-8422")
    public ResponseEntity<Map<String, Object>> createTestOrder8422() {
        log.info("Creating test order 8422 with garments 21523, 21524, 21525, 21526");
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Check if order already exists
            Optional<Orders> existingOrder = ordersRepository.findByCleanCloudOrderId(8422);
            if (existingOrder.isPresent()) {
                response.put("message", "Order 8422 already exists");
                response.put("orderId", existingOrder.get().getOrderId());
                return ResponseEntity.ok(response);
            }
            
            // Create order 8422
            Orders order = new Orders();
            order.setCleanCloudOrderId(8422);
            order.setOrderNumber("8422");
            order.setCustomerName("Test Customer");
            order.setCustomerPhone("+1234567890");
            order.setPickupDate("2025-01-15");
            order.setDeliveryDate("2025-01-17");
            order.setStatus("2");
            order.setType("DRY_CLEAN");
            order.setExpress(false);
            order.setCreatedAt(new Date());
            order.setUpdatedAt(new Date());
            Orders savedOrder = ordersRepository.save(order);
            log.info("Created test order: {}", savedOrder.getOrderId());

            // Create garments 21523, 21524, 21525, 21526
            List<Garments> garments = Arrays.asList(
                createTestGarment("21523", "Test Garment 1", savedOrder),
                createTestGarment("21524", "Test Garment 2", savedOrder),
                createTestGarment("21525", "Test Garment 3", savedOrder),
                createTestGarment("21526", "Test Garment 4", savedOrder)
            );
            
            garmentRepository.saveAll(garments);
            log.info("Created {} test garments", garments.size());
            
            response.put("success", true);
            response.put("message", "Test order 8422 created successfully");
            response.put("orderId", savedOrder.getOrderId());
            response.put("garmentsCount", garments.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error creating test order 8422: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/create-test-garment")
    public ResponseEntity<Map<String, Object>> createTestGarment() {
        log.info("Creating test garment for scanning");
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Create a simple test garment
            Garments garment = new Garments();
            garment.setCleanCloudGarmentId("GAR-101");
            garment.setDescription("Test Blue Shirt");
            garment.setType("SHIRT");
            garment.setColor("BLUE");
            garment.setSize("M");
            garment.setSpecialInstructions("Test garment for scanning");
            garment.setDepartmentId(Departments.IRONING);
            garment.setLastUpdate(new Date());
            garment.setCreatedAt(new Date());
            
            Garments savedGarment = garmentRepository.save(garment);
            log.info("Created test garment: {}", savedGarment.getCleanCloudGarmentId());
            
            response.put("success", true);
            response.put("message", "Test garment created successfully");
            response.put("garmentId", savedGarment.getCleanCloudGarmentId());
            response.put("department", savedGarment.getDepartmentId());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error creating test garment: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/set-manoyan-password")
    public ResponseEntity<Map<String, Object>> setManoyanPassword() {
        log.info("Setting password for Manoyan user");
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Find Manoyan user
            Optional<User> manoyanOpt = userRepository.findByEmail("manoyan@maqoor.com");
            if (!manoyanOpt.isPresent()) {
                response.put("success", false);
                response.put("message", "Manoyan user not found");
                return ResponseEntity.badRequest().body(response);
            }
            
            User manoyan = manoyanOpt.get();
            
            // Set a simple password: "manoyan123"
            manoyan.setPassword(passwordEncoder.encode("manoyan123"));
            userRepository.save(manoyan);
            
            log.info("Successfully set password for Manoyan user: {}", manoyan.getEmail());
            
            response.put("success", true);
            response.put("message", "Password set successfully for Manoyan");
            response.put("email", manoyan.getEmail());
            response.put("password", "manoyan123");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error setting password for Manoyan: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    private Garments createTestGarment(String cleanCloudId, String description, Orders order) {
        Garments garment = new Garments();
        garment.setCleanCloudGarmentId(cleanCloudId);
        garment.setOrder(order);
        garment.setDescription(description);
        garment.setType("Unknown");
        garment.setColor("Unknown");
        garment.setSize("Unknown");
        garment.setSpecialInstructions("");
        garment.setDepartmentId(Departments.RECEPTION);
        garment.setLastUpdate(new Date());
        garment.setCreatedAt(new Date());
        return garment;
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
