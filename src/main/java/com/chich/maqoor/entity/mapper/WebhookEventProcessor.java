package com.chich.maqoor.entity.mapper;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.*;

import java.util.Map;


@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WebhookEventProcessor {

    @Getter
    private String event;
    @Getter
    private String source;
    private int id;
    private Map<String, String> data;

    public String getStatusNumber(String status){
        return data.get(status);
    }

}
