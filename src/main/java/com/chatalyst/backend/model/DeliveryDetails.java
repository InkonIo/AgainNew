package com.chatalyst.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "delivery_details")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Связь один-к-одному с Bot
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bot_id", nullable = false, unique = true)
    private Bot bot;

    // Адрес для самовывоза или общая информация о доставке
    @Column(length = 500)
    private String pickupAddress;

    // Контактный телефон владельца/менеджера
    @Column(length = 50)
    private String contactPhone;

    // Ссылка на WhatsApp
    @Column(length = 255)
    private String whatsappLink;

    // Ссылка на другую соцсеть (например, Instagram, Telegram)
    @Column(length = 255)
    private String otherSocialMediaLink;

    // Дополнительная информация о доставке/оплате
    @Column(length = 1000)
    private String additionalInfo;
}
