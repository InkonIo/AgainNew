package com.chatalyst.backend.Notifications.dto;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import com.chatalyst.backend.Entity.User;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_notification_settings")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserNotificationSettings {
    
    @Id
    private Long userId;
    
    @OneToOne
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;
    
    @Column(nullable = false)
    private Boolean systemNotifications = true;
    
    @Column(nullable = false)
    private Boolean browserNotifications = true;
    
    @Column(nullable = false)
    private Boolean messageNotifications = true;
    
    @Column(nullable = false)
    private Boolean adminNotifications = true;
    
    @Column(nullable = false)
    private Boolean soundEnabled = true;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}

