package com.chich.maqoor.service.impl;

import com.chich.maqoor.dto.CleanCloudOrderDetails;
import com.chich.maqoor.entity.Garments;
import com.chich.maqoor.entity.Orders;
import com.chich.maqoor.entity.constant.Departments;
import com.chich.maqoor.repository.GarmentRepository;
import com.chich.maqoor.repository.OrdersRepository;
import com.chich.maqoor.service.CleanCloudService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import com.chich.maqoor.dto.CleanCloudOrderResponse;
import com.chich.maqoor.dto.CleanCloudGarmentsResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

@Slf4j
@Service
public class CleanCloudServiceImpl implements CleanCloudService {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private OrdersRepository ordersRepository;

    @Autowired
    private GarmentRepository garmentRepository;

    @Value("${cleancloud.api.base-url}")
    private String baseUrl;

    @Value("${cleancloud.api.token}")
    private String apiToken;

    @Value("${cleancloud.api.timeout:30000}")
    private int timeout;

    @Override
    public CleanCloudOrderDetails getOrder(int orderId) {
        try {
            log.info("Fetching complete order details for order {} from CleanCloud", orderId);
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            
            // Step 1: Call getOrders API to get order details
            String ordersUrl = baseUrl + "/api/getOrders";
            String ordersRequestBody = "{\"api_token\":\"" + apiToken + "\",\"orderID\":\"" + orderId + "\"}";
            HttpEntity<String> ordersEntity = new HttpEntity<>(ordersRequestBody, headers);
            
            log.info("Calling getOrders endpoint: {} with body: {}", ordersUrl, ordersRequestBody);
            log.info("API Token being used: {}", apiToken);
            log.info("Order ID being requested: {}", orderId);
            
            // First get the raw response to see what we're actually getting
            ResponseEntity<String> rawOrdersResponse = restTemplate.exchange(
                ordersUrl, 
                HttpMethod.POST, 
                ordersEntity, 
                String.class
            );
            
            log.info("Raw orders response status: {}, body: {}", 
                    rawOrdersResponse.getStatusCode(), rawOrdersResponse.getBody());
            
            // Check for authentication error
            if (rawOrdersResponse.getBody() != null && rawOrdersResponse.getBody().contains("Authentication Error")) {
                log.error("Authentication failed for CleanCloud API. Please check your api_token.");
                log.error("Request body sent: {}", ordersRequestBody);
                log.error("API Token used: {}", apiToken);
                return null;
            }
            
            if (!rawOrdersResponse.getStatusCode().is2xxSuccessful() || rawOrdersResponse.getBody() == null) {
                log.warn("Failed to fetch order {} from CleanCloud. Status: {}", orderId, rawOrdersResponse.getStatusCode());
                return null;
            }
            
            // Parse the raw JSON response manually since there might be content type issues
            String rawResponseBody = rawOrdersResponse.getBody();
            log.info("Parsing raw response: {}", rawResponseBody);
            
            // Try to parse the JSON manually
            CleanCloudOrderResponse ordersData;
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                ordersData = objectMapper.readValue(rawResponseBody, CleanCloudOrderResponse.class);
                log.info("Successfully parsed JSON response to CleanCloudOrderResponse");
            } catch (Exception e) {
                log.error("Failed to parse JSON response: {}", e.getMessage());
                log.error("Raw response that failed to parse: {}", rawResponseBody);
                return null;
            }
            
            log.info("Orders API response status: {}, has body: {}", 
                    rawOrdersResponse.getStatusCode(), ordersData != null);
            
            // Add more detailed logging to see the exact response
            log.info("Full orders response object: {}", ordersData);
            log.info("Success field value: '{}' (length: {})", 
                    ordersData.getSuccess(), 
                    ordersData.getSuccess() != null ? ordersData.getSuccess().length() : "null");
            log.info("Success field equals 'True': {}", "True".equals(ordersData.getSuccess()));
            log.info("Success field equals 'true': {}", "true".equals(ordersData.getSuccess()));
            
            if (ordersData.getOrders() != null && !ordersData.getOrders().isEmpty()) {
                CleanCloudOrderResponse.CleanCloudOrderData firstOrder = ordersData.getOrders().get(0);
                log.info("First order data: id={}, summary={}, status={}", 
                        firstOrder.getId(), firstOrder.getSummary(), firstOrder.getStatus());
            }
            
            // More flexible success check
            boolean isSuccess = ordersData.getSuccess() != null && 
                              ("True".equals(ordersData.getSuccess()) || "true".equals(ordersData.getSuccess()));
            
            if (!isSuccess || ordersData.getOrders() == null || ordersData.getOrders().isEmpty()) {
                log.warn("No order data returned for order {} from CleanCloud", orderId);
                log.warn("Success check failed: Success='{}', Orders null={}, Orders empty={}", 
                        ordersData.getSuccess(), 
                        ordersData.getOrders() == null,
                        ordersData.getOrders() != null ? ordersData.getOrders().isEmpty() : "N/A");
                return null;
            }
            
            CleanCloudOrderResponse.CleanCloudOrderData orderInfo = ordersData.getOrders().get(0);
            log.info("Successfully fetched order {} from CleanCloud: {}", orderId, orderInfo.getSummary());
            
            // Step 2: Call getGarments API to get garment details
            String garmentsUrl = baseUrl + "/api/getGarments";
            String garmentsRequestBody = "{\"api_token\":\"" + apiToken + "\",\"orderID\":\"" + orderId + "\"}";
            HttpEntity<String> garmentsEntity = new HttpEntity<>(garmentsRequestBody, headers);
            
            log.info("Calling getGarments endpoint: {} with body: {}", garmentsUrl, garmentsRequestBody);
            
            // Get raw response for garments API as well
            ResponseEntity<String> rawGarmentsResponse = restTemplate.exchange(
                garmentsUrl,
                HttpMethod.POST,
                garmentsEntity,
                String.class
            );

            log.info("Raw garments response status: {}, body: {}", 
                    rawGarmentsResponse.getStatusCode(), rawGarmentsResponse.getBody());

            if (!rawGarmentsResponse.getStatusCode().is2xxSuccessful() || rawGarmentsResponse.getBody() == null) {
                log.warn("Failed to fetch garments for order {} from CleanCloud. Status: {}", orderId, rawGarmentsResponse.getStatusCode());
                // Return order without garments
                return createOrderDetailsFromResponse(orderInfo, null);
            }
            
            // Parse the raw garments JSON response manually
            String rawGarmentsBody = rawGarmentsResponse.getBody();
            log.info("Parsing raw garments response: {}", rawGarmentsBody);
            
            CleanCloudGarmentsResponse garmentsData;
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                garmentsData = objectMapper.readValue(rawGarmentsBody, CleanCloudGarmentsResponse.class);
                log.info("Successfully parsed garments JSON response to CleanCloudGarmentsResponse");
            } catch (Exception e) {
                log.error("Failed to parse garments JSON response: {}", e.getMessage());
                log.error("Raw garments response that failed to parse: {}", rawGarmentsBody);
                // Return order without garments
                return createOrderDetailsFromResponse(orderInfo, null);
            }
            
