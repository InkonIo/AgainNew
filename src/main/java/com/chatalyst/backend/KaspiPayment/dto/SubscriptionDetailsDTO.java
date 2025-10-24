package com.chatalyst.backend.KaspiPayment.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionDetailsDTO {
    private int botsAllowed;
    private String supportLevel;
}

