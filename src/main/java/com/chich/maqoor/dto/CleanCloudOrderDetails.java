package com.chich.maqoor.dto;

import lombok.Data;
import java.util.List;

@Data
public class CleanCloudOrderDetails {
    private int orderId;
    private String status;
    private List<CleanCloudGarment> garments;
    private String customerName;
    private String customerPhone;
    private String pickupDate;
    private String deliveryDate;
    
    @Data
    public static class CleanCloudGarment {
        private String garmentId;
        private String description;
        private String type;
        private String color;
        private String size;
        private String specialInstructions;
    }
}
