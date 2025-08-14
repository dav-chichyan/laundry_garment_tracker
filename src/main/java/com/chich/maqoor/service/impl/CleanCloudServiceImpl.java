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
            String url = baseUrl + "/orders/" + orderId;
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + apiToken);
            headers.set("Content-Type", "application/json");
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            log.info("Fetching order from CleanCloud: {}", url);
            
            ResponseEntity<CleanCloudOrderDetails> response = restTemplate.exchange(
                url, 
                HttpMethod.GET, 
                entity, 
                CleanCloudOrderDetails.class
            );
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.info("Successfully fetched order {} from CleanCloud", orderId);
                return response.getBody();
            } else {
                log.warn("Failed to fetch order {} from CleanCloud. Status: {}", orderId, response.getStatusCode());
                return null;
            }
            
        } catch (Exception e) {
            log.error("Error fetching order {} from CleanCloud: {}", orderId, e.getMessage(), e);
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
        newOrder.setCleanCloudOrderId(orderDetails.getOrderId());
        newOrder.setOrderNumber(String.valueOf(orderDetails.getOrderId()));
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
                String cleanCloudGarmentId = garmentData.getGarmentId();
                
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
        Garments newGarment = new Garments();
        newGarment.setCleanCloudGarmentId(garmentData.getGarmentId());
        newGarment.setOrder(order);
        newGarment.setDescription(garmentData.getDescription());
        newGarment.setType(garmentData.getType());
        newGarment.setColor(garmentData.getColor());
        newGarment.setSize(garmentData.getSize());
        newGarment.setSpecialInstructions(garmentData.getSpecialInstructions());
        newGarment.setDepartmentId(Departments.RECEPTION); // Default to reception
        newGarment.setCreatedAt(new Date());
        newGarment.setLastUpdate(new Date());
        
        garmentRepository.save(newGarment);
        log.debug("Created new garment: {} for order: {}", garmentData.getGarmentId(), order.getOrderId());
    }

    private void updateGarment(Garments existingGarment, CleanCloudOrderDetails.CleanCloudGarment garmentData) {
        existingGarment.setDescription(garmentData.getDescription());
        existingGarment.setType(garmentData.getType());
        existingGarment.setColor(garmentData.getColor());
        existingGarment.setSize(garmentData.getSize());
        existingGarment.setSpecialInstructions(garmentData.getSpecialInstructions());
        existingGarment.setLastUpdate(new Date());
        
        garmentRepository.save(existingGarment);
        log.debug("Updated existing garment: {} for order: {}", garmentData.getGarmentId(), existingGarment.getOrder().getOrderId());
    }
}
