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
                return;
            }
            
            // Check if order already exists
            Optional<Orders> existingOrder = ordersRepository.findByCleanCloudOrderId(orderId);
            
            if (existingOrder.isPresent()) {
                // Update existing order
                updateExistingOrder(existingOrder.get(), orderDetails);
            } else {
                // Create new order
                createNewOrder(orderDetails);
            }
            
            log.info("Successfully synchronized order {} with database", orderId);
            
        } catch (Exception e) {
            log.error("Error synchronizing order {} with database: {}", orderId, e.getMessage(), e);
            throw new RuntimeException("Failed to sync order with database", e);
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
                ordersRepository.save(order);
                log.info("Updated order {} status to {}", orderId, newStatus);
            } else {
                // If order doesn't exist, sync it completely
                syncOrderWithDatabase(orderId);
            }
        } catch (Exception e) {
            log.error("Error handling order status change for order {}: {}", orderId, e.getMessage(), e);
        }
    }

    private void createNewOrder(CleanCloudOrderDetails orderDetails) {
        log.info("Creating new order with CleanCloud ID: {}", orderDetails.getOrderId());
        
        Orders newOrder = new Orders();
        // Convert String orderId to Integer for database compatibility
        try {
            newOrder.setCleanCloudOrderId(Integer.parseInt(orderDetails.getOrderId()));
        } catch (NumberFormatException e) {
            log.error("Invalid order ID format: {}", orderDetails.getOrderId());
            return;
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
        
        // Create garments for this order
        if (orderDetails.getGarments() != null) {
            for (CleanCloudOrderDetails.CleanCloudGarment garmentData : orderDetails.getGarments()) {
                createGarment(garmentData, savedOrder);
            }
        }
        
        log.info("Created new order {} with {} garments", savedOrder.getOrderId(), 
                orderDetails.getGarments() != null ? orderDetails.getGarments().size() : 0);
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
    
    private CleanCloudOrderDetails.CleanCloudGarment convertToCleanCloudGarment(CleanCloudGarmentsResponse.CleanCloudGarmentData garmentData) {
        CleanCloudOrderDetails.CleanCloudGarment garment = new CleanCloudOrderDetails.CleanCloudGarment();
        garment.setBarcodeID(garmentData.getBarcodeID());
        
        // Use the actual barcodeID as garmentId since that's what we want to display
        garment.setGarmentId(garmentData.getBarcodeID());
        
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
}
