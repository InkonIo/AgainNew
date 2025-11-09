package com.chatalyst.backend.dto;

import lombok.Data;

@Data
public class DeliveryDetailsRequest {
    
    private Long botId;
    private String pickupAddress;
    private String contactPhone;
    private String whatsappLink;
    private String otherSocialMediaLink;
    private String additionalInfo;
}