            // Add more detailed logging to see the exact response
            log.info("Full garments response object: {}", garmentsData);
            log.info("Success field value: '{}' (length: {})", 
                    garmentsData.getSuccess(), 
                    garmentsData.getSuccess() != null ? garmentsData.getSuccess().length() : "null");
            log.info("Success field equals 'True': {}", "True".equals(garmentsData.getSuccess()));
            log.info("Success field equals 'true': {}", "true".equals(garmentsData.getSuccess()));
            
            if (garmentsData.getGarments() != null && !garmentsData.getGarments().isEmpty()) {
                CleanCloudGarmentsResponse.CleanCloudGarmentData firstGarment = garmentsData.getGarments().get(0);
                log.info("First garment data: barcodeID={}, notes={}", 
                        firstGarment.getBarcodeID(), firstGarment.getNotes());
            }
            
            // More flexible success check for garments
            boolean isGarmentsSuccess = garmentsData.getSuccess() != null && 
                                      ("True".equals(garmentsData.getSuccess()) || "true".equals(garmentsData.getSuccess()));
            
            if (!isGarmentsSuccess || garmentsData.getGarments() == null) {
                log.warn("No garments data returned for order {} from CleanCloud", orderId);
                log.warn("Success check failed: Success='{}', Garments null={}", 
                        garmentsData.getSuccess(), 
                        garmentsData.getGarments() == null);
                // Return order without garments
                return createOrderDetailsFromResponse(orderInfo, null);
            }
            
            log.info("Successfully fetched {} garments for order {} from CleanCloud", 
                    garmentsData.getGarments().size(), orderId);
            
