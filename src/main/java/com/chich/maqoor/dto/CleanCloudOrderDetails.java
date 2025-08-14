package com.chich.maqoor.dto;

import lombok.Data;
import java.util.List;

@Data
public class CleanCloudOrderDetails {
    private String orderId;
    private String status;
    private String customerID;
    private String customerName;  // Added for compatibility
    private String customerPhone; // Added for compatibility
    private String deliveryDate;
    private String pickupDate;
    private String pieces;
    private String summary;
    private List<CleanCloudGarment> garments;
    
    @Data
    public static class CleanCloudGarment {
        private String barcodeID;
        private String garmentId;  // Added for compatibility
        private String description; // Added for compatibility
        private String type;        // Added for compatibility
        private String color;       // Added for compatibility
        private String size;        // Added for compatibility
        private String specialInstructions; // Added for compatibility
        private String color1;
        private String color2;
        private String notes;
        private String customStatus;
        private String conveyorLocation;
    }
}
