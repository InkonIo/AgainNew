package com.chatalyst.backend.dto;

import lombok.Data;

@Data
public class DeliveryDetailsResponse {

    private Long id;
    private Long botId;
    private String pickupAddress;
    private String contactPhone;
    private String whatsappLink;
    private String otherSocialMediaLink;
    private String additionalInfo;
}