            // Step 3: Combine both API responses to create complete order details
            List<CleanCloudOrderDetails.CleanCloudGarment> convertedGarments = null;
            if (garmentsData != null && !garmentsData.getGarments().isEmpty()) {
                convertedGarments = garmentsData.getGarments().stream()
                    .map(this::convertToCleanCloudGarment)
                    .collect(java.util.stream.Collectors.toList());
            }
            
            CleanCloudOrderDetails orderDetails = createOrderDetailsFromResponse(orderInfo, convertedGarments);
            
            log.info("Created complete order details: orderId={}, summary={}, garments count={}", 
                    orderDetails.getOrderId(), 
                    orderDetails.getSummary(),
                    orderDetails.getGarments() != null ? orderDetails.getGarments().size() : 0);
            
            // Map garment type from order summary to garments (ascending by garment ID)
            try {
                if (orderDetails != null && orderDetails.getSummary() != null && orderDetails.getGarments() != null) {
                    java.util.List<String> summaryTypes = parseTypesFromSummary(orderDetails.getSummary());
                    if (!summaryTypes.isEmpty()) {
                        java.util.List<CleanCloudOrderDetails.CleanCloudGarment> sorted = new java.util.ArrayList<>(orderDetails.getGarments());
                        sorted.sort((a, b) -> {
                            try {
                                int ia = Integer.parseInt(a.getGarmentId());
                                int ib = Integer.parseInt(b.getGarmentId());
                                return Integer.compare(ia, ib);
                            } catch (Exception e) {
                                return String.valueOf(a.getGarmentId()).compareTo(String.valueOf(b.getGarmentId()));
                            }
                        });
                        int n = Math.min(summaryTypes.size(), sorted.size());
                        for (int i = 0; i < n; i++) {
                            String t = summaryTypes.get(i);
                            if (t != null && !t.isBlank()) {
                                sorted.get(i).setType(t.trim());
                            }
                        }
                        // Fallback: if there's exactly one summary item and no types were set, set the first garment's type
                        if (summaryTypes.size() == 1 && !sorted.isEmpty()) {
                            if (sorted.get(0).getType() == null || sorted.get(0).getType().isBlank() || "Unknown".equalsIgnoreCase(sorted.get(0).getType())) {
                                String t = summaryTypes.get(0);
                                if (t != null && !t.isBlank()) sorted.get(0).setType(t.trim());
                            }
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to map garment types from summary: {}", e.getMessage());
            }

            if (orderDetails.getGarments() != null) {
                for (CleanCloudOrderDetails.CleanCloudGarment g : orderDetails.getGarments()) {
                    log.info("Garment in order details: barcodeID={}, garmentId={}, description={}", 
                            g.getBarcodeID(), g.getGarmentId(), g.getDescription());
                }
            }
            
            return orderDetails;
            
        } catch (Exception e) {
            log.error("Error fetching complete order details for order {} from CleanCloud: {}", orderId, e.getMessage(), e);
            return null;
        }
    }

    @Override
    @Transactional
    public void syncOrderWithDatabase(int orderId) {
        try {
            log.info("Starting synchronization for order: {}", orderId);
            
            // Fetch order details from CleanCloud
            CleanCloudOrderDetails orderDetails = getOrder(orderId);
            if (orderDetails == null) {
                log.error("Failed to fetch order details from CleanCloud for order: {}", orderId);
                throw new RuntimeException("Failed to fetch order details from CleanCloud for order: " + orderId);
            }
            
            log.info("Successfully fetched order details: orderId={}, customerName={}, garmentsCount={}", 
                    orderDetails.getOrderId(), 
                    orderDetails.getCustomerName(),
                    orderDetails.getGarments() != null ? orderDetails.getGarments().size() : 0);
            
            // Check if order already exists
            Optional<Orders> existingOrder = ordersRepository.findByCleanCloudOrderId(orderId);
            
            if (existingOrder.isPresent()) {
                log.info("Updating existing order: {}", existingOrder.get().getOrderId());
                // Update existing order
                updateExistingOrder(existingOrder.get(), orderDetails);
            } else {
                log.info("Creating new order for CleanCloud order: {}", orderId);
                // Create new order
                createNewOrder(orderDetails);
            }
            
            log.info("Successfully synchronized order {} with database", orderId);
            
        } catch (Exception e) {
            log.error("Error synchronizing order {} with database: {}", orderId, e.getMessage(), e);
            throw new RuntimeException("Failed to sync order " + orderId + " with database: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public void handleOrderCreated(int orderId) {
        log.info("Handling order created event for order: {}", orderId);
        syncOrderWithDatabase(orderId);
    }

    @Override
    @Transactional
    public void handleOrderStatusChanged(int orderId, int newStatus) {
        log.info("Handling order status changed event for order: {} with new status: {}", orderId, newStatus);
        
        try {
            // Update order status in database
            Optional<Orders> existingOrder = ordersRepository.findByCleanCloudOrderId(orderId);
            if (existingOrder.isPresent()) {
                Orders order = existingOrder.get();
                order.setStatus(String.valueOf(newStatus));
                order.setUpdatedAt(new Date());
                
                // If new status is 2, mark order as completed
                if (newStatus == 2) {
                    order.setOrderState(com.chich.maqoor.entity.constant.OrderState.COMPLETED);
                    log.info("Order {} marked as COMPLETED (status: 2)", orderId);
                } else {
                    // For other statuses, set as ACTIVE
                    order.setOrderState(com.chich.maqoor.entity.constant.OrderState.ACTIVE);
                    log.info("Order {} set as ACTIVE (status: {})", orderId, newStatus);
                }
                
                ordersRepository.save(order);
                log.info("Updated order {} status to {} with orderState: {}", orderId, newStatus, order.getOrderState());
            } else {
                log.warn("Order {} not found in database, syncing from CleanCloud", orderId);
                // If order doesn't exist, sync it completely
                syncOrderWithDatabase(orderId);
            }
        } catch (Exception e) {
            log.error("Error handling order status change for order {}: {}", orderId, e.getMessage(), e);
        }
    }

    private void createNewOrder(CleanCloudOrderDetails orderDetails) {
        try {
            log.info("Creating new order with CleanCloud ID: {}", orderDetails.getOrderId());
            
            Orders newOrder = new Orders();
            // Convert String orderId to Integer for database compatibility
            try {
                newOrder.setCleanCloudOrderId(Integer.parseInt(orderDetails.getOrderId()));
            } catch (NumberFormatException e) {
                log.error("Invalid order ID format: {}", orderDetails.getOrderId());
                throw new RuntimeException("Invalid order ID format: " + orderDetails.getOrderId(), e);
            }
            newOrder.setOrderNumber(orderDetails.getOrderId());
            newOrder.setCustomerName(orderDetails.getCustomerName());
            newOrder.setCustomerPhone(orderDetails.getCustomerPhone());
            newOrder.setPickupDate(orderDetails.getPickupDate());
            newOrder.setDeliveryDate(orderDetails.getDeliveryDate());
            newOrder.setStatus(orderDetails.getStatus());
            newOrder.setCreatedAt(new Date());
            newOrder.setUpdatedAt(new Date());
            
            // Save the order first
            Orders savedOrder = ordersRepository.save(newOrder);
            log.info("Successfully saved order to database: orderId={}", savedOrder.getOrderId());
            
            // Create garments for this order
            if (orderDetails.getGarments() != null) {
                log.info("Creating {} garments for order {}", orderDetails.getGarments().size(), savedOrder.getOrderId());
                for (CleanCloudOrderDetails.CleanCloudGarment garmentData : orderDetails.getGarments()) {
                    try {
                        createGarment(garmentData, savedOrder);
                        log.debug("Successfully created garment: barcodeID={}", garmentData.getBarcodeID());
                    } catch (Exception e) {
                        log.error("Failed to create garment: barcodeID={}, error: {}", garmentData.getBarcodeID(), e.getMessage(), e);
                        throw new RuntimeException("Failed to create garment " + garmentData.getBarcodeID() + ": " + e.getMessage(), e);
                    }
                }
                log.info("Successfully created all {} garments for order {}", orderDetails.getGarments().size(), savedOrder.getOrderId());
            } else {
                log.warn("No garments provided for order {}", savedOrder.getOrderId());
            }
            
            log.info("Created new order {} with {} garments", savedOrder.getOrderId(), 
                    orderDetails.getGarments() != null ? orderDetails.getGarments().size() : 0);
                    
        } catch (Exception e) {
            log.error("Error creating new order: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create new order: " + e.getMessage(), e);
        }
    }

    private void updateExistingOrder(Orders existingOrder, CleanCloudOrderDetails orderDetails) {
        log.info("Updating existing order: {}", existingOrder.getOrderId());
        
        // Update order details
        existingOrder.setCustomerName(orderDetails.getCustomerName());
        existingOrder.setCustomerPhone(orderDetails.getCustomerPhone());
        existingOrder.setPickupDate(orderDetails.getPickupDate());
        existingOrder.setDeliveryDate(orderDetails.getDeliveryDate());
        existingOrder.setStatus(orderDetails.getStatus());
        existingOrder.setUpdatedAt(new Date());
        
        // Save updated order
        ordersRepository.save(existingOrder);
        
        // Sync garments
        syncGarmentsForOrder(existingOrder, orderDetails);
        
        log.info("Updated existing order: {}", existingOrder.getOrderId());
    }

    private void syncGarmentsForOrder(Orders order, CleanCloudOrderDetails orderDetails) {
        log.info("Synchronizing garments for order: {}", order.getOrderId());
        
        // Get existing garments for this order
        List<Garments> existingGarments = garmentRepository.findAllByOrder_OrderId(order.getOrderId());
        
        // Create a map of existing garments by CleanCloud ID for quick lookup
        java.util.Map<String, Garments> existingGarmentMap = existingGarments.stream()
                .collect(java.util.stream.Collectors.toMap(
                    Garments::getCleanCloudGarmentId,
                    garment -> garment
                ));
        
        // Process new garments from CleanCloud
        if (orderDetails.getGarments() != null) {
            for (CleanCloudOrderDetails.CleanCloudGarment garmentData : orderDetails.getGarments()) {
                String cleanCloudGarmentId = garmentData.getBarcodeID(); // Use barcodeID for lookup
                
                if (existingGarmentMap.containsKey(cleanCloudGarmentId)) {
                    // Update existing garment
                    updateGarment(existingGarmentMap.get(cleanCloudGarmentId), garmentData);
                    existingGarmentMap.remove(cleanCloudGarmentId); // Mark as processed
                } else {
                    // Create new garment
                    createGarment(garmentData, order);
                }
            }
        }
        
        // Remove garments that no longer exist in CleanCloud
        for (Garments garmentToRemove : existingGarmentMap.values()) {
            log.info("Removing garment {} from order {} (no longer exists in CleanCloud)", 
                    garmentToRemove.getGarmentId(), order.getOrderId());
            garmentRepository.delete(garmentToRemove);
        }
        
        log.info("Garment synchronization completed for order: {}", order.getOrderId());
    }

    private void createGarment(CleanCloudOrderDetails.CleanCloudGarment garmentData, Orders order) {
        try {
            log.info("Creating garment: barcodeID={}, garmentId={}, description={}", 
                    garmentData.getBarcodeID(), garmentData.getGarmentId(), garmentData.getDescription());
            
            Garments newGarment = new Garments();
            // Use the actual barcodeID from CleanCloud, not the generated garmentId
            newGarment.setCleanCloudGarmentId(garmentData.getBarcodeID());
            newGarment.setOrder(order);
            newGarment.setDescription(garmentData.getDescription());
            newGarment.setType(garmentData.getType());
            newGarment.setColor(garmentData.getColor());
            newGarment.setSize(garmentData.getSize());
            newGarment.setSpecialInstructions(garmentData.getSpecialInstructions());
            newGarment.setDepartmentId(Departments.RECEPTION); // Default to reception
            newGarment.setCreatedAt(new Date());
            newGarment.setLastUpdate(new Date());
            
            log.info("About to save garment with order ID: {}", order.getOrderId());
            Garments savedGarment = garmentRepository.save(newGarment);
            log.info("Successfully created garment with ID: {} linked to order: {}", 
                    savedGarment.getGarmentId(), savedGarment.getOrder().getOrderId());
                    
        } catch (Exception e) {
            log.error("Error creating garment: barcodeID={}, error: {}", garmentData.getBarcodeID(), e.getMessage(), e);
            throw new RuntimeException("Failed to create garment " + garmentData.getBarcodeID() + ": " + e.getMessage(), e);
        }
    }

    private void updateGarment(Garments existingGarment, CleanCloudOrderDetails.CleanCloudGarment garmentData) {
        existingGarment.setDescription(garmentData.getDescription());
        existingGarment.setType(garmentData.getType());
        existingGarment.setColor(garmentData.getColor());
        existingGarment.setSize(garmentData.getSize());
        existingGarment.setSpecialInstructions(garmentData.getSpecialInstructions());
        existingGarment.setLastUpdate(new Date());
        
        garmentRepository.save(existingGarment);
        log.debug("Updated existing garment: {} for order: {}", garmentData.getBarcodeID(), existingGarment.getOrder().getOrderId());
    }

    private CleanCloudOrderDetails createOrderDetailsFromResponse(CleanCloudOrderResponse.CleanCloudOrderData orderInfo, List<CleanCloudOrderDetails.CleanCloudGarment> garments) {
        if (orderInfo == null) {
            return null;
        }

        // Convert CleanCloudOrderResponse.CleanCloudOrderData to CleanCloudOrderDetails
        CleanCloudOrderDetails orderDetails = new CleanCloudOrderDetails();
        orderDetails.setOrderId(orderInfo.getId());
        orderDetails.setStatus(orderInfo.getStatus());
        orderDetails.setCustomerID(orderInfo.getCustomerID());
        orderDetails.setCustomerName(orderInfo.getSummary() != null ? orderInfo.getSummary() : "Unknown Customer");
        orderDetails.setCustomerPhone("");
        orderDetails.setDeliveryDate(orderInfo.getDeliveryDate());
        orderDetails.setPickupDate(orderInfo.getPickupDate());
        orderDetails.setPieces(orderInfo.getPieces());
        orderDetails.setSummary(orderInfo.getSummary());
        orderDetails.setGarments(garments);
        
        return orderDetails;
    }
    
    /**
     * Format garment ID by adding a leading "0" prefix
     * Example: "146081" becomes "0146081"
     */
    private String formatGarmentId(String garmentId) {
        if (garmentId == null || garmentId.isEmpty()) {
            return garmentId;
        }
        return "0" + garmentId;
    }

    private CleanCloudOrderDetails.CleanCloudGarment convertToCleanCloudGarment(CleanCloudGarmentsResponse.CleanCloudGarmentData garmentData) {
        CleanCloudOrderDetails.CleanCloudGarment garment = new CleanCloudOrderDetails.CleanCloudGarment();
        String formattedGarmentId = formatGarmentId(garmentData.getBarcodeID());
        garment.setBarcodeID(formattedGarmentId);
        
        // Use the actual barcodeID as garmentId since that's what we want to display
        garment.setGarmentId(formattedGarmentId);
        
        // Set compatibility fields
        garment.setDescription(garmentData.getNotes() != null ? garmentData.getNotes() : "Garment");
        garment.setType("Unknown"); // CleanCloud doesn't provide type
        garment.setColor(garmentData.getColor1() != null ? garmentData.getColor1() : "Unknown");
        garment.setSize("Unknown"); // CleanCloud doesn't provide size
        garment.setSpecialInstructions(garmentData.getNotes() != null ? garmentData.getNotes() : "");
        
        // Set original fields
        garment.setColor1(garmentData.getColor1());
        garment.setColor2(garmentData.getColor2());
        garment.setNotes(garmentData.getNotes());
        garment.setCustomStatus(garmentData.getCustomStatus());
        garment.setConveyorLocation(garmentData.getConveyorLocation());
        return garment;
    }

    // Helper: parse summary lines to type labels
    private java.util.List<String> parseTypesFromSummary(String summary) {
        java.util.List<String> result = new java.util.ArrayList<>();
        if (summary == null || summary.isBlank()) return result;

        // Normalize line breaks and <br> variants
        String normalized = summary
                .replace("<br />", "<br>")
                .replace("<br/>", "<br>")
                .replace("\r\n", "\n")
                .replace("\r", "\n");

        // Split items by <br>
        String[] items = normalized.split("<br>");
        java.util.regex.Pattern qtyPattern = java.util.regex.Pattern.compile("(?i)\\bx\\s*\\d+");

        for (String part : items) {
            if (part == null) continue;
            String line = part.trim();
            if (line.isEmpty()) continue;

            // Find the 'x N' portion even if preceded by whitespace/newline
            java.util.regex.Matcher m = qtyPattern.matcher(line);
            String name;
            if (m.find()) {
                name = line.substring(0, m.start()).trim();
            } else {
                // If there is no explicit quantity, take the whole line
                name = line;
            }
            name = name.replace("&amp;", "&").trim();
            if (!name.isEmpty()) result.add(name);
        }
        return result;
    }
}
