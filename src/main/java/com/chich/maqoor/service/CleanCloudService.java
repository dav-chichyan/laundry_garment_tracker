package com.chich.maqoor.service;

import com.chich.maqoor.dto.CleanCloudOrderDetails;
import org.springframework.stereotype.Service;

@Service
public interface CleanCloudService {
    
    /**
     * Get order details from CleanCloud API
     * @param orderId The order ID from CleanCloud
     * @return Order details including garment information
     */
    CleanCloudOrderDetails getOrder(int orderId);
    
    /**
     * Sync order with our database
     * @param orderId The order ID to sync
     */
    void syncOrderWithDatabase(int orderId);
    
    /**
     * Handle order creation webhook
     * @param orderId The order ID from webhook
     */
    void handleOrderCreated(int orderId);
    
    /**
     * Handle order status change webhook
     * @param orderId The order ID from webhook
     * @param newStatus The new status code
     */
    void handleOrderStatusChanged(int orderId, int newStatus);
}
