package com.chatalyst.backend.KaspiPayment.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "subscription")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String type; // STANDARD, PREMIUM
    

    @Column(name = "duration_months", nullable = false, columnDefinition = "integer default 1")
    private int durationInMonths; // 1, 3, 6, 12

    @Column(name = "bots_allowed", nullable = false)
    private int botsAllowed;

    @Column(name = "messages_allowed", nullable = false)
    private int messagesAllowed;
}