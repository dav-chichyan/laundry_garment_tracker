package com.chich.maqoor.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

@Data
public class CleanCloudGarmentsResponse {
    @JsonProperty("Success")
    private String success;
    
    @JsonProperty("garments")
    private List<CleanCloudGarmentData> garments;
    
    @Data
    public static class CleanCloudGarmentData {
        private String barcodeID;
        private String color1;
        private String color2;
        private String notes;
        private String customStatus;
        private String conveyorLocation;
        private String sqmTotal;
        private String sqmWidth;
        private String sqmHeight;
        private Upcharges upcharges;
        
        @Data
        public static class Upcharges {
            private UpchargeItem upcharge1;
            private UpchargeItem upcharge2;
            private UpchargeItem upcharge3;
            private UpchargeItem upcharge4;
            private UpchargeItem upcharge5;
            private UpchargeItem upcharge6;
            
            @Data
            public static class UpchargeItem {
                private String id;
            }
        }
    }
}
