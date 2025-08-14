package com.chich.maqoor.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

@Data
public class CleanCloudOrderResponse {
    @JsonProperty("Success")
    private String success;
    
    @JsonProperty("Orders")
    private List<CleanCloudOrderData> orders;
    
    @Data
    public static class CleanCloudOrderData {
        private String id;
        private String customerID;
        private String businessID;
        private String total;
        private String status;
        private String delivery;
        private String deliveryDate;
        private String deliveryTime;
        private String pickup;
        private String pickupDate;
        private String pickupTime;
        private String createdDate;
        private String pieces;
        private String summary;
        private String notes;
        private String address;
        private String weight;
        private String discount;
        private String discountPercentOverride;
        private String creditUsed;
        private String priceListID;
        private String tax1;
        private String tax2;
        private String tax3;
        private String tip;
        private String tipPercent;
        private String paid;
        private String paymentType;
        private String paymentTime;
        private String deliveryFee;
        private String expressFee;
        private String expressFeePercent;
        private String cleanedDate;
        private String completedDate;
        private String driverPickedUpTimestamp;
        private String routeID;
        private String rack;
        private String lockerOrder;
        private String lockerLocationID;
        private String lockerNumber;
        private String lockerReturnNumber;
        private String lockerReturnCode;
        private String lockerExtraStatus;
        private String hasPhotos;
        private String hasSignature;
        private String driverOnTheWay;
        private String pickupReschedule;
        private String deliveryReschedule;
        private String splitParentID;
        private String bagsToPickup;
        private String doordash;
        private String services;
        private String reviewStatus;
        private String retail;
        private String sectionIDs;
        private String receiptLink;
        private String minimumAdjust;
        private String invoiceID;
        private String invoiceStatus;
        private StaffIds staffIds;
        
        @Data
        public static class StaffIds {
            private String create;
            private String cleaned;
            private String completed;
            private String payment;
            private String pickup;
        }
    }
}
