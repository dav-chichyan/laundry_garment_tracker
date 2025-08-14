package com.chich.maqoor.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class CleanCloudWebhookPayload {
    private String event;
    private int id;
    
    @JsonProperty("store_id")
    private int storeId;
    
    private String source;
    
    @JsonProperty("webhook_time")
    private long webhookTime;
    
    private WebhookData data;
    
    @Data
    public static class WebhookData {
        @JsonProperty("new_status")
        private Integer newStatus;
    }
}
