package com.chatalyst.backend.Entity;

public enum RoleName {
    ROLE_USER, // New default role
    ROLE_ADMIN,
    
    // Standard Subscriptions
    ROLE_STANDARD_1M,
    ROLE_STANDARD_3M,
    ROLE_STANDARD_6M,
    ROLE_STANDARD_12M,
    
    // Premium Subscriptions
    ROLE_PREMIUM_1M,
    ROLE_PREMIUM_3M,
    ROLE_PREMIUM_6M,
    ROLE_PREMIUM_12M,
    
    // Special role for users who completed the trial month and are eligible for a discount on renewal
    ROLE_AFTERMONTH_DISCOUNT
}
