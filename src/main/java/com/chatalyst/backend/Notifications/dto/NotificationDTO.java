package com.chatalyst.backend.Notifications.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDTO {
    private String id;
    private String type;
    private String title;
    private String message;
    private Object data;
    private LocalDateTime timestamp;
}

